package com.sun.electric.tool.generator.layout;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

/** The TechType class holds technology dependent information for the layout
 * generators. Most of the information is available from public static methods.
 * <p> The TechType class queries the appropriate Technology object to get 
 * references to prototypes commonly used by the layout generators such as the 
 * metal 1 arc proto or the metal 1 pin proto. The Tech class also holds 
 * technology dependant dimensions such as the width of a diffusion contact.
 * <p>  The TechType class serves two purposes. First, it makes it convenient
 *  to access technology dependent information. Second, it hides foundry 
 *  specific information that we're not allowed to distribute as open source 
 *  software. */
public abstract class TechType implements Serializable {
    private static class ArcPair implements Serializable {
        private static final long serialVersionUID = 0;

        private ArcProto arc1, arc2;
        public ArcPair(ArcProto a1, ArcProto a2) {arc1=a1; arc2=a2;}
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArcPair)) return false;
            ArcPair ap = (ArcPair)o;
            if (ap.arc1==arc1 && ap.arc2==arc2) return true;
            if (ap.arc1==arc2 && ap.arc2==arc1) return true;
            return false;
        }
        @Override
        public int hashCode() {return arc1.hashCode()*arc2.hashCode();}
    }
	//----------------------------- public data ----------------------------------
	private static final Variable.Key ATTR_X = Variable.newKey("ATTR_X");
	private static final Variable.Key ATTR_S = Variable.newKey("ATTR_S");
	private static final Variable.Key ATTR_SN = Variable.newKey("ATTR_SN");
	private static final Variable.Key ATTR_SP = Variable.newKey("ATTR_SP");

    private final Technology generic = Technology.findTechnology("generic");
    private final Technology technology;
    private final TechTypeEnum techEnum;
    private final PrimitiveNode essentialBounds =
        generic.findNodeProto("Essential-Bounds");
    private final PrimitiveNode facetCenter =
        generic.findNodeProto("Facet-Center");

    private final int nbLay;
    private final ArcProto[] layers;
    private final PrimitiveNode[] vias;
    private final HashMap<ArcPair,PrimitiveNode> viaMap = new HashMap<ArcPair,PrimitiveNode>();

    /** layers
     *
     * Poly and metal are considered to be routing layers.  In contrast
     * we assume we never want to route in diffusion or well.  This
     * allows us to assign a unique "height" to each of the routing
     * layers.  Layers at the same height can connect directly.  Layers
     * at adjacent heights can connect using vias.
     *
     * For now, well and diffusion don't have heights. */
    private final ArcProto pdiff, ndiff, p1, m1, m2, m3, m4, m5, m6, m7, m8, m9,
        ndiff18, pdiff18, ndiff25, pdiff25, ndiff33, pdiff33;

    /** layer pins */
    private final PrimitiveNode ndpin, pdpin, p1pin, m1pin, m2pin, m3pin,
        m4pin, m5pin, m6pin, m7pin, m8pin, m9pin;

    /** vias */
    private final PrimitiveNode nwm1, pwm1, nwm1Y, pwm1Y, ndm1, pdm1, p1m1,
        m1m2, m2m3, m3m4, m4m5, m5m6, m6m7, m7m8, m8m9;

    /** Transistors */
    private final PrimitiveNode nmos, pmos, nmos18, pmos18, nmos25, pmos25,
        nmos33, pmos33;
    // nvth, pvth, nvtl, pvtl, nnat, pnat;

    /** special threshold transistor contacts */
    private final PrimitiveNode nmos18contact, pmos18contact, nmos25contact,
        pmos25contact, nmos33contact, pmos33contact;

    /** Pure layer nodes for Well and Select */
    private final PrimitiveNode nwell, pwell;

    /** Layer nodes are sometimes used to patch notches */
    private final PrimitiveNode m1Node, m2Node, m3Node, m4Node, m5Node, m6Node,
        m7Node, m8Node, m9Node, p1Node, pdNode, ndNode, pselNode, nselNode;

    /** Transistor layer nodes */
    private final PrimitiveNode od18, od25, od33, vth, vtl;

    //-------------------- Technology dependent dimensions  -------------------
    protected double
        // Gilda: gate length depending on foundry
        gateLength,

        // Wire offset from center of the poly contact to form a L-shape arc to gate
        offsetLShapePolyContact, offsetTShapePolyContact,

        // denote Select spacing rule
        selectSpace,

        // surround distance of select from active in transistor
        selectSurroundDiffInTrans, selectSurroundDiffAlongGateInTrans,

        // select surround over poly. PP/NP.R.1 in 90nm
        selectSurround;

    // RKao my first attempt to embed technology specific dimensions
    protected double
        wellSurroundDiff,
        gateExtendPastMOS,
        p1Width,
        p1ToP1Space,
        gateToGateSpace,
        gateToDiffContSpace,
        gateToDiffContSpaceDogBone,
        selectSurroundDiffInActiveContact,	// select surround in active contacts
        m1MinArea, 							// min area rules, sq lambda
        diffCont_m1Width,					// width of m1 of min sized diff contact  
        diffContIncr;						// when diff cont increases by diffContIncr,
                                            // we get an additional cut

    //----------------------------- private methods  -----------------------------
    private static void error(boolean pred, String msg) {
        Job.error(pred, msg);
    }
    private ArcProto getLayer(int n) {
        return n>(layers.length-1) ? null : layers[n];
    }
    private PrimitiveNode getVia(int n) {
        return n>(vias.length-1) ? null : vias[n];
    }
    /**
     * get the PrimitiveNode of a particular type that connects to the
     * complete set of wires given
     */
    private PrimitiveNode findNode(PrimitiveNode.Function type,
                                   ArcProto[] arcs, Technology tech) {
        for (Iterator<PrimitiveNode> it=tech.getNodes(); it.hasNext();) {
            PrimitiveNode pn = it.next();
            boolean found = true;
            if (pn.getFunction() == type) {
                for (int j=0; j<arcs.length; j++) {
                    if (pn.connectsTo(arcs[j]) == null) {
                        found = false;
                        break;
                    }
                }
                if (found) return pn;
            }
        }
        return null;
    }

    private PrimitiveNode findPin(ArcProto arc) {
        return arc==null ? null : arc.findPinProto();
    }

    private void putViaMap(ArcProto arc1, ArcProto arc2, PrimitiveNode via) {
        if (arc1==null || arc2==null || via==null) return;
        ArcPair ap = new ArcPair(arc1, arc2);
        error(viaMap.containsKey(ap), "two contacts for same pair of arcs?");
        viaMap.put(ap, via);
    }

    // initialize map from pair of layers to via that connects them
    private void initViaMap() {
        putViaMap(m1, m2, m1m2);
        putViaMap(m2, m3, m2m3);
        putViaMap(m3, m4, m3m4);
        putViaMap(m4, m5, m4m5);
        putViaMap(m5, m6, m5m6);
        putViaMap(m6, m7, m6m7);
        putViaMap(m7, m8, m7m8);
        putViaMap(m8, m9, m8m9);
        putViaMap(ndiff, m1, ndm1);
        putViaMap(pdiff, m1, pdm1);
        putViaMap(p1, m1, p1m1);
    }

    protected TechType(Technology techy, TechTypeEnum techEnum, String[] layerNms) {
        // This error could happen when there are XML errors while uploading the technologies.
        error((techy==null), "Null technology in TechType constructor");
        
        // I can't break this into subroutines because most data members are
        // final.
        nbLay = layerNms.length;
        technology = techy;
        this.techEnum = techEnum;

        //--------------------------- initialize layers -----------------------
        layers = new ArcProto[nbLay];
        for (int i=0; i<nbLay; i++) {
            layers[i] = techy.findArcProto(layerNms[i]);
            error(layers[i]==null, "No such layer: " + layerNms[i] + " in technology " + techy.getTechName());
        }
        p1 = getLayer(0);
        m1 = getLayer(1);
        m2 = getLayer(2);
        m3 = getLayer(3);
        m4 = getLayer(4);
        m5 = getLayer(5);
        m6 = getLayer(6);
        m7 = getLayer(7);
        m8 = getLayer(8);
        m9 = getLayer(9);

        pdiff = techy.findArcProto("P-Active");
        ndiff = techy.findArcProto("N-Active");
        ndiff18 = techy.findArcProto("thick-OD18-N-Active");
        pdiff18 = techy.findArcProto("thick-OD18-P-Active");
        ndiff25 = techy.findArcProto("thick-OD25-N-Active");
        pdiff25 = techy.findArcProto("thick-OD25-P-Active");
        ndiff33 = techy.findArcProto("thick-OD33-N-Active");
        pdiff33 = techy.findArcProto("thick-OD33-P-Active");

        // Layer Nodes
        m1Node = techy.findNodeProto("Metal-1-Node");
        m2Node = techy.findNodeProto("Metal-2-Node");
        m3Node = techy.findNodeProto("Metal-3-Node");
        m4Node = techy.findNodeProto("Metal-4-Node");
        m5Node = techy.findNodeProto("Metal-5-Node");
        m6Node = techy.findNodeProto("Metal-6-Node");
        m7Node = techy.findNodeProto("Metal-7-Node");
        m8Node = techy.findNodeProto("Metal-8-Node");
        m9Node = techy.findNodeProto("Metal-9-Node");
        p1Node = techy.findNodeProto("Polysilicon-1-Node");
        pdNode = techy.findNodeProto("P-Active-Node");
        ndNode = techy.findNodeProto("N-Active-Node");
        nselNode = techy.findNodeProto("N-Select-Node");
        pselNode = techy.findNodeProto("P-Select-Node");

        //--------------------------- initialize pins -------------------------
        pdpin = findPin(pdiff);
        ndpin = findPin(ndiff);
        p1pin = findPin(p1);
        m1pin = findPin(m1);
        m2pin = findPin(m2);
        m3pin = findPin(m3);
        m4pin = findPin(m4);
        m5pin = findPin(m5);
        m6pin = findPin(m6);
        m7pin = findPin(m7);
        m8pin = findPin(m8);
        m9pin = findPin(m9);

        //--------------------------- initialize vias -------------------------
        vias = new PrimitiveNode[nbLay - 1];
        for (int i = 0; i < nbLay - 1; i++) {
            vias[i] = findNode(PrimitiveNode.Function.CONTACT,
                               new ArcProto[] {layers[i], layers[i+1]},
                               techy);
            error(vias[i] == null, "No via for layer: " + layerNms[i]);
        }

        p1m1 = getVia(0);
        m1m2 = getVia(1);
        m2m3 = getVia(2);
        m3m4 = getVia(3);
        m4m5 = getVia(4);
        m5m6 = getVia(5);
        m6m7 = getVia(6);
        m7m8 = getVia(7);
        m8m9 = getVia(8);

        ndm1 = techy.findNodeProto("Metal-1-N-Active-Con");
        pdm1 = techy.findNodeProto("Metal-1-P-Active-Con");
        nwm1 = techy.findNodeProto("Metal-1-N-Well-Con");
        pwm1 = techy.findNodeProto("Metal-1-P-Well-Con");
        nwm1Y = techy.findNodeProto("Y-Metal-1-N-Well-Con");
        pwm1Y = techy.findNodeProto("Y-Metal-1-P-Well-Con");

        // initialize special threshold transistor contacts
        nmos18contact = techy.findNodeProto("thick-OD18-Metal-1-N-Active-Con");
        pmos18contact = techy.findNodeProto("thick-OD18-Metal-1-P-Active-Con");
        nmos25contact = techy.findNodeProto("thick-OD25-Metal-1-N-Active-Con");
        pmos25contact = techy.findNodeProto("thick-OD25-Metal-1-P-Active-Con");
        nmos33contact = techy.findNodeProto("thick-OD33-Metal-1-N-Active-Con");
        pmos33contact = techy.findNodeProto("thick-OD33-Metal-1-P-Active-Con");

        initViaMap();

        //------------------------ initialize transistors ---------------------
        nmos = techy.findNodeProto("N-Transistor");
        pmos = techy.findNodeProto("P-Transistor");
        nmos18 = techy.findNodeProto("OD18-N-Transistor");
        pmos18 = techy.findNodeProto("OD18-P-Transistor");
        nmos25 = techy.findNodeProto("OD25-N-Transistor");
        pmos25 = techy.findNodeProto("OD25-P-Transistor");
        nmos33 = techy.findNodeProto("OD33-N-Transistor");
        pmos33 = techy.findNodeProto("OD33-P-Transistor");

        // transistor layers
        od18 = techy.findNodeProto("OD18-Node");
        od25 = techy.findNodeProto("OD25-Node");
        od33 = techy.findNodeProto("OD33-Node");
        vth = techy.findNodeProto("VTH-Node");
        vtl = techy.findNodeProto("VTL-Node");

        //--------------------------- initialize well -------------------------
        nwell = techy.findNodeProto("N-Well-Node");
        pwell = techy.findNodeProto("P-Well-Node");
    }

    //---------------------------- public classes -----------------------------
    /** Hide the differences between technologies. A MosInst's gate is always
     * vertical. */
    public static class MosInst {
        private final NodeInst mos;
        private final String leftDiff, rightDiff, topPoly, botPoly;
        private static void error(boolean pred, String msg) {
            Job.error(pred, msg);
        }
        private MosInst(char np, double x, double y, double xSize, double ySize,
                        double angle,
                        String leftDiff, String rightDiff,
                        String topPoly, String botPoly,
                        TechType tech,
                        Cell parent) {
            NodeProto npr = np=='n' ? tech.nmos() : tech.pmos();
            this.leftDiff = leftDiff;
            this.rightDiff = rightDiff;
            this.topPoly = topPoly;
            this.botPoly = botPoly;
            mos = LayoutLib.newNodeInst(npr, x, y, xSize, ySize, angle, parent);
        }
        private PortInst getPort(String portNm) {
            PortInst pi = mos.findPortInst(portNm);
            error(pi==null, "MosInst can't find port!");
            return pi;
        }
        public PortInst leftDiff() {return getPort(leftDiff);}
        public PortInst rightDiff() {return getPort(rightDiff);}
        public PortInst topPoly() {return getPort(topPoly);}
        public PortInst botPoly() {return getPort(botPoly);}

        public static class MosInstV extends MosInst {
            public MosInstV(char np, double x, double y, double w, double l,
            		        TechType tech, Cell parent) {
                super(np, x, y, l, w, 0,
                      "diff-left", "diff-right", "poly-top", "poly-bottom",
                      tech, parent);
            }
        }
        public static class MosInstH extends MosInst {
            public MosInstH(char np, double x, double y, double w, double l,
                            TechType tech, Cell parent) {
                super(np, x, y, w, l, 90,
                      np+"-trans-diff-top", np+"-trans-diff-bottom",
                      np+"-trans-poly-right", np+"-trans-poly-left",
                      tech, parent);
            }
        }
        public static class MosInstH1 extends MosInst {
            public MosInstH1(char np, double x, double y, double w, double l,
                            TechType tech, Cell parent) {
                super(np, x, y, w, l, 90,
                      "diff-top", "diff-bottom",
                      "poly-right", "poly-left",
                      tech, parent);
            }
        }
    }

    //------------------------------ public data ------------------------------

    /** These are the Electric technologies understood by the gate layout
     * generators */
    public static enum TechTypeEnum {MOCMOS, TSMC180, CMOS90;
        private TechType type;
        public TechType getTechType()
        {
            loadTechType();
            return type;
        }
        private void loadTechType()
        {
            if (type != null) return; // loaded
            if (this == TechTypeEnum.MOCMOS)
                type = new TechTypeMoCMOS(this);
            else if (this == TechTypeEnum.TSMC180)
                type = getTechTypeTSMC180(this);
            else if (this == TechTypeEnum.CMOS90)
                type = getTechTypeCMOS90(this);
            else {
                System.out.println("Invalid TechTypeEnum");
                assert(false);
            }
        }

        public static TechTypeEnum getTechTypeEnumFromTechnology(Technology tech)
        {
            for (TechTypeEnum t : TechTypeEnum.values())
            {
                // not loaded yet
                if (t.type == null)
                    t.loadTechType();
                if (t.type.getTechnology() == tech)
                    return t;
            }
            return null;
        }
    }

    private static TechType getTechTypeTSMC180(TechTypeEnum techEnum)
    {
        TechType tsmc180 = null;
        try
        {
            Class tsmc180Class = Class.forName("com.sun.electric.plugins.tsmc.TechTypeTSMC180");
            Constructor<TechType> techConstr = tsmc180Class.getConstructor(TechTypeEnum.class);
            tsmc180 = techConstr.newInstance(techEnum);
//            java.lang.reflect.Field techField = tsmc180Class.getDeclaredField("TSMC180");
//            tsmc180 = (TechType) techField.get(null);
         } catch (Exception e)
        {
            assert(false); // runtime error
        }
         return tsmc180;
    }

    private static TechType getTechTypeCMOS90(TechTypeEnum techEnum)
    {
        TechType cmos90 = null;
        try
		{
			Class cmos90Class = Class.forName("com.sun.electric.plugins.tsmc.TechTypeCMOS90");
            Constructor<TechType> techConstr = cmos90Class.getConstructor(TechTypeEnum.class);
            cmos90 = techConstr.newInstance(techEnum);
//			java.lang.reflect.Field techField = cmos90Class.getDeclaredField("CMOS90");
//			cmos90 = (TechType) techField.get(null);
         } catch (Exception e)
        {
            assert(false); // runtime error
        }
 		return cmos90;
    }

    //----------------------------- public methods ----------------------------

    public abstract int getNumMetals();
    public Technology getTechnology() {return technology;}
    public TechTypeEnum getEnum() {return techEnum; }

    /** layers */
    public ArcProto pdiff() {return pdiff;}
    public ArcProto ndiff() {return ndiff;}
    public ArcProto p1() {return p1;}
    public ArcProto m1() {return m1;}
    public ArcProto m2() {return m2;}
    public ArcProto m3() {return m3;}
    public ArcProto m4() {return m4;}
    public ArcProto m5() {return m5;}
    public ArcProto m6() {return m6;}
    public ArcProto m7() {return m7;}
    public ArcProto m8() {return m8;}
    public ArcProto m9() {return m9;}
    public ArcProto ndiff18() {return ndiff18;}
    public ArcProto pdiff18() {return pdiff18;}
    public ArcProto ndiff25() {return ndiff25;}
    public ArcProto pdiff25() {return pdiff25;}
    public ArcProto ndiff33() {return ndiff33;}
    public ArcProto pdiff33() {return pdiff33;}

    /** pins */
    public PrimitiveNode ndpin() {return ndpin;}
    public PrimitiveNode pdpin() {return pdpin;}
    public PrimitiveNode p1pin() {return p1pin;}
    public PrimitiveNode m1pin() {return m1pin;}
    public PrimitiveNode m2pin() {return m2pin;}
    public PrimitiveNode m3pin() {return m3pin;}
    public PrimitiveNode m4pin() {return m4pin;}
    public PrimitiveNode m5pin() {return m5pin;}
    public PrimitiveNode m6pin() {return m6pin;}
    public PrimitiveNode m7pin() {return m7pin;}
    public PrimitiveNode m8pin() {return m8pin;}
    public PrimitiveNode m9pin() {return m9pin;}

    /** vias */
    public PrimitiveNode nwm1() {return nwm1;}
    public PrimitiveNode pwm1() {return pwm1;}
    public PrimitiveNode nwm1Y() {return nwm1Y;}
    public PrimitiveNode pwm1Y() {return pwm1Y;}
    public PrimitiveNode ndm1() {return ndm1;}
    public PrimitiveNode pdm1() {return pdm1;}
    public PrimitiveNode p1m1() {return p1m1;}
    public PrimitiveNode m1m2() {return m1m2;}
    public PrimitiveNode m2m3() {return m2m3;}
    public PrimitiveNode m3m4() {return m3m4;}
    public PrimitiveNode m4m5() {return m4m5;}
    public PrimitiveNode m5m6() {return m5m6;}
    public PrimitiveNode m6m7() {return m6m7;}
    public PrimitiveNode m7m8() {return m7m8;}
    public PrimitiveNode m8m9() {return m8m9;}

    /** Transistors */
    public PrimitiveNode nmos() {return nmos;}
    public PrimitiveNode pmos() {return pmos;}
    public PrimitiveNode nmos18() {return nmos18;}
    public PrimitiveNode pmos18() {return pmos18;}
    public PrimitiveNode nmos25() {return nmos25;}
    public PrimitiveNode pmos25() {return pmos25;}
    public PrimitiveNode nmos33() {return nmos33;}
    public PrimitiveNode pmos33() {return pmos33;}

    /** special threshold transistor contacts */
    public PrimitiveNode nmos18contact() {return nmos18contact;}
    public PrimitiveNode pmos18contact() {return pmos18contact;}
    public PrimitiveNode nmos25contact() {return nmos25contact;}
    public PrimitiveNode pmos25contact() {return pmos25contact;}
    public PrimitiveNode nmos33contact() {return nmos33contact;}
    public PrimitiveNode pmos33contact() {return pmos33contact;}

    /** Well */
    public PrimitiveNode nwell() {return nwell;}
    public PrimitiveNode pwell() {return pwell;}

    /** Layer nodes are sometimes used to patch notches */
    public PrimitiveNode m1Node() {return m1Node;}
    public PrimitiveNode m2Node() {return m2Node;}
    public PrimitiveNode m3Node() {return m3Node;}
    public PrimitiveNode m4Node() {return m4Node;}
    public PrimitiveNode m5Node() {return m5Node;}
    public PrimitiveNode m6Node() {return m6Node;}
    public PrimitiveNode m7Node() {return m7Node;}
    public PrimitiveNode m8Node() {return m8Node;}
    public PrimitiveNode m9Node() {return m9Node;}
    public PrimitiveNode p1Node() {return p1Node;}
    public PrimitiveNode pdNode() {return pdNode;}
    public PrimitiveNode ndNode() {return ndNode;}
    public PrimitiveNode pselNode() {return pselNode;}
    public PrimitiveNode nselNode() {return nselNode;}

    /** Transistor layer nodes */
    public PrimitiveNode od18() {return od18;}
    public PrimitiveNode od25() {return od25;}
    public PrimitiveNode od33() {return od33;}
    public PrimitiveNode vth() {return vth;}
    public PrimitiveNode vtl() {return vtl;}

    /** Essential-Bounds */
    public PrimitiveNode essentialBounds() {return essentialBounds;}

    /** Facet-Center */
    public PrimitiveNode facetCenter() {return facetCenter;}

    public PrimitiveNode getViaFor(ArcProto a1, ArcProto a2) {
        return viaMap.get(new ArcPair(a1, a2));
    }

    public int layerHeight(ArcProto p) {
        for (int i = 0; i < nbLay; i++) {
            if (layers[i] == p)
                return i;
        }
        error(true, "Can't find layer: " + p);
        return -1;
    }

    public ArcProto closestLayer(PortProto port, ArcProto layer) {
        int h = layerHeight(layer);
        for (int dist = 0; dist < nbLay; dist++) {
            int lookUp = h + dist;
            int lookDn = h - dist;
            if (lookUp < nbLay) {
                ArcProto lay = layerAtHeight(lookUp);
                if (port.connectsTo(lay))
                    return lay;
            }
            if (lookDn >= 0) {
                ArcProto lay = layerAtHeight(lookDn);
                if (port.connectsTo(lay))
                    return lay;
            }
        }
        error(true, "port can't connect to any layer?!!");
        return null;
    }
    
    public ArcProto highestLayer(PortProto port) {
    	for (int h=layers.length-1; h>=0; h--) {
    		if (port.connectsTo(layers[h])) return layers[h];
    	}
    	error(true, "port can't connect to any layer?!!");
    	return null;
    }
    
    public ArcProto layerAtHeight(int layHeight) {return layers[layHeight];	}

    public PrimitiveNode viaAbove(int layHeight) {return vias[layHeight];}

    public PrimitiveNode viaBelow(int layHeight) {return vias[layHeight - 1];}

    /** round to avoid MOCMOS CIF resolution errors */
    public abstract double roundToGrid(double x);

    public abstract MosInst newNmosInst(double x, double y,
                                        double w, double l, Cell parent);
    public abstract MosInst newPmosInst(double x, double y,
                                        double w, double l, Cell parent);
    public abstract String name();

    public abstract double reservedToLambda(int layer, double nbTracks);
    
	/** @return min width of Well */
	public double getWellWidth() {return nwm1.getMinSizeRule().getWidth();}
	/** @return amount that well surrounds diffusion */
	public double getWellSurroundDiff() {return wellSurroundDiff;}
	/** @return MOS edge to gate edge */
    public double getGateExtendPastMOS() {return gateExtendPastMOS;}
    /** @return min width of polysilicon 1 */
    public double getP1Width() {return p1Width;}
    /** @return min spacing between polysilicon 1 */
    public double getP1ToP1Space() {return p1ToP1Space;}
    /** @return min spacing between gates of series transistors */ 
    public double getGateToGateSpace() {return gateToGateSpace;}
    /** @return min spacing between MOS gate and diffusion edge of diff contact */
    public double getGateToDiffContSpace() {return gateToDiffContSpace;}
    /** @return min spacing between MOS gate and diffusion edge of diff contact
     * when the diffusion width is larger than the gate width */
    public double getGateToDiffContSpaceDogBone() {return gateToDiffContSpaceDogBone;}
    /** @return min width of diffusion surrounding diff contact */
    public double getDiffContWidth() {
        SizeOffset so = ndm1().getProtoSizeOffset();
        return (ndm1().getMinSizeRule().getHeight() - so.getHighYOffset() - so.getLowYOffset());
    }
    /** @return min width of poly contact */
    public double getP1M1Width() {
        SizeOffset so = p1m1().getProtoSizeOffset();
        return (p1m1().getMinSizeRule().getHeight() - so.getHighYOffset() - so.getLowYOffset());
    }
    /** @return gate length that depends on foundry */
    public double getGateLength() {return gateLength;}
    /** @return amount that select surrounds diffusion in well? */
    public double selectSurroundDiffInActiveContact() {return selectSurroundDiffInActiveContact;}
    /** @return amount that Select surrounds MOS, along gate width dimension */
    public double selectSurroundDiffAlongGateInTrans() {return selectSurroundDiffAlongGateInTrans;}
    /** @return y offset of poly arc connecting poly contact and gate in a L-Shape case */
    public double getPolyLShapeOffset() {return offsetLShapePolyContact;}
    /** @return y offset of poly arc connecting poly contact and gate in a T-Shape case */
    public double getPolyTShapeOffset() {return offsetTShapePolyContact;}
    /** @return select spacing rule */
    public double getSelectSpacingRule() {return selectSpace;}
    /** @return select surround active in transistors but not along the gate */
    public double getSelectSurroundDiffInTrans() {return selectSurroundDiffInTrans;}
    /** @return select surround over poly */
    public double getSelectSurroundOverPoly() {return selectSurround;}
    /** @return minimum metal1 area (sq lambda) */
    public double getM1MinArea() {return m1MinArea;}
    /** @return width of metal-1 in min sized diffusion contact */
    public double getDiffCont_m1Width() {return diffCont_m1Width;}
    /** @return amount diffusion contact grows to accomodate an one additional contact cut */ 
    public double getDiffContIncr() {return diffContIncr;}
    
    public Variable.Key getAttrX() {return ATTR_X;}
    public Variable.Key getAttrS() {return ATTR_S;}
    public Variable.Key getAttrSP() {return ATTR_SP;}
    public Variable.Key getAttrSN() {return ATTR_SN;}


}
