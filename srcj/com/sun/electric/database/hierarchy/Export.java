/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Export.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.dialogs.BusParameters;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.util.Collections;
import java.util.Iterator;

/**
 * An Export is a PortProto at the Cell level.  It points to the
 * PortInst that got exported, which identifies a NodeInst and a PortProto on that NodeInst.
 * <P>
 * An Export takes a PortInst on a NodeInst and makes it available as a PortInst
 * on instances of this NodeInst, farther up the hierarchy.
 * An Export therefore belongs to the NodeInst that is its source and also to the Cell
 * that the NodeInst belongs to.
 * The data structures look like this:
 * <P>
 * <CENTER><IMG SRC="doc-files/Export-1.gif"></CENTER>
 */
public class Export extends ElectricObject implements PortProto, Comparable<Export> {

    /** Empty Export array for initialization. */
    public static final Export[] NULL_ARRAY = {};
    /** Key of text descriptor of export name */
    public static final Variable.Key EXPORT_NAME = Variable.newKey("EXPORT_name");
    /** Key of Varible holding reference name. */
    public static final Variable.Key EXPORT_REFERENCE_NAME = Variable.newKey("EXPORT_reference_name");
    // -------------------------- private data ---------------------------
    /** persistent data of this Export. */
    private ImmutableExport d;
    /** The parent Cell of this Export. */
    private final Cell parent;
    /** Index of this Export in Cell ports. */
    private int portIndex;

    // -------------------- protected and private methods --------------
    /**
     * The constructor of Export. Use the factory "newInstance" instead.
     * @param d persistent data of this Export.
     * @param parent the Cell in which this Export will reside.
     */
    Export(ImmutableExport d, Cell parent) {
        this.parent = parent;
        this.d = d;
        assert d.exportId.parentId == parent.getId();
    }

    private Object writeReplace() {
        return new ExportKey(this);
    }

    private static class ExportKey extends EObjectInputStream.Key<Export> {

        public ExportKey() {
        }

        private ExportKey(Export export) {
            super(export);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, Export export) throws IOException {
            ExportId exportId = export.getId();
            if (export.getDatabase() != out.getDatabase() || !export.isLinked()) {
                throw new NotSerializableException(export + " not linked");
            }
            out.writeObject(exportId);
        }

