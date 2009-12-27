/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParasiticTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Parasitics" tab of the Preferences dialog.
 */
public class ParasiticTab extends PreferencePanel {

	private JList layerList;
	private DefaultListModel layerListModel;
	private boolean changing;

	/** Creates new form ParasiticTab */
	public ParasiticTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(resistance);
	    EDialog.makeTextFieldSelectAllOnTab(capacitance);
	    EDialog.makeTextFieldSelectAllOnTab(edgeCapacitance);
	    EDialog.makeTextFieldSelectAllOnTab(minResistance);
	    EDialog.makeTextFieldSelectAllOnTab(minCapacitance);
	    EDialog.makeTextFieldSelectAllOnTab(maxSeriesResistance);
	    EDialog.makeTextFieldSelectAllOnTab(gateLengthSubtraction);
	}

	/** return the JPanel to use for the user preferences. */
	public JPanel getUserPreferencesPanel() { return preferences; }

	/** return the JPanel to use for the project preferences. */
	public JPanel getProjectPreferencesPanel() { return projectSettings; }

	/** return the name of this preferences tab. */
	public String getName() { return "Parasitic"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	public void init()
	{
		// user preferences
		verboseNaming.setSelected(Simulation.isParasiticsUseVerboseNaming());
		backannotateLayout.setSelected(Simulation.isParasiticsBackAnnotateLayout());
		extractPowerGround.setSelected(Simulation.isParasiticsExtractPowerGround());
		extractPowerGround.setEnabled(false);
        useExemptedNetsFile.setSelected(Simulation.isParasiticsUseExemptedNetsFile());
        ignoreExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        extractExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        ignoreExemptedNets.setSelected(Simulation.isParasiticsIgnoreExemptedNets());
        extractExemptedNets.setSelected(!Simulation.isParasiticsIgnoreExemptedNets());
        extractR.setSelected(Simulation.isParasiticsExtractsR());
        extractC.setSelected(Simulation.isParasiticsExtractsC());

        // the parasitics panel (not visible)
		maxDistValue.setText(TextUtils.formatDistance(ParasiticTool.getMaxDistance()));
		parasiticPanel.setVisible(false);
		
		// project preferences
		changing = false;
		layerListModel = new DefaultListModel();
		layerList = new JList(layerListModel);
		layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceLayer.setViewportView(layerList);
		layerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { layerListClick(); }
		});

		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			techSelection.addItem(tech.getTechName());
		}
		techSelection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { techChanged(); }
		});
		techSelection.setSelectedItem(Technology.getCurrent().getTechName());

        ParasiticLayerDocumentListener updateLayerParasitics = new ParasiticLayerDocumentListener();
		resistance.getDocument().addDocumentListener(updateLayerParasitics);
		capacitance.getDocument().addDocumentListener(updateLayerParasitics);
		edgeCapacitance.getDocument().addDocumentListener(updateLayerParasitics);

        ParasiticTechDocumentListener updateTechnologyGlobals = new ParasiticTechDocumentListener();
		minResistance.getDocument().addDocumentListener(updateTechnologyGlobals);
		minCapacitance.getDocument().addDocumentListener(updateTechnologyGlobals);
		maxSeriesResistance.getDocument().addDocumentListener(updateTechnologyGlobals);
		gateLengthSubtraction.getDocument().addDocumentListener(updateTechnologyGlobals);
		includeGate.addActionListener(updateTechnologyGlobals);
		includeGround.addActionListener(updateTechnologyGlobals);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	public void term()
	{
		ParasiticTool.setMaxDistance(TextUtils.atofDistance(maxDistValue.getText()));

		boolean b = verboseNaming.isSelected();
		if (b != Simulation.isParasiticsUseVerboseNaming()) Simulation.setParasiticsUseVerboseNaming(b);
		b = backannotateLayout.isSelected();
		if (b != Simulation.isParasiticsBackAnnotateLayout()) Simulation.setParasiticsBackAnnotateLayout(b);
		b = extractPowerGround.isSelected();
		if (b != Simulation.isParasiticsExtractPowerGround()) Simulation.setParasiticsExtractPowerGround(b);
        b = useExemptedNetsFile.isSelected();
        if (b != Simulation.isParasiticsUseExemptedNetsFile()) Simulation.setParasiticsUseExemptedNetsFile(b);
        b = ignoreExemptedNets.isSelected();
            Simulation.setParasiticsIgnoreExemptedNets(b);
        b = extractR.isSelected();
        if (b != Simulation.isParasiticsExtractsR())
            Simulation.setParasiticsExtractsR(b);
        b = extractC.isSelected();
        if (b != Simulation.isParasiticsExtractsC())
            Simulation.setParasiticsExtractsC(b);
    }

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		// user preferences
		if (Simulation.isFactoryParasiticsUseVerboseNaming() != Simulation.isParasiticsUseVerboseNaming())
			Simulation.setParasiticsUseVerboseNaming(Simulation.isFactoryParasiticsUseVerboseNaming());
		if (Simulation.isFactoryParasiticsBackAnnotateLayout() != Simulation.isParasiticsBackAnnotateLayout())
			Simulation.setParasiticsBackAnnotateLayout(Simulation.isFactoryParasiticsBackAnnotateLayout());
		if (Simulation.isFactoryParasiticsExtractPowerGround() != Simulation.isParasiticsExtractPowerGround())
			Simulation.setParasiticsExtractPowerGround(Simulation.isFactoryParasiticsExtractPowerGround());
		if (Simulation.isFactoryParasiticsUseExemptedNetsFile() != Simulation.isParasiticsUseExemptedNetsFile())
			Simulation.setParasiticsUseExemptedNetsFile(Simulation.isFactoryParasiticsUseExemptedNetsFile());
		if (Simulation.isFactoryParasiticsIgnoreExemptedNets() != Simulation.isParasiticsIgnoreExemptedNets())
			Simulation.setParasiticsIgnoreExemptedNets(Simulation.isFactoryParasiticsIgnoreExemptedNets());
		if (Simulation.isFactoryParasiticsExtractsR() != Simulation.isParasiticsExtractsR())
			Simulation.setParasiticsExtractsR(Simulation.isFactoryParasiticsExtractsR());
		if (Simulation.isFactoryParasiticsExtractsC() != Simulation.isParasiticsExtractsC())
			Simulation.setParasiticsExtractsC(Simulation.isFactoryParasiticsExtractsC());
		if (ParasiticTool.getFactoryMaxDistance() != ParasiticTool.getMaxDistance())
			ParasiticTool.setMaxDistance(ParasiticTool.getFactoryMaxDistance());
	}

	private void techChanged()
	{
		String techName = (String)techSelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		changing = true;
		minResistance.setText(getFormattedDouble(tech.getMinResistanceSetting()));
		minCapacitance.setText(getFormattedDouble(tech.getMinCapacitanceSetting()));
		gateLengthSubtraction.setText(TextUtils.formatDistance(getDouble(tech.getGateLengthSubtractionSetting())));
        maxSeriesResistance.setText(getFormattedDouble(tech.getMaxSeriesResistanceSetting()));
		includeGate.setSelected(getBoolean(tech.getGateIncludedSetting()));
		includeGround.setSelected(getBoolean(tech.getGroundNetIncludedSetting()));

		layerListModel.clear();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			layerListModel.addElement(layer.getName());
		}
		layerList.setSelectedIndex(0);
		layerListClick();
		changing = false;
	}

	private void layerListClick()
	{
		String techName = (String)techSelection.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		changing = true;
		String layerName = (String)layerList.getSelectedValue();
		Layer layer = tech.findLayer(layerName);
		if (layer != null)
		{
			resistance.setText(getFormattedDouble(layer.getResistanceSetting()));
			capacitance.setText(getFormattedDouble(layer.getCapacitanceSetting()));
			edgeCapacitance.setText(getFormattedDouble(layer.getEdgeCapacitanceSetting()));
		}
		changing = false;
	}
    
	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private class ParasiticLayerDocumentListener implements DocumentListener
	{
		private void change()
		{
			if (changing) return;
			// get the currently selected layer
			String techName = (String)techSelection.getSelectedItem();
			Technology tech = Technology.findTechnology(techName);
			if (tech == null) return;

			String layerName = (String)layerList.getSelectedValue();
			Layer layer = tech.findLayer(layerName);
			if (layer == null) return;

            setDouble(layer.getResistanceSetting(), TextUtils.atof(resistance.getText()));
            setDouble(layer.getCapacitanceSetting(), TextUtils.atof(capacitance.getText()));
            setDouble(layer.getEdgeCapacitanceSetting(), TextUtils.atof(edgeCapacitance.getText()));
		}

		public void changedUpdate(DocumentEvent e) { change(); }
		public void insertUpdate(DocumentEvent e) { change(); }
		public void removeUpdate(DocumentEvent e) { change(); }
	}

    /**
     * Class to handle special changes to per-layer parasitics.
     */
    private class ParasiticTechDocumentListener implements ActionListener, DocumentListener {
        public void actionPerformed(ActionEvent evt) { updateTechnologyGlobals(); }
        
        public void changedUpdate(DocumentEvent e) { updateTechnologyGlobals(); }
        public void insertUpdate(DocumentEvent e) { updateTechnologyGlobals(); }
        public void removeUpdate(DocumentEvent e) { updateTechnologyGlobals(); }
        
        private void updateTechnologyGlobals() {
            if (changing) return;
            String techName = (String)techSelection.getSelectedItem();
            Technology tech = Technology.findTechnology(techName);
            if (tech == null) return;
            
            setDouble(tech.getMinResistanceSetting(), TextUtils.atof(minResistance.getText()));
            setDouble(tech.getMinCapacitanceSetting(),TextUtils.atof(minCapacitance.getText()));
            setDouble(tech.getGateLengthSubtractionSetting(), TextUtils.atofDistance(gateLengthSubtraction.getText()));
            setDouble(tech.getMaxSeriesResistanceSetting(), TextUtils.atof(maxSeriesResistance.getText()));
            setBoolean(tech.getGateIncludedSetting(), includeGate.isSelected());
            setBoolean(tech.getGroundNetIncludedSetting(), includeGround.isSelected());
        }
    }

	/** This method is called from within the constructor to
	 * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        exemptedNetsGroup = new javax.swing.ButtonGroup();
        projectSettings = new javax.swing.JPanel();
        parasiticPanel = new javax.swing.JPanel();
        maxDist = new javax.swing.JLabel();
        maxDistValue = new javax.swing.JTextField();
        simpleParasiticOptions = new javax.swing.JPanel();
        verboseNaming = new javax.swing.JCheckBox();
        backannotateLayout = new javax.swing.JCheckBox();
        extractPowerGround = new javax.swing.JCheckBox();
        useExemptedNetsFile = new javax.swing.JCheckBox();
        ignoreExemptedNets = new javax.swing.JRadioButton();
        extractExemptedNets = new javax.swing.JRadioButton();
        extractR = new javax.swing.JCheckBox();
        extractC = new javax.swing.JCheckBox();
        preferences = new javax.swing.JPanel();
        techValues = new javax.swing.JPanel();
        spiceLayer = new javax.swing.JScrollPane();
        jLabel7 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        resistance = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        capacitance = new javax.swing.JTextField();
        edgeCapacitance = new javax.swing.JTextField();
        globalValues = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        minResistance = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        minCapacitance = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        gateLengthSubtraction = new javax.swing.JTextField();
        includeGate = new javax.swing.JCheckBox();
        includeGround = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        maxSeriesResistance = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        techSelection = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        projectSettings.setLayout(new java.awt.GridBagLayout());

        parasiticPanel.setLayout(new java.awt.GridBagLayout());

        parasiticPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Coupling Parasitics"));
        parasiticPanel.setEnabled(false);
        maxDist.setText("Maximum distance (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDist, gridBagConstraints);

        maxDistValue.setColumns(6);
        maxDistValue.setText("20");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDistValue, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        projectSettings.add(parasiticPanel, gridBagConstraints);

        simpleParasiticOptions.setLayout(new java.awt.GridBagLayout());

        simpleParasiticOptions.setBorder(javax.swing.BorderFactory.createTitledBorder("Simple Parasitics"));
        verboseNaming.setText("Use Verbose Naming");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(verboseNaming, gridBagConstraints);

        backannotateLayout.setText("Back-Annotate Layout");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(backannotateLayout, gridBagConstraints);

        extractPowerGround.setText("Extract Power/Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractPowerGround, gridBagConstraints);

        useExemptedNetsFile.setText("Use exemptedNets.txt file");
        useExemptedNetsFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                useExemptedNetsFileStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(useExemptedNetsFile, gridBagConstraints);

        exemptedNetsGroup.add(ignoreExemptedNets);
        ignoreExemptedNets.setText("Extract all but exempted nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleParasiticOptions.add(ignoreExemptedNets, gridBagConstraints);

        exemptedNetsGroup.add(extractExemptedNets);
        extractExemptedNets.setText("Extract only exempted nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleParasiticOptions.add(extractExemptedNets, gridBagConstraints);

        extractR.setText("Extract R");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractR, gridBagConstraints);

        extractC.setText("Extract C");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractC, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        projectSettings.add(simpleParasiticOptions, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        getContentPane().add(projectSettings, gridBagConstraints);

        preferences.setLayout(new java.awt.GridBagLayout());

        techValues.setLayout(new java.awt.GridBagLayout());

        techValues.setBorder(javax.swing.BorderFactory.createTitledBorder("Individual Layers"));
        spiceLayer.setMinimumSize(new java.awt.Dimension(200, 50));
        spiceLayer.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        techValues.add(spiceLayer, gridBagConstraints);

        jLabel7.setText("Layer:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        techValues.add(jLabel7, gridBagConstraints);

        jLabel11.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(jLabel11, gridBagConstraints);

        jLabel2.setText("Perimeter Cap (fF/um):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(jLabel2, gridBagConstraints);

        resistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(resistance, gridBagConstraints);

        jLabel12.setText("Area Cap (fF/um^2):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(jLabel12, gridBagConstraints);

        capacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(capacitance, gridBagConstraints);

        edgeCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        techValues.add(edgeCapacitance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        preferences.add(techValues, gridBagConstraints);

        globalValues.setLayout(new java.awt.GridBagLayout());

        globalValues.setBorder(javax.swing.BorderFactory.createTitledBorder("For All Layers"));
        jLabel20.setText("Min. Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(jLabel20, gridBagConstraints);

        minResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(minResistance, gridBagConstraints);

        jLabel21.setText("Min. Capacitance (fF):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(jLabel21, gridBagConstraints);

        minCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        globalValues.add(minCapacitance, gridBagConstraints);

        jLabel5.setText("Gate Length Shrink (Subtraction) um:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(jLabel5, gridBagConstraints);

        gateLengthSubtraction.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(gateLengthSubtraction, gridBagConstraints);

        includeGate.setText("Include Gate In Resistance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(includeGate, gridBagConstraints);

        includeGround.setText("Include Ground Network");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globalValues.add(includeGround, gridBagConstraints);

        jLabel1.setText("Max. Series Resistance: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        globalValues.add(jLabel1, gridBagConstraints);

        maxSeriesResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        globalValues.add(maxSeriesResistance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        preferences.add(globalValues, gridBagConstraints);

        jLabel3.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        preferences.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        preferences.add(techSelection, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(preferences, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void useExemptedNetsFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_useExemptedNetsFileStateChanged
        ignoreExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        extractExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
    }//GEN-LAST:event_useExemptedNetsFileStateChanged
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox backannotateLayout;
    private javax.swing.JTextField capacitance;
    private javax.swing.JTextField edgeCapacitance;
    private javax.swing.ButtonGroup exemptedNetsGroup;
    private javax.swing.JCheckBox extractC;
    private javax.swing.JRadioButton extractExemptedNets;
    private javax.swing.JCheckBox extractPowerGround;
    private javax.swing.JCheckBox extractR;
    private javax.swing.JTextField gateLengthSubtraction;
    private javax.swing.JPanel globalValues;
    private javax.swing.JRadioButton ignoreExemptedNets;
    private javax.swing.JCheckBox includeGate;
    private javax.swing.JCheckBox includeGround;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel maxDist;
    private javax.swing.JTextField maxDistValue;
    private javax.swing.JTextField maxSeriesResistance;
    private javax.swing.JTextField minCapacitance;
    private javax.swing.JTextField minResistance;
    private javax.swing.JPanel parasiticPanel;
    private javax.swing.JPanel preferences;
    private javax.swing.JPanel projectSettings;
    private javax.swing.JTextField resistance;
    private javax.swing.JPanel simpleParasiticOptions;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JComboBox techSelection;
    private javax.swing.JPanel techValues;
    private javax.swing.JCheckBox useExemptedNetsFile;
    private javax.swing.JCheckBox verboseNaming;
    // End of variables declaration//GEN-END:variables
    
}