        @Override
        public Export readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            ExportId exportId = (ExportId) in.readObject();
            Export export = exportId.inDatabase(in.getDatabase());
            if (export == null) {
                throw new InvalidObjectException(exportId + " not linked");
            }
            return export;
        }
    }

    /****************************** CREATE, DELETE, MODIFY ******************************/
    /**
     * Method to create an Export with the specified values.
     * @param parent the Cell in which this Export resides.
     * @param portInst the PortInst to export
     * @param protoName the name of this Export.
     * It may not have unprintable characters, spaces, or tabs in it.
     * @return the newly created Export.
     */
    public static Export newInstance(Cell parent, PortInst portInst, String protoName) {
        return newInstance(parent, portInst, protoName, null, true, null);
    }

    /**
     * Method to create an Export with the specified values.
     * @param parent the Cell in which this Export resides.
     * @param portInst the PortInst to export
     * @param protoName the name of this Export.
     * It may not have unprintable characters, spaces, or tabs in it.
     * @param characteristic the characteristic (input, output) of this Export.
     * @return the newly created Export.
     */
    public static Export newInstance(Cell parent, PortInst portInst, String protoName, 
                                     PortCharacteristic characteristic, IconParameters iconParameters)
    {
        return newInstance(parent, portInst, protoName, characteristic, true, iconParameters);
    }

    /**
     * Method to create an Export with the specified values.
     * @param parent the Cell in which this Export resides.
     * @param portInst the PortInst to export
     * @param protoName the name of this Export.
     * It may not have unprintable characters, spaces, or tabs in it.
     * @param characteristic the characteristic (input, output) of this Export.
     * @param createOnIcon true to create an equivalent export on any associated icon.
     * @return the newly created Export.
     */
    public static Export newInstance(Cell parent, PortInst portInst, String protoName,
                                     PortCharacteristic characteristic, boolean createOnIcon,
                                     IconParameters iconParameters)
    {
        if (protoName == null) {
            return null;
        }
        EditingPreferences ep = parent.getEditingPreferences();

        boolean busNamesAllowed = parent.busNamesAllowed();
        Name protoNameKey = ImmutableExport.validExportName(protoName, busNamesAllowed);
        if (protoNameKey == null) {
            // hack: try removing offending characters
            protoName = protoName.replace(':', '_');
            protoNameKey = ImmutableExport.validExportName(protoName, busNamesAllowed);
            if (protoNameKey == null) {
                System.out.println("Bad export name " + protoName + " : " + Name.checkName(protoName));
                return null;
            }
        }

        if (parent.findExport(protoName) != null) {
            String oldName = protoName;
            protoName = ElectricObject.uniqueObjectName(protoName, parent, PortProto.class, false, true);
            if (protoName == null) {
                System.out.println(parent + " already has an export named " + oldName + ", export was not created");
                return null;
            }
            System.out.println(parent + " already has an export named " + oldName
                    + ", making new export named " + protoName);
            assert (parent.findExport(protoName) == null);
        }
        ExportId exportId = parent.getD().cellId.newPortId(protoName);
        if (exportId.inDatabase(parent.getDatabase()) != null) {
            exportId = parent.getD().cellId.randomExportId(protoName);
        }
        PortProto originalProto = portInst.getPortProto();
        boolean alwaysDrawn = false;
        boolean bodyOnly = false;
        if (originalProto instanceof Export) {
            Export e = (Export) originalProto;
            alwaysDrawn = e.isAlwaysDrawn();
            bodyOnly = e.isBodyOnly();
        }
        PortCharacteristic newCharacteristic = characteristic;
        if (newCharacteristic == null) {
            newCharacteristic = originalProto.getCharacteristic();
        }
        Export pp = newInstance(parent, exportId, protoName, smartPlacement(portInst), portInst, alwaysDrawn, bodyOnly, newCharacteristic, null);

        if (createOnIcon) {
            // if this was made on a schematic, and an icon exists, make the export on the icon as well
            Cell icon = parent.iconView();
            if (icon != null && icon.findExport(protoName) == null) {
                // find analagous point to create export
                Rectangle2D bounds = parent.getBounds();
                double locX = portInst.getPoly().getCenterX();
                double locY = portInst.getPoly().getCenterY();
                Rectangle2D iconBounds = icon.getBounds();
                Dimension2D alignmentToGrid = parent.getEditingPreferences().getAlignmentToGrid();
                double newlocX = (locX - bounds.getMinX()) / bounds.getWidth() * iconBounds.getWidth() + iconBounds.getMinX();
                newlocX = DBMath.toNearest(newlocX, alignmentToGrid.getWidth());
                double bodyDX = ep.iconGenLeadLength;
                double distToXEdge = locX - bounds.getMinX();
                if (locX >= bounds.getCenterX()) {
                    bodyDX = -bodyDX;
                    distToXEdge = bounds.getMaxX() - locX;
                }
                double newlocY = (locY - bounds.getMinY()) / bounds.getHeight() * iconBounds.getHeight() + iconBounds.getMinY();
                newlocY = DBMath.toNearest(newlocY, alignmentToGrid.getHeight());
                double bodyDY = ep.iconGenLeadLength;
                double distToYEdge = locY - bounds.getMinY();
                if (locY >= bounds.getCenterY()) {
                    bodyDY = -bodyDY;
                    distToYEdge = bounds.getMaxY() - locY;
                }
                if (distToXEdge > distToYEdge) {
                    bodyDX = 0;
                } else {
                    bodyDY = 0;
                }

                // round
                Point2D point = new Point2D.Double(newlocX, newlocY);
                DBMath.gridAlign(point, alignmentToGrid);
                newlocX = point.getX();
                newlocY = point.getY();

                // create export in icon
                int rotation = ViewChanges.iconTextRotation(pp);
                if (!IconParameters.makeIconExport(pp, 0, newlocX, newlocY, newlocX + bodyDX, newlocY + bodyDY, icon, rotation)) {
                    System.out.println("Warning: Failed to create associated export in icon " + icon.describe(true));
                }
            }
        }

        return pp;
    }

    /**
     * Factory method to create an Export
     * @param parent the Cell in which this Export resides.
     * @param exportId ExportId of this Export
     * @param name the user name of this Export. if null then the same as id.
     * It may not have unprintable characters, spaces, or tabs in it.
     * @param nameTextDescriptor text descriptor of this Export
     * @param originalPort the PortInst that is being exported.
     * @param alwaysDrawn true if this Export is always drawn.
     * @param bodyOnly true to exclude this Export from icon.
     * @param characteristic PortCharacteristic of this Export.
     * @param errorLogger error logger to report errors.
     * @return created Export or null on error.
     */
    public static Export newInstance(Cell parent, ExportId exportId, String name, TextDescriptor nameTextDescriptor, PortInst originalPort,
            boolean alwaysDrawn, boolean bodyOnly, PortCharacteristic characteristic, ErrorLogger errorLogger) {
        assert parent.isLinked();
        String errorMsg = null;
        if (exportId.inDatabase(parent.getDatabase()) != null) {
            errorMsg = parent + " already has exportId " + exportId.externalId;
            System.out.println(errorMsg);
            errorLogger.logError(errorMsg, parent, 1);
            return null;
        }
        if (name == null) {
            name = exportId.externalId;
        }

        // initialize this object
        if (originalPort == null || !originalPort.isLinked()) {
            System.out.println("Null port on Export " + name + " in " + parent);
            return null;
        }
        NodeInst originalNode = originalPort.getNodeInst();
        PortProto subpp = originalPort.getPortProto();
        if (originalNode.getParent() != parent || subpp.getParent() != originalNode.getProto()) {
            System.out.println("Bad port on Export " + name + " in " + parent);
            return null;
        }

        if (ImmutableExport.validExportName(name, parent.busNamesAllowed()) == null) {
            errorMsg = parent + " has bad export name " + name + " ";
            String newName = repairExportName(parent, name);
            if (newName == null) {
                newName = repairExportName(parent, "X");
            }
            if (newName == null) {
                errorMsg += " removed ";
                System.out.println(errorMsg);
                errorLogger.logError(errorMsg, parent, 1);
                return null;
            }
            errorMsg += " renamed to " + newName;
            name = newName;
        }
        if (nameTextDescriptor == null) {
            nameTextDescriptor = TextDescriptor.getExportTextDescriptor();
        }
        ImmutableExport d = ImmutableExport.newInstance(exportId, Name.findName(name), nameTextDescriptor,
                originalNode.getD().nodeId, subpp.getId(), alwaysDrawn, bodyOnly, characteristic);
        Export e = new Export(d, parent);
        assert e.getOriginalPort() == originalPort;
        originalNode.redoGeometric();
        parent.addExport(e);
        if (errorMsg != null) {
            System.out.println(errorMsg);
            if (errorLogger != null) {
                errorLogger.logError(errorMsg, e, 1);
            }
        }

        // handle change control, constraint, and broadcast
        Constraints.getCurrent().newObject(e);
        return e;
    }

    /**
     * Method to unlink this Export from its Cell.
     */
    public void kill() {
        parent.killExports(Collections.singleton(this));
    }

    /**
     * Method to rename this Export.
     * @param newName the new name of this Export.
     */
    public void rename(String newName) {
        checkChanging();

        // get unique name
        // special case: if changing case only, allow it
//		if (!getName().equalsIgnoreCase(newName) || getName().equals(newName))
//		{
        // not changing case
        String dupName = ElectricObject.uniqueObjectName(newName, parent, PortProto.class, false, true);
        if (!dupName.equals(newName)) {
            System.out.println(parent + " already has an export named " + newName
                    + ", making new export named " + dupName);
            newName = dupName;
        }
//		}
        Name newNameKey = ImmutableExport.validExportName(newName, parent.busNamesAllowed());
        if (newNameKey == null) {
            System.out.println("Bad export name " + newName + " : " + Name.checkName(newName));
            return;
        }

        // do the rename
        Name oldName = getNameKey();
        parent.moveExport(portIndex, newName);
        setD(d.withName(newNameKey), true);
        //       parent.notifyRename(false);

        // rename associated export in icons, if any
        if (parent.getView() == View.SCHEMATIC)
        {
	        for(Iterator<Cell> cIt = parent.getCellGroup().getCells(); cIt.hasNext(); )
	        {
	        	Cell iconCell = cIt.next();
	        	if (iconCell.getView() != View.ICON) continue;
	            for (Iterator<Export> it = iconCell.getExports(); it.hasNext();)
	            {
	                Export pp = it.next();
	                if (pp.getName().equals(oldName.toString()))
	                {
	                    pp.rename(newName);
	                    break;
	                }
	            }
	        }
        }

//        Cell iconCell = parent.iconView();
//        if (iconCell != null && iconCell != parent)
//        {
//            for (Iterator<Export> it = iconCell.getExports(); it.hasNext();)
//            {
//                Export pp = it.next();
//                if (pp.getName().equals(oldName.toString()))
//                {
//                    pp.rename(newName);
//                    break;
//                }
//            }
//        }
    }

    /**
     * Method to move this Export to a different PortInst in the Cell.
     * The method expects both ports to be in the same place and simply shifts
     * the arcs without re-constraining them.
     * @param newPi the new PortInst on which to base this Export.
     * @return true on error.
     */
    public boolean move(PortInst newPi) {
        checkChanging();

        NodeInst newno = newPi.getNodeInst();
        PortProto newsubpt = newPi.getPortProto();

        // error checks
        if (newno.getParent() != parent) {
            return true;
        }
        if (newsubpt.getParent() != newno.getProto()) {
            return true;
        }
        if (doesntConnect(newsubpt.getBasePort())) {
            return true;
        }

        // remember old state
        ImmutableExport oldD = d;

        // change the port origin
        lowLevelModify(d.withOriginalPort(newno.getD().nodeId, newsubpt.getId()));

        // handle change control, constraint, and broadcast
        Constraints.getCurrent().modifyExport(this, oldD);

        // update all port characteristics exported from this one
        changeallports();
        return false;
    }

    /****************************** LOW-LEVEL IMPLEMENTATION ******************************/
    /**
     * Method to change the origin of this Export to another place in the Cell.
     * @param d the new PortInst in the cell that will hold this Export.
     */
    public void lowLevelModify(ImmutableExport d) {
        assert isLinked();
        boolean renamed = getNameKey() != d.name;
        boolean moved = this.d.originalNodeId != d.originalNodeId || this.d.originalPortId != d.originalPortId;
        // remove the old linkage
        if (moved) {
            parent.getNodeById(this.d.originalNodeId).redoGeometric();
        }
        if (renamed) {
            parent.moveExport(portIndex, d.name.toString());
        }

        setD(d, false);

        // create the new linkage
        if (moved) {
            parent.getNodeById(d.originalNodeId).redoGeometric();
        }
    }

    /**
     * Method to set an index of this Export in Cell ports.
     * This is a zero-based index of ports on the Cell.
     * @param portIndex an index of this Export in Cell ports.
     */
    void setPortIndex(int portIndex) {
        this.portIndex = portIndex;
    }

    /**
     * Method to copy state bits from other Export.
     * State bits are alowaysDrawn, bodyOnly and characteristic.
     * @param other Export from which to take state bits.
     */
    public void copyStateBits(Export other) {
        setAlwaysDrawn(other.isAlwaysDrawn());
        setBodyOnly(other.isBodyOnly());
        setCharacteristic(other.getCharacteristic());
    }

    /****************************** GRAPHICS ******************************/
    /**
     * Method to return a Poly that describes this Export name.
     * @return a Poly that describes this Export's name.
     */
    public Poly getNamePoly() {
        Poly poly = getPoly();
        double cX = poly.getCenterX();
        double cY = poly.getCenterY();
        TextDescriptor td = getTextDescriptor(EXPORT_NAME);
        double offX = td.getXOff();
        double offY = td.getYOff();
        TextDescriptor.Position pos = td.getPos();
        Poly.Type style = pos.getPolyType();
        Point2D[] pointList = new Point2D.Double[1];
//        if (NEWWAY)
//        {
//	        // must untransform the node to apply the offset
//	        NodeInst ni = getOriginalPort().getNodeInst();
//            AffineTransform trans = ni.rotateIn();
//	        pointList[0] = new Point2D.Double(cX, cY);
//	        trans.transform(pointList[0], pointList[0]);
//	        pointList[0].setLocation(pointList[0].getX()+offX, pointList[0].getY()+offY);
//
//	        poly = new Poly(pointList);
//	        poly.setStyle(style);
//	        poly.setPort(this);
//	        poly.setString(getName());
//	        poly.setTextDescriptor(td);
//	        poly.setDisplayedText(new DisplayedText(this, EXPORT_NAME));
//	        poly.transform(ni.rotateOut());
//        } else
        {
	        // must untransform the node to apply the offset
	        NodeInst ni = getOriginalPort().getNodeInst();
	        if (!ni.getOrient().equals(Orientation.IDENT)) {
	            pointList[0] = new Point2D.Double(cX, cY);
	            AffineTransform trans = ni.rotateIn();
	            trans.transform(pointList[0], pointList[0]);
	            pointList[0].setLocation(pointList[0].getX() + offX, pointList[0].getY() + offY);
	            trans = ni.rotateOut();
	            trans.transform(pointList[0], pointList[0]);
	        } else {
	            pointList[0] = new Point2D.Double(cX + offX, cY + offY);
	        }

	        poly = new Poly(pointList);
	        poly.setStyle(style);
	        poly.setPort(this);
	        poly.setString(getName());
	        poly.setTextDescriptor(td);
	        poly.setDisplayedText(new DisplayedText(this, EXPORT_NAME));
        }
        return poly;
    }

    /**
     * Method to return the Poly that describes this Export.
     * @return the Poly that describes this Export.
     */
    public Poly getPoly() {
        return getOriginalPort().getPoly();
    }

    /****************************** TEXT ******************************/
    /**
     * Method to determine the appropriate Cell associated with this ElectricObject.
     * @return the appropriate Cell associated with this ElectricObject.
     * Returns null if no Cell can be found.
     */
    public Cell whichCell() {
        return parent;
    }

    ;

    /**
     * Returns persistent data of this Export.
     * @return persistent data of this Export.
     */
    @Override
    public ImmutableExport getD() {
        return d;
    }

    /**
     * Modifies persistend data of this Export.
     * @param newD new persistent data.
     * @param notify true to notify Undo system.
     * @return true if persistent data was modified.
     */
    boolean setD(ImmutableExport newD, boolean notify) {
        checkChanging();
        ImmutableExport oldD = d;
        if (newD == oldD) {
            return false;
        }
        if (parent != null) {
            parent.setContentsModified();
            d = newD;
            if (notify) {
                Constraints.getCurrent().modifyExport(this, oldD);
            }
        } else {
            d = newD;
        }
        return true;
    }

    /**
     * Modifies persistent data of this Export.
     * @param newD new persistent data.
     */
    void setDInUndo(ImmutableExport newD) {
        checkUndoing();
        d = newD;
    }

    /**
     * Method to add a Variable on this Export.
     * It may add repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        setD(d.withVariable(var), true);
    }

    /**
     * Method to delete a Variable from this Export.
     * @param key the key of the Variable to delete.
     */
    public void delVar(Variable.Key key) {
        setD(d.withoutVariable(key), true);
    }

    /**
     * Method to copy all variables from another Export to this Export.
     * @param other the other Export from which to copy Variables.
     */
    public void copyVarsFrom(ElectricObject other) {
        super.copyVarsFrom(other);

        // delete the Bus parameterization in icons
        if (getParent().isIcon()) {
            for (Iterator<Variable> it = getVariables(); it.hasNext();) {
                Variable var = it.next();
                if (var.getKey() == BusParameters.EXPORT_BUS_TEMPLATE) {
                    delVar(var.getKey());
                    break;
                }
            }
        }
    }

    /** Method to return PortProtoId of this Export.
     * PortProtoId identifies Export independently of threads.
     * @return PortProtoId of this Export.
     */
    public ExportId getId() {
        return d.exportId;
    }

    /**
     * Method to return the parent NodeProto of this Export.
     * @return the parent NodeProto of this Export.
     */
    public Cell getParent() {
        return parent;
    }

    /**
     * Method to return chronological index of this Export in parent.
     * @return chronological index of this Export in parent.
     */
    public int getChronIndex() {
        return d.exportId.chronIndex;
    }

    /**
     * Method to get the index of this Export.
     * This is a zero-based index of ports on the Cell.
     * @return the index of this Export.
     */
    public int getPortIndex() {
        return portIndex;
    }

    /**
     * Returns the TextDescriptor on this Export selected by variable key.
     * This key may be a key of variable on this Export or
     * the special key <code>Export.EXPORT_NAME</code>.
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special key.
     * @return the TextDescriptor on this Export.
     */
    public TextDescriptor getTextDescriptor(Variable.Key varKey) {
        if (varKey == EXPORT_NAME) {
            return d.nameDescriptor;
        }
        return super.getTextDescriptor(varKey);
    }

    /**
     * Updates the TextDescriptor on this Export selected by varName.
     * The varKey may be a key of variable on this ElectricObject or
     * the special key Export.EXPORT_NAME.
     * If varKey doesn't select any text descriptor, no action is performed.
     * The TextDescriptor gives information for displaying the Variable.
     * @param varKey key of variable or special name.
     * @param td new value TextDescriptor
     */
    @Override
    public void setTextDescriptor(Variable.Key varKey, TextDescriptor td) {
        if (varKey == EXPORT_NAME) {
            setD(d.withNameDescriptor(td), true);
            return;
        }
        super.setTextDescriptor(varKey, td);
    }

    /**
     * Method to determine whether a variable key on Export is deprecated.
     * Deprecated variable keys are those that were used in old versions of Electric,
     * but are no longer valid.
     * @param key the key of the variable.
     * @return true if the variable key is deprecated.
     */
    public boolean isDeprecatedVariable(Variable.Key key) {
        if (key == EXPORT_NAME) {
            return true;
        }
        return super.isDeprecatedVariable(key);
    }

    /**
     * Method chooses TextDescriptor with "smart text placement"
     * of Export on specified origianl port.
     * @param originalPort original port for the Export
     * @return Immutable text descriptor with smart text placement
     */
    private static TextDescriptor smartPlacement(PortInst originalPort) {
        // handle smart text placement relative to attached object
        EditingPreferences ep = originalPort.getEditingPreferences();
        int smartVertical = ep.smartVerticalPlacementExport;
        int smartHorizontal = ep.smartHorizontalPlacementExport;
        if (smartVertical == 0 && smartHorizontal == 0) {
            return TextDescriptor.getExportTextDescriptor();
        }

        // figure out location of object relative to environment
        double dx = 0, dy = 0;
        NodeInst ni = originalPort.getNodeInst();
        Rectangle2D nodeBounds = ni.getBounds();
        for (Iterator<Connection> it = originalPort.getConnections(); it.hasNext();) {
            Connection con = it.next();
            ArcInst ai = con.getArc();
            Rectangle2D arcBounds = ai.getBounds();
            dx = arcBounds.getCenterX() - nodeBounds.getCenterX();
            dy = arcBounds.getCenterY() - nodeBounds.getCenterY();
        }

        // first move placement horizontally
        if (smartHorizontal == 2) // place label outside (away from center)
        {
            dx = -dx;
        } else if (smartHorizontal != 1) // place label inside (towards center)
        {
            dx = 0;
        }

        // next move placement vertically
        if (smartVertical == 2) // place label outside (away from center)
        {
            dy = -dy;
        } else if (smartVertical != 1) // place label inside (towards center)
        {
            dy = 0;
        }

        TextDescriptor td = TextDescriptor.getExportTextDescriptor();
        return td.withPos(td.getPos().align(Double.compare(dx, 0), Double.compare(dy, 0)));
//		MutableTextDescriptor td = MutableTextDescriptor.getExportTextDescriptor();
//		td.setPos(td.getPos().align(Double.compare(dx, 0), Double.compare(dy, 0)));
//		return ImmutableTextDescriptor.newTextDescriptor(td);
    }

    /**
     * Method to return the name key of this Export.
     * @return the Name key of this Export.
     */
    public Name getNameKey() {
        return d.name;
    }

    /**
     * Method to return the name of this Export.
     * @return the name of this Export.
     */
    public String getName() {
        return d.name.toString();
    }

    /**
     * Method to return the short name of this PortProto.
     * The short name is everything up to the first nonalphabetic character.
     * @return the short name of this PortProto.
     */
    public String getShortName() {
        return getShortName(getNameKey().toString());
    }

    /**
     * Method to convert name of export to short name.
     * The short name is everything up to the first nonalphabetic character.
     * @param name long name
     * @return the short name of this PortProto.
     */
    public static String getShortName(String name) {
        int len = name.length();
        for (int i = 0; i < len; i++) {
            char ch = name.charAt(i);
            if (TextUtils.isLetterOrDigit(ch)) {
                continue;
            }
            return name.substring(0, i);
        }
        return name;
    }

    /**
     * Repairs export name  true if string is a valid Export name with cirtain width.
     * @param parent parent Cell
     * @param name string to test.
     * @return true if string is a valid Export name with cirtain width.
     */
    private static String repairExportName(Cell parent, String name) {
        String newName = null;
        int oldBusWidth = Name.findName(name).busWidth();
        if (!parent.busNamesAllowed()) {
            oldBusWidth = 1;
        }
        int openIndex = name.indexOf('[');
        if (openIndex >= 0) {
            int afterOpenIndex = openIndex + 1;
            while (afterOpenIndex < name.length() && name.charAt(afterOpenIndex) == '[') {
                afterOpenIndex++;
            }
            int closeIndex = name.lastIndexOf(']');
            if (closeIndex < 0) {
                int lastOpenIndex = name.lastIndexOf('[');
                if (lastOpenIndex > afterOpenIndex) {
                    closeIndex = lastOpenIndex;
                }
            }
            if (afterOpenIndex < closeIndex) {
                newName = name.substring(0, openIndex) + name.substring(closeIndex + 1)
                        + "[" + name.substring(afterOpenIndex, closeIndex) + "]";
            }
        }
        if (validExportName(newName, oldBusWidth)) {
            newName = ElectricObject.uniqueObjectName(newName, parent, PortProto.class, false, true);
            if (validExportName(newName, oldBusWidth)) {
                return newName;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '[' || ch == ']' || ch == ':' || ch == ',' || ch == '@') {
                ch = 'X';
            }
            sb.append(ch);
        }
        newName = sb.toString();
        if (validExportName(newName, oldBusWidth)) {
            newName = ElectricObject.uniqueObjectName(newName, parent, PortProto.class, false, true);
            if (validExportName(newName, oldBusWidth)) {
                return newName;
            }
        }
        return null;
    }

    /**
     * Returns true if string is a valid Export name with cirtain width.
     * @param name string to test.
     * @param busWidth cirtain width.
     * @return true if string is a valid Export name with cirtain width.
     */
    private static boolean validExportName(String name, int busWidth) {
        Name nameKey = ImmutableExport.validExportName(name, true);
        return nameKey != null && nameKey.busWidth() == busWidth;
    }

    /**
     * Compares Exports by their Cells and names.
     * @param that the other Export.
     * @return a comparison between the Exports.
     */
    public int compareTo(Export that) {
        if (parent != that.parent) {
            int cmp = parent.compareTo(that.parent);
            if (cmp != 0) {
                return cmp;
            }
        }
        return d.name.toString().compareTo(that.d.name.toString());
    }

    /**
     * Returns a printable version of this Export.
     * @return a printable version of this Export.
     */
    public String toString() {
        return "export '" + getName() + "'";
    }

    /****************************** MISCELLANEOUS ******************************/
    /**
     * Method to return the port on the NodeInst inside of the cell that is the origin of this Export.
     * @return the port on the NodeInst inside of the cell that is the origin of this Export.
     */
    public PortInst getOriginalPort() {
        return parent.getPortInst(d.originalNodeId, d.originalPortId);
    }

    /**
     * Method to return the base-level port that this PortProto is created from.
     * Since this is an Export, it returns the base port of its sub-port, the port on the NodeInst
     * from which the Export was created.
     * @return the base-level port that this PortProto is created from.
     */
    public PrimitivePort getBasePort() {
        PortProto pp = d.originalPortId.inDatabase(getDatabase());
        return pp.getBasePort();
    }

    /**
     * Method to return true if the specified ArcProto can connect to this Export.
     * @param arc the ArcProto to test for connectivity.
     * @return true if this Export can connect to the ArcProto, false if it can't.
     */
    public boolean connectsTo(ArcProto arc) {
        return getBasePort().connectsTo(arc);
    }

    /**
     * Method to return the PortCharacteristic of this Export.
     * @return the PortCharacteristic of this Exort.
     */
    public PortCharacteristic getCharacteristic() {
        return d.characteristic;
    }

    /**
     * Method to set the PortCharacteristic of this Export.
     * @param characteristic the PortCharacteristic of this Exort.
     */
    public void setCharacteristic(PortCharacteristic characteristic) {
        setD(d.withCharacteristic(characteristic), true);
    }

    /**
     * Method to determine whether this Export is of type Power.
     * This is determined by either having the proper Characteristic, or by
     * having the proper name (starting with "vdd", "vcc", "pwr", or "power").
     * @return true if this Export is of type Power.
     */
    public boolean isPower() {
        PortCharacteristic ch = getCharacteristic();
        if (ch == PortCharacteristic.PWR) {
            return true;
        }
        if (ch != PortCharacteristic.UNKNOWN) {
            return false;
        }
        return isNamedPower();
    }

    /**
     * Method to determine whether this Export has a name that suggests Power.
     * This is determined by having a name starting with "vdd", "vcc", "pwr", or "power".
     * @return true if this Export has a name that suggests Power.
     */
    public boolean isNamedPower() {
        String name = TextUtils.canonicString(getName());
        if (name.indexOf("vdd") >= 0) {
            return true;
        }
        if (name.indexOf("vcc") >= 0) {
            return true;
        }
        if (name.indexOf("pwr") >= 0) {
            return true;
        }
        if (name.indexOf("power") >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Method to determine whether this Export is of type Ground.
     * This is determined by either having the proper PortCharacteristic, or by
     * having the proper name (starting with "vss", "gnd", or "ground").
     * @return true if this Export is of type Ground.
     */
    public boolean isGround() {
        PortCharacteristic ch = getCharacteristic();
        if (ch == PortCharacteristic.GND) {
            return true;
        }
        if (ch != PortCharacteristic.UNKNOWN) {
            return false;
        }
        return isNamedGround();
    }

    /**
     * Method to determine whether this Export has a name that suggests Ground.
     * This is determined by either having a name starting with "vss", "gnd", or "ground".
     * @return true if this Export has a name that suggests Ground.
     */
    public boolean isNamedGround() {
        String name = TextUtils.canonicString(getName());
        if (name.indexOf("vss") >= 0) {
            return true;
        }
        if (name.indexOf("gnd") >= 0) {
            return true;
        }
        if (name.indexOf("ground") >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if this export has its original port on Global-Partition schematics
     * primitive.
     * @return true if this export is Global-Partition export.
     */
    public boolean isGlobalPartition() {
        return d.originalPortId.parentId == Schematics.tech().globalPartitionNode.getId();
    }

    /**
     * Method to set this PortProto to be always drawn.
     * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
     */
    public void setAlwaysDrawn(boolean b) {
        setD(d.withAlwaysDrawn(b), true);
    }

    /**
     * Method to tell whether this PortProto is always drawn.
     * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
     * @return true if this PortProto is always drawn.
     */
    public boolean isAlwaysDrawn() {
        return d.alwaysDrawn;
    }

    /**
     * Method to set this PortProto to exist only in the body of a cell.
     * Ports that exist only in the body do not have an equivalent in the icon.
     * This is used by simulators and icon generators to recognize less significant ports.
     * @param b true if this Export exists only in the body of a cell.
     */
    public void setBodyOnly(boolean b) {
        setD(d.withBodyOnly(b), true);
    }

    /**
     * Method to tell whether this PortProto exists only in the body of a cell.
     * Ports that exist only in the body do not have an equivalent in the icon.
     * This is used by simulators and icon generators to recognize less significant ports.
     * @return true if this PortProto exists only in the body of a cell.
     */
    public boolean isBodyOnly() {
        return d.bodyOnly;
    }

    /**
     * Returns true if this Export is linked into database.
     * @return true if this Export is linked into database.
     */
    public boolean isLinked() {
        try {
            return parent.isLinked() && parent.getPort(portIndex) == this;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Returns database to which this Export belongs.
     * @return database to which this Export belongs.
     */
    public EDatabase getDatabase() {
        return parent.getDatabase();
    }

    /**
     * Method to return the PortProto that is equivalent to this in the
     * corresponding schematic Cell.
     * It finds the PortProto with the same name on the corresponding Cell.
     * If there are multiple versions of the Schematic Cell return the latest.
     * @return the PortProto that is equivalent to this in the corresponding Cell.
     */
    public PortProto getEquivalent() {
        Cell equiv = parent.getEquivalent();
        if (equiv == parent) {
            return this;
        }
        if (equiv == null) {
            return null;
        }
        return equiv.findPortProto(getNameKey());
    }

    /**
     * Method to find the Export on another Cell that is equivalent to this Export.
     * @param otherCell the other cell to equate.
     * @return the Export on that other Cell which matches this Export.
     * Returns null if none can be found.
     */
    public Export getEquivalentPort(Cell otherCell) {
        /* don't waste time searching if the two views are the same */
        if (parent == otherCell) {
            return this;
        }

        // this is the non-cached way to do it
        return otherCell.findExport(getName());

        /* load the cache if not already there */
//		if (otherCell != thisCell->cachedequivcell)
//		{
//			for(Iterator it = thisCell.getPorts(); it.hasNext(); )
//			{
//				Export opp = (Export)it.next();
//				opp->cachedequivport = null;
//			}
//			for(Iterator it = thisCell.getPorts(); it.hasNext(); )
//			{
//				Export opp = (Export)it.next();
//				Export epp = otherCell.findExport(opp.getName());
//				if (epp != null) opp->cachedequivport = epp;
//			}
//			thisCell->cachedequivcell = otherCell;
//		}
//		epp = pp->cachedequivport;
//		if (epp != null) return epp;
//
//		/* don't report errors for global ports not on icons */
//		if (epp == null)
//		{
//			if (!otherCell.isIcon() || !pp.isBodyOnly())
//				System.out.println("Warning: no port in cell %s corresponding to port %s in cell %s"),
//					describenodeproto(otherCell), pp->protoname, describenodeproto(thisCell));
//		}
//		pp->cachedequivport = null;
//		return null;
    }

    /**
     * helper method to ensure that all arcs connected to Export "pp" at
     * instances of its Cell (or any of its export sites)
     * can connect to Export newPP.
     * @return true if the connection cannot be made.
     */
    public boolean doesntConnect(PrimitivePort newPP) {
        // check every instance of this node
        for (Iterator<NodeInst> it = parent.getInstancesOf(); it.hasNext();) {
            NodeInst ni = it.next();

            // make sure all arcs on this port can connect
            PortInst pi = ni.findPortInstFromProto(this);
            for (Iterator<Connection> cIt = pi.getConnections(); cIt.hasNext();) {
                Connection con = cIt.next();
//			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
//			{
//				Connection con = (Connection)cIt.next();
//				if (con.getPortInst().getPortProto() != this) continue;
                if (!newPP.connectsTo(con.getArc().getProto())) {
                    System.out.println(con.getArc() + " in " + ni.getParent()
                            + " cannot connect to port " + getName());
                    return true;
                }
            }

            // make sure all further exports are still valid
            for (Iterator<Export> eIt = ni.getExports(); eIt.hasNext();) {
                Export oPP = eIt.next();
                if (oPP.getOriginalPort().getPortProto() != this) {
                    continue;
                }
                if (oPP.doesntConnect(newPP)) {
                    return true;
                }
            }
        }
        return false;
    }

    /****************************** SUPPORT ******************************/
    /**
     * Method to change all usage of this Export because it has been moved.
     * The various state bits are changed to reflect the new Export base.
     */
    private void changeallports() {
        // look at all instances of the cell that had export motion
        recursivelyChangeAllPorts();

        // look at associated cells and change their ports
        if (parent.isIcon()) {
            // changed an export on an icon: find contents and change it there
            Cell onp = parent.contentsView();
            if (onp != null) {
                Export opp = getEquivalentPort(onp);
                if (opp != null) {
                    opp.setCharacteristic(getCharacteristic());
                    opp.recursivelyChangeAllPorts();
                }
            }
            return;
        }

        // see if there is an icon to change
        Cell onp = parent.iconView();
        if (onp != null) {
            Export opp = getEquivalentPort(onp);
            if (opp != null) {
                opp.setCharacteristic(getCharacteristic());
                opp.recursivelyChangeAllPorts();
            }
        }
    }

    /**
     * Method to recursively alter the state bit fields of this Export.
     */
    private void recursivelyChangeAllPorts() {
        parent.recursivelyChangeAllPorts(Collections.singleton(this));
    }

    /**
     * This function is to compare Export elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same Export
     */
    public boolean compare(Object obj, StringBuffer buffer) {
        if (this == obj) {
            return (true);
        }

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass()) {
            return (false);
        }

        PortProto no = (PortProto) obj;
        // getNameKey is required to call proper Name.equals()
        if (!getNameKey().equals(no.getNameKey())) {
            if (buffer != null) {
                buffer.append("'" + this + "' and '" + no + "' do not have same name\n");
            }
            return (false);
        }
        PortCharacteristic noC = no.getCharacteristic();

        if (!getCharacteristic().getName().equals(noC.getName())) {
            if (buffer != null) {
                buffer.append("'" + this + "' and '" + no + "' do not have same characteristic\n");
            }
            return (false);
        }
        return (true);
    }
}
