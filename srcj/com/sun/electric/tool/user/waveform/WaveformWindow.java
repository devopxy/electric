/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WaveformWindow.java
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
package com.sun.electric.tool.user.waveform;

import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.CodeExpression;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.EpicOut.EpicAnalysis;
import com.sun.electric.tool.io.input.SimulationData;
import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.ncc.NccCrossProbing;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Sample;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.DigitalAnalysis;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.ExplorerTree;
import com.sun.electric.tool.user.ui.ExplorerTreeModel;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.prefs.Preferences;

import javax.print.attribute.standard.ColorSupported;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.sun.electric.tool.io.*;
import java.io.*;

/**
 * This class defines the a screenful of Panels that make up a waveform display.
 */
public class WaveformWindow implements WindowContent, PropertyChangeListener
{
	/** minimum height of an Analog panel */				private static final int MINANALOGPANELSIZE = 30;
	/** minimum height of a Digital panel */				private static final int MINDIGITALPANELSIZE = 20;
	public static final boolean USETABLES = true;

	/** the window that this lives in */					private WindowFrame wf;
	/** the cell being simulated */							private Stimuli sd;
	/** the signal on all X axes (null for time) */			private Signal xAxisSignalAll;
	/** the top-level panel of the waveform window. */		private JPanel overall;
	/** the "lock X axis" button. */						private JButton xAxisLockButton;
	/** the "refresh" button. */							private JButton refresh;
	/** the "show points" button. */						private JButton showPoints;
	/** the "grow panel" button for widening. */			private JButton growPanel;
	/** the "shrink panel" button for narrowing. */			private JButton shrinkPanel;
	/** the list of panels. */								private JComboBox signalNameList;
	/** mapping from analysis to entries in "SIGNALS" tree*/private Map<Signal,TreePath> treePathFromSignal = new HashMap<Signal,TreePath>();
	/** true if rebuilding the list of panels */			private boolean rebuildingSignalNameList = false;
	/** the main scroll of all panels. */					private JScrollPane scrollAll;
	/** the split between signal names and traces. */		private JSplitPane split;
	/** left panel: the signal names */						private JPanel left;
	/** right panel: the signal traces */					private JPanel right;
	/** the table with panels and labels */					private WaveTable table;
	/** the table editor for left and right halves */		private WaveCellEditor leftSideColumn, rightSideColumn;
	/** the table with panels and labels */					private TableModel tableModel;
	/** labels for the text at the top */					private JLabel mainPos, extPos, delta, diskLabel;
	/** buttons for centering the X-axis cursors. */		private JButton centerMain, centerExt;
	/** a list of panels in this window */					private List<Panel> wavePanels;
	/** a list of sweep signals in this window */			private List<SweepSignal> sweepSignals;
	/** the main horizontal ruler for all panels. */		private HorizRuler mainHorizRulerPanel;
	/** true if the main horizontal ruler is logarithmic */	private boolean mainHorizRulerPanelLogarithmic;
	/** the VCR timer, when running */						private Timer vcrTimer;
	/** true to run VCR backwards */						private boolean vcrPlayingBackwards = false;
	/** time the VCR last advanced */						private long vcrLastAdvance;
	/** speed of the VCR (in screen pixels) */				private int vcrAdvanceSpeed = 3;
	/** current "main" x-axis cursor */						private double mainXPosition;
	/** current "extension" x-axis cursor */				private double extXPosition;
	/** default range along horozintal axis */				private double minXPosition, maxXPosition;
	/** true if the X axis is the same in each panel */		private boolean xAxisLocked;
	/** the sweep signal that is highlighted */				private int highlightedSweep = -1;
	/** display mode (0=lines, 1=lines&points, 2=points) */	private int linePointMode;
	/** true to show a grid (analog only) */				private boolean showGrid;
	/** the actual screen coordinates of the waveform */	private int screenLowX, screenHighX;
	/** a listener for redraw requests */					private WaveComponentListener wcl;
	/** The highlighter for this waveform window. */		private Highlighter highlighter;
	/** 0: color display, 1: color printing, 2: B&W printing */	private int nowPrinting;

	/** lock for crossprobing */							private static boolean freezeWaveformHighlighting = false;
	/** lock for crossprobing */							private static boolean freezeEditWindowHighlighting = false;
	/** The global listener for all waveform windows. */	private static WaveformWindowHighlightListener waveHighlighter = new WaveformWindowHighlightListener();
	/** Font for all text in the window */					private static Font waveWindowFont;
	/** For rendering text */								private static FontRenderContext waveWindowFRC;
	/** The colors of signal lines */						private static Color offStrengthColor, nodeStrengthColor, gateStrengthColor, powerStrengthColor;
	/** The background color */								private static Color backgroundColor;
	/** drop target (for drag and drop) */					public static WaveFormDropTarget waveformDropTarget = new WaveFormDropTarget();

	private static final ImageIcon iconAddPanel = Resources.getResource(WaveformWindow.class, "ButtonSimAddPanel.gif");
	private static final ImageIcon iconLockXAxes = Resources.getResource(WaveformWindow.class, "ButtonSimLockTime.gif");
	private static final ImageIcon iconUnLockXAxes = Resources.getResource(WaveformWindow.class, "ButtonSimUnLockTime.gif");
	private static final ImageIcon iconRefresh = Resources.getResource(WaveformWindow.class, "ButtonSimRefresh.gif");
	private static final ImageIcon iconLineOnPointOn = Resources.getResource(WaveformWindow.class, "ButtonSimLineOnPointOn.gif");
	private static final ImageIcon iconLineOnPointOff = Resources.getResource(WaveformWindow.class, "ButtonSimLineOnPointOff.gif");
	private static final ImageIcon iconLineOffPointOn = Resources.getResource(WaveformWindow.class, "ButtonSimLineOffPointOn.gif");
	private static final ImageIcon iconToggleGrid = Resources.getResource(WaveformWindow.class, "ButtonSimGrid.gif");
	private static final ImageIcon iconGrowPanel = Resources.getResource(WaveformWindow.class, "ButtonSimGrow.gif");
	private static final ImageIcon iconShrinkPanel = Resources.getResource(WaveformWindow.class, "ButtonSimShrink.gif");
	private static final ImageIcon iconVCRRewind = Resources.getResource(WaveformWindow.class, "ButtonVCRRewind.gif");
	private static final ImageIcon iconVCRPlayBackward = Resources.getResource(WaveformWindow.class, "ButtonVCRPlayBackward.gif");
	private static final ImageIcon iconVCRStop = Resources.getResource(WaveformWindow.class, "ButtonVCRStop.gif");
	private static final ImageIcon iconVCRPlay = Resources.getResource(WaveformWindow.class, "ButtonVCRPlay.gif");
	private static final ImageIcon iconVCRToEnd = Resources.getResource(WaveformWindow.class, "ButtonVCRToEnd.gif");
	private static final ImageIcon iconVCRFaster = Resources.getResource(WaveformWindow.class, "ButtonVCRFaster.gif");
	private static final ImageIcon iconVCRSlower = Resources.getResource(WaveformWindow.class, "ButtonVCRSlower.gif");

	private static final Cursor resizeRowCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
	private static final Cursor resizeColumnCursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);

	/**
	 * Constructor creates a Waveform window in a given WindowFrame with given Stimuli data.
	 * @param sd the Stimuli data to show in the window.
	 * @param wf the WindowFrame in which to place the window.
	 */
	public WaveformWindow(Stimuli sd, WindowFrame wf)
	{
		// initialize the structure
		this.wf = wf;
		this.sd = sd;
		sd.setWaveformWindow(this);
		resetSweeps();
		wavePanels = new ArrayList<Panel>();
		xAxisLocked = true;
		linePointMode = 0;
		nowPrinting = 0;
		showGrid = false;
		xAxisSignalAll = null;
		mainHorizRulerPanelLogarithmic = false;

		// compute static fields used in graphics
		waveWindowFont = new Font(User.getDefaultFont(), Font.PLAIN, 12);
		waveWindowFRC = new FontRenderContext(null, false, false);
		offStrengthColor = new Color(User.getColor(User.ColorPrefType.WAVE_OFF_STRENGTH));
		nodeStrengthColor = new Color(User.getColor(User.ColorPrefType.WAVE_NODE_STRENGTH));
		gateStrengthColor = new Color(User.getColor(User.ColorPrefType.WAVE_GATE_STRENGTH));
		powerStrengthColor = new Color(User.getColor(User.ColorPrefType.WAVE_POWER_STRENGTH));

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);

		Highlighter.addHighlightListener(waveHighlighter);

		// the total panel in the waveform window
		overall = new OnePanel(null, this);
		overall.setLayout(new GridBagLayout());
		wcl = new WaveComponentListener(overall);
		overall.addComponentListener(wcl);

		// a drop target for the overall waveform window
		new DropTarget(overall, DnDConstants.ACTION_LINK, waveformDropTarget, true);

		// the table that holds the waveform panels
		if (USETABLES)
		{
			tableModel = new WaveTableModel();
			table = new WaveTable(tableModel, this);
			new TableMouseListener(table);
			table.getTableHeader().setPreferredSize(new Dimension(1, 1));
			TableColumn column1 = table.getColumnModel().getColumn(0);
			column1.setPreferredWidth(100);
			TableColumn column2 = table.getColumnModel().getColumn(1);
			column2.setPreferredWidth(500);
			leftSideColumn = new WaveCellEditor(table, 0);
			rightSideColumn = new WaveCellEditor(table, 1);
			int height = User.getWaveformDigitalPanelHeight();
			if (sd.isAnalog()) height = User.getWaveformAnalogPanelHeight();
			table.setRowHeight(height);
			scrollAll = new JScrollPane(table);

			// a drop target for the table
			new DropTarget(table, DnDConstants.ACTION_LINK, WaveformWindow.waveformDropTarget, true);
		} else
		{
			// the left half has signal names; the right half has waveforms
			left = new JPanel();
			left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
			right = new JPanel();
			right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

			// the main part of the waveform window: a split-pane between names and waveforms, put into a scrollpane
			split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
			split.setResizeWeight(0.1);
			split.addPropertyChangeListener(this);
			scrollAll = new JScrollPane(split);
		}

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 2;
		gbc.gridwidth = 11;  gbc.gridheight = 1;
		gbc.weightx = 0;     gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		overall.add(scrollAll, gbc);

		if (sd.isAnalog())
		{
			// the top part of the waveform window: status information
			JButton addPanel = new JButton(iconAddPanel);
			addPanel.setBorderPainted(false);
			addPanel.setDefaultCapable(false);
			addPanel.setToolTipText("Create new waveform panel");
			Dimension minWid = new Dimension(iconAddPanel.getIconWidth()+4, iconAddPanel.getIconHeight()+4);
			addPanel.setMinimumSize(minWid);
			addPanel.setPreferredSize(minWid);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			overall.add(addPanel, gbc);
			addPanel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { makeNewPanel(); }
			});

			showPoints = new JButton(iconLineOnPointOff);
			showPoints.setBorderPainted(false);
			showPoints.setDefaultCapable(false);
			showPoints.setToolTipText("Toggle display of vertex points and lines");
			minWid = new Dimension(iconLineOnPointOff.getIconWidth()+4, iconLineOnPointOff.getIconHeight()+4);
			showPoints.setMinimumSize(minWid);
			showPoints.setPreferredSize(minWid);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;       gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			overall.add(showPoints, gbc);
			showPoints.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { toggleShowPoints(); }
			});

			JButton toggleGrid = new JButton(iconToggleGrid);
			toggleGrid.setBorderPainted(false);
			toggleGrid.setDefaultCapable(false);
			toggleGrid.setToolTipText("Toggle display of a grid");
			minWid = new Dimension(iconToggleGrid.getIconWidth()+4, iconToggleGrid.getIconHeight()+4);
			toggleGrid.setMinimumSize(minWid);
			toggleGrid.setPreferredSize(minWid);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;       gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			overall.add(toggleGrid, gbc);
			toggleGrid.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { toggleGridPoints(); }
			});
		}

		refresh = new JButton(iconRefresh);
		refresh.setBorderPainted(false);
		refresh.setDefaultCapable(false);
		refresh.setToolTipText("Reread stimuli data file and update waveforms");
		Dimension minWid = new Dimension(iconRefresh.getIconWidth()+4, iconRefresh.getIconHeight()+4);
		refresh.setMinimumSize(minWid);
		refresh.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(refresh, gbc);
		refresh.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { refreshData(); }
		});

		xAxisLockButton = new JButton(iconLockXAxes);
		xAxisLockButton.setBorderPainted(false);
		xAxisLockButton.setDefaultCapable(false);
		xAxisLockButton.setToolTipText("Lock all panels horizontally");
		minWid = new Dimension(iconLockXAxes.getIconWidth()+4, iconLockXAxes.getIconHeight()+4);
		xAxisLockButton.setMinimumSize(minWid);
		xAxisLockButton.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(xAxisLockButton, gbc);
		xAxisLockButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelXAxisLock(); }
		});

		signalNameList = new JComboBox();
		signalNameList.setToolTipText("Show or hide waveform panels");
		signalNameList.setLightWeightPopupEnabled(false);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		overall.add(signalNameList, gbc);
		signalNameList.addItem("Panel 1");
		signalNameList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { togglePanelName(); }
		});

		growPanel = new JButton(iconGrowPanel);
		growPanel.setBorderPainted(false);
		growPanel.setDefaultCapable(false);
		growPanel.setToolTipText("Increase minimum panel height");
		minWid = new Dimension(iconGrowPanel.getIconWidth()+4, iconGrowPanel.getIconHeight()+4);
		growPanel.setMinimumSize(minWid);
		growPanel.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 8;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(growPanel, gbc);
		growPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(1.25); }
		});

		shrinkPanel = new JButton(iconShrinkPanel);
		shrinkPanel.setBorderPainted(false);
		shrinkPanel.setDefaultCapable(false);
		shrinkPanel.setToolTipText("Decrease minimum panel height");
		minWid = new Dimension(iconShrinkPanel.getIconWidth()+4, iconShrinkPanel.getIconHeight()+4);
		shrinkPanel.setMinimumSize(minWid);
		shrinkPanel.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 9;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(shrinkPanel, gbc);
		shrinkPanel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { growPanels(0.8); }
		});

		// the X axis section that shows the value of the main and extension cursors
		JPanel xAxisLabelPanel = new JPanel();
		xAxisLabelPanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 10;      gbc.gridy = 0;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 0, 4);
		overall.add(xAxisLabelPanel, gbc);

		mainPos = new JLabel("Main:", JLabel.RIGHT);
		mainPos.setToolTipText("The main (dashed) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 0;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(mainPos, gbc);

		centerMain = new JButton("Center");
		centerMain.setToolTipText("Center the main (dashed) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(2, 4, 2, 0);
		xAxisLabelPanel.add(centerMain, gbc);
		centerMain.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(true); }
		});

		extPos = new JLabel("Ext:", JLabel.RIGHT);
		extPos.setToolTipText("The extension (dotted) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(extPos, gbc);

		centerExt = new JButton("Center");
		centerExt.setToolTipText("Center the extension (dotted) X axis cursor");
		gbc = new GridBagConstraints();
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(2, 4, 2, 0);
		xAxisLabelPanel.add(centerExt, gbc);
		centerExt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { centerCursor(false); }
		});

		delta = new JLabel("Delta:", JLabel.CENTER);
		delta.setToolTipText("X distance between cursors");
		gbc = new GridBagConstraints();
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		xAxisLabelPanel.add(delta, gbc);

		// the name of the waveform disk file
		if (sd.getFileURL() != null)
		{
			String fileName = TextUtils.getFileNameWithoutExtension(sd.getFileURL());
			String ext = TextUtils.getExtension(sd.getFileURL());
			if (ext.length() > 0) fileName += "." + ext;
			diskLabel = new JLabel("File: " + fileName, JLabel.CENTER);
			diskLabel.setToolTipText("The disk file that is being displayed");
			gbc = new GridBagConstraints();
			gbc.gridx = 5;       gbc.gridy = 0;
			gbc.weightx = 0.4;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(0, 10, 0, 0);
			xAxisLabelPanel.add(diskLabel, gbc);
		}

		// add VCR controls
		JButton vcrButtonRewind = new JButton(iconVCRRewind);
		vcrButtonRewind.setBorderPainted(false);
		vcrButtonRewind.setDefaultCapable(false);
		vcrButtonRewind.setToolTipText("Rewind main X axis cursor to start");
		minWid = new Dimension(iconVCRRewind.getIconWidth()+4, iconVCRRewind.getIconHeight()+4);
		vcrButtonRewind.setMinimumSize(minWid);
		vcrButtonRewind.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonRewind, gbc);
		vcrButtonRewind.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickRewind(); }
		});

		JButton vcrButtonPlayBackwards = new JButton(iconVCRPlayBackward);
		vcrButtonPlayBackwards.setBorderPainted(false);
		vcrButtonPlayBackwards.setDefaultCapable(false);
		vcrButtonPlayBackwards.setToolTipText("Play main X axis cursor backwards");
		minWid = new Dimension(iconVCRPlayBackward.getIconWidth()+4, iconVCRPlayBackward.getIconHeight()+4);
		vcrButtonPlayBackwards.setMinimumSize(minWid);
		vcrButtonPlayBackwards.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 4;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonPlayBackwards, gbc);
		vcrButtonPlayBackwards.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickPlayBackwards(); }
		});

		JButton vcrButtonStop = new JButton(iconVCRStop);
		vcrButtonStop.setBorderPainted(false);
		vcrButtonStop.setDefaultCapable(false);
		vcrButtonStop.setToolTipText("Stop moving main X axis cursor");
		minWid = new Dimension(iconVCRStop.getIconWidth()+4, iconVCRStop.getIconHeight()+4);
		vcrButtonStop.setMinimumSize(minWid);
		vcrButtonStop.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 5;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonStop, gbc);
		vcrButtonStop.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickStop(); }
		});

		JButton vcrButtonPlay = new JButton(iconVCRPlay);
		vcrButtonPlay.setBorderPainted(false);
		vcrButtonPlay.setDefaultCapable(false);
		vcrButtonPlay.setToolTipText("Play main X axis cursor");
		minWid = new Dimension(iconVCRPlay.getIconWidth()+4, iconVCRPlay.getIconHeight()+4);
		vcrButtonPlay.setMinimumSize(minWid);
		vcrButtonPlay.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 6;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonPlay, gbc);
		vcrButtonPlay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickPlay(); }
		});

		JButton vcrButtonToEnd = new JButton(iconVCRToEnd);
		vcrButtonToEnd.setBorderPainted(false);
		vcrButtonToEnd.setDefaultCapable(false);
		vcrButtonToEnd.setToolTipText("Move main X axis cursor to end");
		minWid = new Dimension(iconVCRToEnd.getIconWidth()+4, iconVCRToEnd.getIconHeight()+4);
		vcrButtonToEnd.setMinimumSize(minWid);
		vcrButtonToEnd.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 7;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonToEnd, gbc);
		vcrButtonToEnd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickToEnd(); }
		});

		JButton vcrButtonFaster = new JButton(iconVCRFaster);
		vcrButtonFaster.setBorderPainted(false);
		vcrButtonFaster.setDefaultCapable(false);
		vcrButtonFaster.setToolTipText("Move main X axis cursor faster");
		minWid = new Dimension(iconVCRFaster.getIconWidth()+4, iconVCRFaster.getIconHeight()+4);
		vcrButtonFaster.setMinimumSize(minWid);
		vcrButtonFaster.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 8;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonFaster, gbc);
		vcrButtonFaster.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickFaster(); }
		});

		JButton vcrButtonSlower = new JButton(iconVCRSlower);
		vcrButtonSlower.setBorderPainted(false);
		vcrButtonSlower.setDefaultCapable(false);
		vcrButtonSlower.setToolTipText("Move main X axis cursor slower");
		minWid = new Dimension(iconVCRSlower.getIconWidth()+4, iconVCRSlower.getIconHeight()+4);
		vcrButtonSlower.setMinimumSize(minWid);
		vcrButtonSlower.setPreferredSize(minWid);
		gbc = new GridBagConstraints();
		gbc.gridx = 9;       gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		overall.add(vcrButtonSlower, gbc);
		vcrButtonSlower.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { vcrClickSlower(); }
		});

		backgroundColor = vcrButtonSlower.getBackground();
		// the single horizontal ruler panel (when the X axes are locked)
		if (xAxisLocked)
		{
			addMainHorizRulerPanel();
		}

		// set bounds of the window from extent of the data
        double lowTime = sd.getMinTime();
        double highTime = sd.getMaxTime();
        double timeRange = highTime - lowTime;
        setMainXPositionCursor(timeRange*0.2 + lowTime);
        setExtensionXPositionCursor(timeRange*0.8 + lowTime);
        setDefaultHorizontalRange(lowTime, highTime);
	}

	private class WaveTable extends JTable
	{
		private WaveformWindow ww;

		WaveTable(TableModel model, WaveformWindow ww)
		{
			super(model);
			this.ww = ww;
		}
	}

	private class WaveTableModel extends DefaultTableModel
	{
		public int getColumnCount() { return 2; }

		public int getRowCount()
		{
			int numVisPanels = 0;
			for(Panel wp : wavePanels)
			{
				if (!wp.isHidden()) numVisPanels++;
			}
			return numVisPanels;
		}

		public Object getValueAt(int row, int col)
		{
			int rowNo = 0;
			for(Panel panel : wavePanels)
			{
				if (panel.isHidden()) continue;
				if (rowNo == row)
				{
					if (col == 0) return panel.getLeftHalf();
					return panel.getRightHalf();
				}
				rowNo++;
			}
			return null;
		}

		public void setValueAt(Object obj, int row, int col) {}

		public Class<?> getColumnClass(int column)
		{
			return JPanel.class;
		}
	}

	static class WaveCellEditor extends AbstractCellEditor
		implements TableCellRenderer, TableCellEditor
	{
		private Component lastOne;

		public WaveCellEditor(JTable table, int column)
		{
			super();
			TableColumnModel columnModel = table.getColumnModel();
			columnModel.getColumn(column).setCellRenderer(this);
			columnModel.getColumn(column).setCellEditor(this);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			return (Component)table.getValueAt(row, column);
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			lastOne = (Component)table.getValueAt(row, column);
			return lastOne;
		}

		public Object getCellEditorValue()
		{
			return lastOne;
		}
	}

	private class TableMouseListener implements MouseListener, MouseMotionListener
	{
		private int mouseXOffset, mouseYOffset, resizingRow;
		private TableColumn resizingColumn;
		private JTable table;

		public TableMouseListener(JTable table)
		{
			this.table = table;
			table.addMouseListener(this);
			table.addMouseMotionListener(this);
		}

		private TableColumn getResizingColumn(Point p)
		{
			int column = table.columnAtPoint(p);
			if (column == -1) return null;
			int row = table.rowAtPoint(p);
			if (row == -1) return null;
			Rectangle r = table.getCellRect(row, column, true);
			r.grow(-3, 0);
			if (r.contains(p)) return null;

			int midPoint = r.x + r.width / 2;
			int columnIndex = (p.x < midPoint) ? column - 1 : column;
			if (columnIndex == -1) return null;
			return table.getTableHeader().getColumnModel().getColumn(columnIndex);
		}

		private int getResizingRow(Point p)
		{
			int row = table.rowAtPoint(p);
			if (row == -1) return -1;
			int col = table.columnAtPoint(p);
			if (col == -1) return -1;
			Rectangle r = table.getCellRect(row, col, true);
			r.grow(0, -3);
			if (r.contains(p)) return -1;

			int midPoint = r.y + r.height / 2;
			int rowIndex = (p.y < midPoint) ? row - 1 : row;
			return rowIndex;
		}

		public void mouseClicked(MouseEvent e)
		{
			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void mouseEntered(MouseEvent e)
		{
			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void mouseExited(MouseEvent e)
		{
			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void mousePressed(MouseEvent e)
		{
			table.getTableHeader().setResizingColumn(null);

			// figure out if a divider was hit
			Point p = e.getPoint();
			resizingColumn = getResizingColumn(p);
			if (resizingColumn != null)
			{
				resizingRow = -1;
				table.getTableHeader().setResizingColumn(resizingColumn);
				mouseXOffset = p.x - resizingColumn.getWidth();
				return;
			}
			resizingRow = getResizingRow(p);
			if (resizingRow >= 0)
			{
				mouseYOffset = p.y - table.getRowHeight(resizingRow);
				return;
			}

			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void mouseMoved(MouseEvent e)
		{
			if (getResizingColumn(e.getPoint()) != null)
			{
				table.setCursor(resizeColumnCursor);
				return;
			}
			if (getResizingRow(e.getPoint()) >= 0)
			{
				table.setCursor(resizeRowCursor);
				return;
			}
			table.setCursor(null);

			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void mouseDragged(MouseEvent e)
		{
			if (resizingColumn != null)
			{
				resizingColumn.setWidth(e.getX() - mouseXOffset);
				if (mainHorizRulerPanel != null) mainHorizRulerPanel.repaint();
				return;
			}
			if (resizingRow >= 0)
			{
				int newHeight = e.getY() - mouseYOffset;
				if (newHeight > 0)
				{
					table.setRowHeight(resizingRow, newHeight);
					return;
				}
			}

			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void mouseReleased(MouseEvent e)
		{
			if (resizingColumn != null)
			{
				table.getTableHeader().setResizingColumn(null);
				return;
			}

			// forward event to the panel contents
			forwardEventToPanel(e);
		}

		public void forwardEventToPanel(MouseEvent e)
		{
			Point p = e.getPoint();
			int row = table.rowAtPoint(p);
			int col = table.columnAtPoint(p);
			if (row < 0 || col < 0) return;
			JPanel panel = (JPanel)table.getValueAt(row, col);

			// so clicks are felt inside the panels
			MouseEvent panelEvent = SwingUtilities.convertMouseEvent(table, e, panel);
			panel.dispatchEvent(panelEvent);

			// This is necessary so that when a button is pressed and released
			// it gets rendered properly.  Otherwise, the button may still appear
			// pressed down when it has been released.
			table.repaint();
		}
	}

	public void stopEditing()
	{
		leftSideColumn.stopCellEditing();
		rightSideColumn.stopCellEditing();
	}

	public void reloadTable()
	{
		table.tableChanged(new TableModelEvent(tableModel));
	}

	// ************************************* REQUIRED IMPLEMENTATION METHODS *************************************

	/**
	 * Method to get rid of this WaveformWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		for(Panel wp : wavePanels)
		{
			wp.finished();
		}
		overall.removeComponentListener(wcl);
		highlighter.delete();
		if (sd != null)
			sd.finished();
	}

	public void fullRepaint() { repaint(); }

	public void repaint()
	{
		for(Panel wp : wavePanels)
		{
			wp.repaintContents();
		}
		if (mainHorizRulerPanel != null)
			mainHorizRulerPanel.repaint();
	}

    public static void exportSimulationDataAsCSV(String file) 
    {
    	if (file == null) // cancel operation for example
    		return;
        WindowFrame current = WindowFrame.getCurrentWindowFrame();
        WindowContent content = current.getContent();
        if (!(content instanceof WaveformWindow)) {
            System.out.println("Must select a Waveform window first");
            return;
        }
        WaveformWindow ww = (WaveformWindow)content;
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
            for(Panel wp : ww.wavePanels)
                wp.dumpDataCSV(pw);
            pw.close();
            System.out.println("Exported Waveform in CSV format file '" + file + "'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void plotSimulationData(String file, String format) {
        WindowFrame current = WindowFrame.getCurrentWindowFrame();
        WindowContent content = current.getContent();
        if (!(content instanceof WaveformWindow)) {
            System.out.println("Must select a Waveform window first");
            return;
        }
        WaveformWindow ww = (WaveformWindow)content;
        try {
            String commands = "";
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            //double sr = 1;
            int numPanels = 0;
            int maxWidth = 0;
            int height = 0;
            for(Panel wp : ww.wavePanels) {
                numPanels++;
                min = Math.min(min, wp.convertXScreenToData(0));
                max = Math.max(max, wp.convertXScreenToData(wp.getSz().width));
                maxWidth =  Math.max(wp.getSz().width, maxWidth);
                height   += wp.getSz().height;
            }
            System.out.println("plotting: maxWidth="+maxWidth+", height="+height);
            if (file!=null) {
                commands += "set terminal "+format+" size 9 , "+(((double)height*9)/maxWidth)+"; ";
                commands += "set output \""+file+"\"; ";
            } else {
                commands += "set terminal aqua size "+(maxWidth+100)+" "+(height+100)+"; ";
            }
            commands += "unset colorbox; ";
            commands += "set multiplot; ";
            commands += "set xrange [\""+min+"\":\""+max+"\"]; ";
            commands += "set format x \"\"; ";
            System.out.println("Running: gnuplot for "+numPanels+" panels");
            ExecProcess ep = new ExecProcess(new String[] { "gnuplot" }, null);
            ep.redirectStdout(System.out);
            ep.redirectStderr(System.out);
            ep.start();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new ExecProcess.MultiOutputStream(new OutputStream[] { System.out, ep.getStdin() })));
            pw.println(commands);
            pw.println("set label 1 \"Voltage (Volts)\" at 1,1 left");
            pw.println();
            int whichPanel = 0;
            double ypos = 1.0;
            for(Panel wp : ww.wavePanels) {
                if (whichPanel==numPanels-1) {
                    pw.println("set xlabel \"time (in seconds)\"; ");
                    pw.println("unset format; ");
                }
                pw.println("set size "+
                           (((double)wp.getSz().width)/maxWidth)+
                           ","+
                           (((double)wp.getSz().height)/height)+"; ");
                ypos -= (((double)wp.getSz().height)/height);
                pw.println("set origin 0, "+ypos);
                pw.flush();
                pw.print("plot ");
                wp.dumpDataForGnuplot(pw, min, max, ",");
                pw.println();
                pw.flush();
                whichPanel++;
            }
            pw.println("unset multiplot; ");
            pw.println("quit;");
            pw.flush();
            pw.close();
            System.out.println("gnuplot finished.");
        } catch (Exception e) {
        	System.out.println("ERROR: Unable to run 'gnuplot': " + e);
        }
    }

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 * @param regExp true if the search string is a regular expression.
	 * @param whatToSearch a collection of text types to consider.
	 * @param codeRestr a restriction on types of Code to consider (null to consider all Code values).
	 * @param unitRestr a restriction on types of Units to consider (null to consider all Unit values).
	 * @param highlightedOnly true to search only in the highlighted area.
	 */
	public void initTextSearch(String search, boolean caseSensitive, boolean regExp,
		Set<TextUtils.WhatToSearch> whatToSearch, CodeExpression.Code codeRestr, TextDescriptor.Unit unitRestr,
		boolean highlightedOnly)
	{
		System.out.println("Text search not implemented for waveform windows");
	}

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public boolean findNextText(boolean reverse) { return false; }

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace) {}

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public void replaceAllText(String replace) {}

	/**
	 * Method to export directly PNG file.
	 * @param ep printable object.
	 * @param filePath
	 */
	public void writeImage(ElectricPrinter ep, String filePath)
	{
		BufferedImage img = getPrintImage(ep);
		PNG.writeImage(img, filePath);
	}

	private int oldBackground, oldForeground;
	private boolean changedColors = false;

	/**
	 * Method to intialize for printing.
	 * @param ep the ElectricPrinter object.
	 * @param pageFormat information about the print job.
	 * @return true if no erros were found during initialization.
	 */
	public boolean initializePrinting(ElectricPrinter ep, PageFormat pageFormat)
	{
		oldForeground = User.getColor(User.ColorPrefType.WAVE_FOREGROUND);
		oldBackground = User.getColor(User.ColorPrefType.WAVE_BACKGROUND);
		User.setColor(User.ColorPrefType.WAVE_FOREGROUND, 0);
		User.setColor(User.ColorPrefType.WAVE_BACKGROUND, 0xFFFFFF);
		changedColors = true;

		PrinterJob pj = ep.getPrintJob();
		if (pj == null) return false; // error
		ColorSupported cs = pj.getPrintService().getAttribute(ColorSupported.class);
		if (cs == null) return false; // error
		nowPrinting = 1;
		if (cs.getValue() == 0) nowPrinting = 2;

		Dimension oldSize = ep.getOldSize();
		int pageWid = (int)pageFormat.getImageableWidth() * ep.getDesiredDPI() / 72;
		int pageHei = (int)pageFormat.getImageableHeight() * ep.getDesiredDPI() / 72;
		double scaleX = (double)pageWid / (double)oldSize.width;
		double scaleY = (double)pageHei / (double)oldSize.height;
		double scale = Math.min(scaleX, scaleY);
		pageWid = (int)(oldSize.width * scale);
		pageHei = (int)(oldSize.height * scale);
		overall.setSize(pageWid, pageHei);
		overall.validate();
		redrawAllPanels();
		overall.repaint();
		return true;
	}

	/**
	 * Method to print window using offscreen canvas.
	 * @param ep printable object.
	 * @return the image to print (null on error).
	 */
	public BufferedImage getPrintImage(ElectricPrinter ep)
	{
		BufferedImage bImage = ep.getBufferedImage();
		Dimension sz = getPanel().getSize();
		if (bImage == null)
		{
			bImage = (BufferedImage)(overall.createImage(sz.width, sz.height));
			ep.setBufferedImage(bImage);
		}

		Graphics2D g2d = (Graphics2D)ep.getGraphics();
		if (g2d == null)
		{
			g2d = bImage.createGraphics();
		}

		// scale if there was an old image size
		Dimension szOld = ep.getOldSize();
		if (szOld != null)
		{
			double scaleX = (double)sz.width / (double)szOld.width;
			double scaleY = (double)sz.height / (double)szOld.height;
			double gSX = (double)szOld.width / (double)szOld.height;
			double gSY = gSX * scaleY / scaleX;
			g2d.translate(ep.getPageFormat().getImageableX(), ep.getPageFormat().getImageableY());
			g2d.scale(72.0 / ep.getDesiredDPI() / gSX, 72.0 / ep.getDesiredDPI() / gSY);
		}

		overall.paint(g2d);
//		if (mainHorizRulerPanel != null)
//			mainHorizRulerPanel.paint(g2d);
		if (changedColors)
		{
			User.setColor(User.ColorPrefType.WAVE_FOREGROUND, oldForeground);
			User.setColor(User.ColorPrefType.WAVE_BACKGROUND, oldBackground);
			changedColors = false;
		}
		nowPrinting = 0;
		return bImage;
	}

	/**
	 * Method to pan along X or Y according to fixed amount of ticks
	 * @param direction 0 for horizontal, 1 for vertical.
	 * @param panningAmounts an array of distances, indexed by the current panning distance index.
	 * @param ticks the number of steps to take (usually 1 or -1).
	 */
	public void panXOrY(int direction, double[] panningAmounts, int ticks)
	{
		// determine the panel extent
		double hRange = maxXPosition - minXPosition;
		double vRange = -1;
		double vRangeAny = -1;
		for(Panel wp : wavePanels)
		{
			vRangeAny = wp.getYAxisRange();
			if (wp.isSelected())
			{
				hRange = wp.getMaxXAxis() - wp.getMinXAxis();
				vRange = wp.getYAxisRange();
				break;
			}
		}
		if (vRange < 0) vRange = vRangeAny;

		double distance = ticks * panningAmounts[User.getPanningDistance()];
		for(Panel wp : wavePanels)
		{
			if (direction == 0)
			{
				// pan horizontally
				if (!xAxisLocked && !wp.isSelected()) continue;
				double low = wp.getMinXAxis() - hRange * distance;
				double high = wp.getMaxXAxis() - hRange * distance;
				wp.setXAxisRange(low, high);
			} else
			{
				// pan vertically
				if (!wp.isSelected()) continue;
				double low = wp.getYAxisLowValue() - vRange * distance;
				double high = wp.getYAxisHighValue() - vRange * distance;
				wp.setYAxisRange(low, high);
			}
			wp.repaintWithRulers();
		}
	}

	/**
	 * Method to shift the panels so that the main cursor location becomes the center.
	 */
	public void centerCursor() {
        double center = getMainXPositionCursor();
        for(Iterator<Panel> it = getPanels(); it.hasNext(); ) {
            Panel wp = it.next();
            double half = (wp.getMaxXAxis() - wp.getMinXAxis())/2.;
            wp.setXAxisRange(center-half, center+half);
            wp.repaintWithRulers();
        }
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		String title = "";
		if (sd.getEngine() != null) title = "Simulation of "; else title = "Waveforms of ";
		wf.setTitle(wf.composeTitle(sd.getCell(), title, 0));
	}

	/**
	 * Method to return the top-level JPanel for this WaveformWindow.
	 * The actual WaveformWindow object is below the top level, surrounded by scroll bars and other display artifacts.
	 * @return the top-level JPanel for this WaveformWindow.
	 */
	public JPanel getPanel() { return overall; }

	public void setCursor(Cursor cursor)
	{
		overall.setCursor(cursor);
		if (USETABLES)
		{
			table.setCursor(cursor);
		} else
		{
			split.setCursor(cursor);
			right.setCursor(cursor);
		}
		for (JPanel p : wavePanels)
		{
			p.setCursor(cursor);
		}
	}

	public void setCell(Cell cell, VarContext context, WindowFrame.DisplayAttributes displayAttributes)
	{
		sd.setCell(cell);
		setWindowTitle();
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return sd.getCell(); }

	/**
	 * Method to return the stimulus information associated with this WaveformWindow.
	 * @return the stimulus information associated with this WaveformWindow.
	 */
	public Stimuli getSimData() { return sd; }

	public void bottomScrollChanged(int e) {}

	public void rightScrollChanged(int e) {}

	// ************************************* WINDOW CONTROL *************************************

	/**
	 * Method to return the associated schematics or layout window for this WaveformWindow.
	 * @return the other window that is cross-linked to this.
	 * Returns null if none can be found.
	 */
	private WindowFrame findSchematicsWindow()
	{
		Cell cell = getCell();
		if (cell == null) return null;

		// look for the original cell to highlight it
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			if (wf.getContent().getCell() != cell) continue;
			if (wf.getContent() instanceof EditWindow) return wf;
		}
		return null;
	}

	/**
	 * Method to return the waveform window associated with a Cell.
	 * There may be multiple such windows, and the most recently used is returned.
	 * @param cell the Cell whose waveform window is desired.
	 * @return the waveform window that is linked to a cell.
	 * Returns null if none can be found.
	 */
	public static WaveformWindow findWaveformWindow(Cell cell)
	{
		// look for the original cell to highlight it
		WaveformWindow found = null;
		int bestClock = 0;
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			if (wf.getContent().getCell() != cell) continue;
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				if (found == null || wf.getUsageClock() > bestClock)
				{
					found = ww;
					bestClock = wf.getUsageClock();
				}
			}
		}
		return found;
	}

	/**
	 * Method to return the WindowFrame in which this WaveformWindow lives.
	 * @return the WindowFrame in which this WaveformWindow lives.
	 */
	public WindowFrame getWindowFrame() { return wf; }

	public int getScreenLowX() { return screenLowX; }

	public int getScreenHighX() { return screenHighX; }

	public void setScreenXSize(int lowX, int highX) { screenLowX = lowX;   screenHighX = highX; }

	public JPanel getSignalNamesPanel() { return left; }

	public JPanel getSignalTracesPanel() { return right; }

	public JTable getWaveformTable() { return table; }

	public static Color getBackgroundColor() { return backgroundColor; }

	// ************************************* CONTROL OF PANELS IN THE WINDOW *************************************

	/**
	 * Method to create a new panel with an X range similar to others on the display.
	 * @return the newly created Panel.
	 */
	public Panel makeNewPanel()
	{
		int panelSize = User.getWaveformDigitalPanelHeight();
		if (sd.isAnalog()) panelSize = User.getWaveformAnalogPanelHeight();

		// determine the X and Y ranges
		Rectangle2D bounds = null;
		double leftEdge, rightEdge;
        //bounds = sd.getBounds();
        leftEdge = sd.getMinTime();
        rightEdge = sd.getMaxTime();

		//double lowValue = bounds.getMinY();
		//double highValue = bounds.getMaxY();
		int vertAxisPos = -1;
		if (xAxisLocked && wavePanels.size() > 0)
		{
			Panel aPanel = wavePanels.get(0);
			leftEdge = aPanel.getMinXAxis();
			rightEdge = aPanel.getMaxXAxis();
			vertAxisPos = aPanel.getVertAxisPos();
		}

		int [] rowHeights = null;
		if (USETABLES)
		{
			int rows = table.getRowCount();
			rowHeights = new int[rows+1];
			for(int i=0; i<rows; i++) rowHeights[i] = table.getRowHeight(i);
			rowHeights[rows] = panelSize;
		}

		// create the new panel
		Panel panel = new Panel(this, sd.isAnalog());

		// set the X and Y ranges
		panel.setXAxisRange(leftEdge, rightEdge);
		if (vertAxisPos > 0) panel.setVertAxisPos(vertAxisPos);

		// show and return the panel
		panel.makeSelectedPanel(-1, -1);
		getPanel().validate();
		if (getMainHorizRuler() != null)
			getMainHorizRuler().repaint();
		if (USETABLES)
		{
			table.tableChanged(new TableModelEvent(tableModel));
			for(int i=0; i<rowHeights.length; i++) table.setRowHeight(i, rowHeights[i]);
		}
		return panel;
	}

	/**
	 * Method to return the number of Panels in this WaveformWindow.
	 * @return the number of Panels in this WaveformWindow.
	 */
	public int getNumPanels() { return wavePanels.size(); }

	/**
	 * Method to return a Panel in this window.
	 * @param index the panel number to get.
	 * @return a Panel in this window.
	 */
	public Panel getPanel(int index) { return wavePanels.get(index); }

	/**
	 * Method to return the index of a Panel in this window.
	 * @param panel the Panel to find.
	 * @return the index of that Panel in this window.
	 */
	public int getPanelIndex(Panel panel) { return wavePanels.indexOf(panel); }

	/**
	 * Method to add a Panel to this window.
	 */
	public void addPanel(Panel panel)
	{
		wavePanels.add(panel);
	}

	/**
	 * Method to add a Panel to this window.
	 */
	public void addPanel(Panel panel, int index)
	{
		wavePanels.add(index, panel);
	}

	/**
	 * Method to remove a Panel from this window.
	 */
	public void removePanel(Panel panel)
	{
		wavePanels.remove(panel);
	}

	/**
	 * Method to return an Iterator over the Panel in this window.
	 * @return an Iterator over the Panel in this window.
	 */
	public Iterator<Panel> getPanels() { return wavePanels.iterator(); }

	/**
	 * Method to return the current printing mode.
	 * @return 0: color display (default), 1: color printing, 2: B&W printing
	 */
	public int getPrintingMode() { return nowPrinting; }

	/**
	 * Method to return a Panel, given its number.
	 * @param panelNumber the number of the desired Panel.
	 * @return the Panel with that number (null if not found).
	 */
	private Panel getPanelFromNumber(int panelNumber)
	{
		for(Panel wp : wavePanels)
		{
			if (wp.getPanelNumber() == panelNumber) return wp;
		}
		return null;
	}

	private void togglePanelName()
	{
		if (rebuildingSignalNameList) return;
		String panelName = (String)signalNameList.getSelectedItem();
		int spacePos = panelName.indexOf(' ');
		if (spacePos >= 0) panelName = panelName.substring(spacePos+1);
		int index = TextUtils.atoi(panelName);

		// toggle its state
		for(Panel wp : wavePanels)
		{
			if (wp.getPanelNumber() == index)
			{
				if (wp.isHidden())
				{
					showPanel(wp);
				} else
				{
					hidePanel(wp);
				}
				break;
			}
		}
	}

	public void validatePanel() { overall.validate(); }

	public void rebuildPanelList()
	{
		rebuildingSignalNameList = true;
		signalNameList.removeAllItems();
		boolean hasSignals = false;
		for(Panel wp : wavePanels)
		{
			signalNameList.addItem("Panel " + Integer.toString(wp.getPanelNumber()) + (wp.isHidden() ? " (HIDDEN)" : ""));
			hasSignals = true;
		}
		if (hasSignals) signalNameList.setSelectedIndex(0);
		rebuildingSignalNameList = false;
	}

	public void redrawAllPanels()
	{
		if (mainHorizRulerPanel != null)
			mainHorizRulerPanel.repaint();
		for(Panel wp : wavePanels)
		{
			wp.repaintContents();
		}
		if (USETABLES)
		{
			table.repaint();
		} else
		{
			left.repaint();
			right.repaint();
		}
	}

	public void repaintAllPanels()
	{
		if (USETABLES)
		{
			table.repaint();
		} else
		{
			right.repaint();
		}
	}

	/**
	 * Method called when a Panel is to be closed.
	 * @param wp the Panel to close.
	 */
	public void closePanel(Panel wp)
	{
		if (USETABLES)
		{
			int rows = wavePanels.size();
			int [] rowHeights = new int[rows];
			int closedPanelIndex = wavePanels.indexOf(wp);
			int validPanels = 0;
			int visRow = 0;
			for(int i=0; i<rows; i++)
			{
				if (wavePanels.get(i).isHidden()) continue;
				int rowHeight = table.getRowHeight(visRow++);
				if (i == closedPanelIndex) continue;
				rowHeights[validPanels++] = rowHeight;
			}

			stopEditing();
			reloadTable();
			wavePanels.remove(wp);

			for(int i=0; i<validPanels; i++) table.setRowHeight(i, rowHeights[i]);
		} else
		{
			left.remove(wp.getLeftHalf());
			right.remove(wp.getRightHalf());
			wavePanels.remove(wp);
		}
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when a Panel is to be hidden.
	 * @param wp the Panel to hide.
	 */
	public void hidePanel(Panel wp)
	{
		if (wp.isHidden()) return;
		if (USETABLES)
		{
			int rows = wavePanels.size();
			int [] rowHeights = new int[rows];
			int hiddenPanelIndex = wavePanels.indexOf(wp);
			int validPanels = 0;
			int visRow = 0;
			for(int i=0; i<rows; i++)
			{
				if (wavePanels.get(i).isHidden()) continue;
				int rowHeight = table.getRowHeight(visRow++);
				if (i == hiddenPanelIndex) continue;
				rowHeights[validPanels++] = rowHeight;
			}

			wp.setHidden(true);
			stopEditing();
			reloadTable();

			for(int i=0; i<validPanels; i++) table.setRowHeight(i, rowHeights[i]);
		} else
		{
			wp.setHidden(true);
			left.remove(wp.getLeftHalf());
			right.remove(wp.getRightHalf());
		}
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called when a Panel is to be shown.
	 * @param wp the Panel to show.
	 */
	public void showPanel(Panel wp)
	{
		if (!wp.isHidden()) return;
		if (USETABLES)
		{
			int rows = wavePanels.size();
			int [] rowHeights = new int[rows];
			int openedPanelIndex = wavePanels.indexOf(wp);
			int validPanels = 0;
			int visRow = 0;
			int openedPanelHeightIndex = -1;
			int averageHeight = 0;
			int numAverages = 0;
			for(int i=0; i<rows; i++)
			{
				if (i == openedPanelIndex)
				{
					openedPanelHeightIndex = validPanels;
					int height = User.getWaveformDigitalPanelHeight();
					if (sd.isAnalog()) height = User.getWaveformAnalogPanelHeight();
					rowHeights[validPanels++] = height;
					continue;
				}
				if (wavePanels.get(i).isHidden()) continue;
				int rowHeight = table.getRowHeight(visRow++);
				rowHeights[validPanels++] = rowHeight;
				averageHeight += rowHeight;
				numAverages++;
			}

			wp.setHidden(false);
			stopEditing();
			reloadTable();

			if (numAverages != 0)
				rowHeights[openedPanelHeightIndex] = averageHeight / numAverages;
			for(int i=0; i<validPanels; i++) table.setRowHeight(i, rowHeights[i]);
		} else
		{
			wp.setHidden(false);
			left.add(wp.getLeftHalf());
			right.add(wp.getRightHalf());
		}
		rebuildPanelList();
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called to grow or shrink the panels vertically.
	 */
	public void growPanels(double scale)
	{
		// if minimum doesn't apply to this display, stop now
		int minHeight;
		if (sd.isAnalog())
		{
			int origPanelSize = User.getWaveformAnalogPanelHeight();
			int newPanelSize = (int)(origPanelSize * scale);
			if (newPanelSize < MINANALOGPANELSIZE) newPanelSize = MINANALOGPANELSIZE;
			if (origPanelSize == newPanelSize) return;
			User.setWaveformAnalogPanelHeight(newPanelSize);
			minHeight = MINANALOGPANELSIZE;
		} else
		{
			int origPanelSize = User.getWaveformDigitalPanelHeight();
			int newPanelSize = (int)(origPanelSize * scale);
			if (newPanelSize < MINDIGITALPANELSIZE) newPanelSize = MINDIGITALPANELSIZE;
			if (origPanelSize == newPanelSize) return;
			User.setWaveformDigitalPanelHeight(newPanelSize);
			minHeight = MINDIGITALPANELSIZE;
		}

		// resize the panels
		if (USETABLES)
		{
			for(int i=0; i<table.getRowCount(); i++)
			{
				int rowHeight = table.getRowHeight(i);
				int newRowHeight = (int)(rowHeight*scale);
				if (newRowHeight < minHeight) newRowHeight = minHeight;
				table.setRowHeight(i, newRowHeight);
			}
		} else
		{
			for(Panel wp : wavePanels)
			{
				Dimension sz = wp.getSize();
				sz.height = (int)(sz.height * scale);
				if (sz.height < minHeight) sz.height = minHeight;
				wp.setSize(sz.width, sz.height);
				wp.setMinimumSize(sz);
				wp.setPreferredSize(sz);

				sz = wp.getLeftHalf().getSize();
				sz.height = (int)(sz.height * scale);
				if (sz.height < minHeight) sz.height = minHeight;
				wp.getLeftHalf().setPreferredSize(sz);
				wp.getLeftHalf().setMinimumSize(sz);
				wp.getLeftHalf().setSize(sz.width, sz.height);

				sz = wp.getRightHalf().getSize();
				sz.height = (int)(sz.height * scale);
				if (sz.height < minHeight) sz.height = minHeight;
				wp.getRightHalf().setPreferredSize(sz);
				wp.getRightHalf().setMinimumSize(sz);
				wp.getRightHalf().setSize(sz.width, sz.height);
			}
		}
		overall.validate();
		redrawAllPanels();
	}

	/**
	 * Method called to delete the highlighted signal from its Panel.
	 * @param wp the Panel with the signal to be deleted.
	 */
	public void deleteSignalFromPanel(Panel wp)
	{
		boolean found = true;
		while (found)
		{
			found = false;
			for(WaveSignal ws : wp.getSignals())
			{
				if (!ws.isHighlighted()) continue;
				wp.removeHighlightedSignal(ws, true);
				wp.removeSignal(ws.getButton());
				found = true;
				break;
			}
		}
		if (wp.getSignalButtons() != null)
		{
			wp.getSignalButtons().validate();
			wp.getSignalButtons().repaint();
		}
		wp.repaintContents();
		saveSignalOrder();
	}

	/**
	 * Method called to delete all signals from a Panel.
	 * @param wp the Panel to clear.
	 */
	public void deleteAllSignalsFromPanel(Panel wp)
	{
		wp.clearHighlightedSignals();
		wp.getSignalButtons().removeAll();
		wp.getSignalButtons().validate();
		wp.getSignalButtons().repaint();
		wp.removeAllSignals();
		wp.repaintContents();
		saveSignalOrder();
	}

	// ************************************* THE HORIZONTAL RULER *************************************

	public HorizRuler getMainHorizRuler() { return mainHorizRulerPanel; }

	public Signal getXAxisSignalAll() { return xAxisSignalAll; }

	public void setXAxisSignalAll(Signal sig) { xAxisSignalAll = sig; }

	private void addMainHorizRulerPanel()
	{
		mainHorizRulerPanel = new HorizRuler(null, this);
		mainHorizRulerPanel.setToolTipText("One X axis ruler applies to all signals when the X axes are locked");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 10;      gbc.gridy = 1;
		gbc.weightx = 1;     gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		overall.add(mainHorizRulerPanel, gbc);
	}

	private void removeMainHorizRulerPanel()
	{
		overall.remove(mainHorizRulerPanel);
		mainHorizRulerPanel = null;
	}

	// ************************************* SWEEP CONTROL *************************************

	private void resetSweeps()
	{
		sweepSignals = new ArrayList<SweepSignal>();
        /*
		for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
		{
			Analysis an = it.next();
			if (!(an instanceof Analysis)) continue;
			Analysis aa = (Analysis)an;
			int maxNum = aa.getNumSweeps();
			if (maxNum <= 1) continue;
			for (int i = 0; i < maxNum; i++)
			{
				Object obj = aa.getSweep(i);
				new SweepSignal(obj, this, an);
			}
		}
        */
	}

	public int addSweep(SweepSignal ss)
	{
		if (sweepSignals.add(ss))
			return sweepSignals.size()-1;
		return -1;
	}

	public void setIncludeInAllSweeps(List<SweepSignal> sweeps, boolean include)
	{
		for(int i=0; i<sweeps.size(); i++)
		{
			SweepSignal ss = sweeps.get(i);
			boolean update = (i == sweeps.size()-1);
			ss.setIncluded(include, update);
		}
	}

	/**
	 * Method to check whether this particular sweep is included.
	 * @return true if the sweep is included
	 */
	public boolean isSweepSignalIncluded(Analysis an, int index)
	{
        /*
		Object sweep = an.getSweep(index);
		for (SweepSignal ss : sweepSignals)
		{
			if (ss.getObject() == sweep)
				return ss.isIncluded();
		}
        */
		return true; // in case no sweep, always true
	}

	public int getHighlightedSweep() { return highlightedSweep; }

	public void setHighlightedSweep(int sweep) { highlightedSweep = sweep; }

	// ************************************* VCR CONTROL *************************************

	private void tick()
	{
		// see if it is time to advance the VCR
		long curtime = System.currentTimeMillis();
		if (curtime - vcrLastAdvance < 100) return;
		vcrLastAdvance = curtime;

		if (wavePanels.size() == 0) return;
		Panel wp = wavePanels.iterator().next();
		int xValueScreen = wp.convertXDataToScreen(mainXPosition);
		//Rectangle2D bounds = sd.getBounds();
		if (vcrPlayingBackwards)
		{
			int newXValueScreen = xValueScreen - vcrAdvanceSpeed;
			double newXValue = wp.convertXScreenToData(newXValueScreen);
			double lowXValue = sd.getMinTime();
			if (newXValue <= lowXValue)
			{
				newXValue = lowXValue;
				vcrClickStop();
			}
			setMainXPositionCursor(newXValue);
		} else
		{
			int newXValueScreen = xValueScreen + vcrAdvanceSpeed;
			double newXValue = wp.convertXScreenToData(newXValueScreen);
			double highXValue = sd.getMaxTime();
			if (newXValue >= highXValue)
			{
				newXValue = highXValue;
				vcrClickStop();
			}
			setMainXPositionCursor(newXValue);
		}
		redrawAllPanels();
	}

	public void vcrClickRewind()
	{
		vcrClickStop();
		double lowXValue = sd.getMinTime();
		setMainXPositionCursor(lowXValue);
		redrawAllPanels();
	}

	public void vcrClickPlayBackwards()
	{
		if (vcrTimer == null)
		{
			ActionListener taskPerformer = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { tick(); }
			};
			vcrTimer = new Timer(100, taskPerformer);
			vcrLastAdvance = System.currentTimeMillis();
			vcrTimer.start();
		}
		vcrPlayingBackwards = true;
	}

	/**
	 * Method to stop the auto-playing in the simulation window.
	 */
	public void vcrClickStop()
	{
		if (vcrTimer == null) return;
		vcrTimer.stop();
		vcrTimer = null;
	}

	public void vcrClickPlay()
	{
		if (vcrTimer == null)
		{
			ActionListener taskPerformer = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { tick(); }
			};
			vcrTimer = new Timer(100, taskPerformer);
			vcrLastAdvance = System.currentTimeMillis();
			vcrTimer.start();
		}
		vcrPlayingBackwards = false;
	}

	public void vcrClickToEnd()
	{
		vcrClickStop();
		double highXValue = sd.getMaxTime();
		setMainXPositionCursor(highXValue);
		redrawAllPanels();
	}

	public void vcrClickFaster()
	{
		int j = vcrAdvanceSpeed / 4;
		if (j <= 0) j = 1;
		vcrAdvanceSpeed += j;
	}

	public void vcrClickSlower()
	{
		int j = vcrAdvanceSpeed / 4;
		if (j <= 0) j = 1;
		vcrAdvanceSpeed -= j;
		if (vcrAdvanceSpeed <= 0) vcrAdvanceSpeed = 1;
	}

	// ************************************* HIGHLIGHTING *************************************

	/**
	 * Method to remove all highlighting from waveform window.
	 */
	public void clearHighlighting()
	{
		// look at all signal names in the cell
		for(Panel wp : wavePanels)
		{
			// look at all traces in this panel
			boolean changed = false;
			for(WaveSignal ws : wp.getSignals())
			{
				if (ws.isHighlighted()) changed = true;
				ws.setHighlighted(false);
			}
			if (changed) wp.repaintContents();
		}
	}

	/**
	 * Method to return a List of highlighted simulation signals.
	 * @return a List of highlighted simulation signals.
	 */
	public List<Signal> getHighlightedNetworkNames()
	{
		List<Signal> highlightedSignals = new ArrayList<Signal>();

		// look at all signal names in the cell
		for(Panel wp : wavePanels)
		{
			// look at all traces in this panel
			for(WaveSignal ws : wp.getSignals())
			{
				if (ws.isHighlighted()) highlightedSignals.add(ws.getSignal());
			}
		}

		// also include what is in the SIGNALS tree
		ExplorerTree sigTree = wf.getExplorerTab();
		Object nodeInfo = sigTree.getCurrentlySelectedObject(0);
		if (nodeInfo != null && nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
			highlightedSignals.add(sig);
		}

		return highlightedSignals;
	}

	/**
	 * Method to get a Set of currently highlighted networks in this WaveformWindow.
	 */
	public Set<Network> getHighlightedNetworks()
	{
		// make empty set
		Set<Network> nets = new HashSet<Network>();

		// if no cell in the window, stop now
		Cell cell = sd.getCell();
		if (cell == null) return nets;
		Netlist netlist = cell.getNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return nets;
		}

		// look at all signal names in the cell
		for(Panel wp : wavePanels)
		{
			// look at all traces in this panel
			for(WaveSignal ws : wp.getSignals())
			{
				Network net = findNetwork(netlist, ws.getSignal().getSignalName());
				if (net != null) nets.add(net);
			}
		}

		// also include what is in the SIGNALS tree
		ExplorerTree sigTree = wf.getExplorerTab();
		Object nodeInfo = sigTree.getCurrentlySelectedObject(0);
		if (nodeInfo != null && nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
			Network net = findNetwork(netlist, sig.getSignalName());
			if (net != null) nets.add(net);
		}
		return nets;
	}

	/**
	 * Get the highlighter for this window content.
	 * @return the highlighter
	 */
	public Highlighter getHighlighter() { return highlighter; }

	// ************************************* PRINTING *************************************

	/**
	 * Method to get a list of polygons describing the waveform window.
	 * @return a List of PolyBase objects that describe this window.
	 */
	public List<PolyBase> getPolysForPrinting()
	{
		int offY = 0;
		List<PolyBase> override = new ArrayList<PolyBase>();
		HorizRuler mainHR = getMainHorizRuler();
		if (mainHR != null)
		{
			List<PolyBase> horizPolys = mainHR.getPolysForPrinting(getPanels().next());
			for(PolyBase poly : horizPolys)
			{
				Point2D [] pts = poly.getPoints();
				for(int i=0; i<pts.length; i++)
				{
					poly.setPoint(i, pts[i].getX(), pts[i].getY() + offY);
				}
				override.add(poly);
			}
			offY += mainHR.getHeight();
		}
		for(Iterator<Panel> it = getPanels(); it.hasNext(); )
		{
			Panel panel = it.next();
			HorizRuler hr = panel.getHorizRuler();
			if (hr != null)
			{
				offY += hr.getHeight();
				List<PolyBase> horizPolys = hr.getPolysForPrinting(panel);
				for(PolyBase poly : horizPolys)
				{
					Point2D [] pts = poly.getPoints();
					for(int i=0; i<pts.length; i++)
					{
						poly.setPoint(i, pts[i].getX(), pts[i].getY() + offY);
					}
					override.add(poly);
				}
				offY += hr.getHeight();
			}
			List<PolyBase> panelList = panel.getPolysForPrinting();
			for(PolyBase poly : panelList)
			{
				Point2D [] pts = poly.getPoints();
				for(int i=0; i<pts.length; i++)
				{
					poly.setPoint(i, pts[i].getX(), pts[i].getY() + offY);
				}
				override.add(poly);
			}
			offY += panel.getHeight();
			if (hr == null) offY += 20;
		}
		return override;
	}

	// ************************************* THE EXPLORER TREE *************************************

	public List<MutableTreeNode> loadExplorerTrees()
	{
		TreePath rootPath = new TreePath(ExplorerTreeModel.rootNode);
		ArrayList<MutableTreeNode> nodes = new ArrayList<MutableTreeNode>();

		treePathFromSignal.clear();
		for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
		{
			Analysis an = it.next();
			if (an.getAnalysisType() == Analysis.ANALYSIS_SIGNALS)
			{
				nodes.add(getSignalsForExplorer(an, rootPath, "SIGNALS"));
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_TRANS)
			{
				nodes.add(getSignalsForExplorer(an, rootPath, "TRANS SIGNALS"));
				nodes.add(getSweepsForExplorer(an, "TRANS SWEEPS"));
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_AC)
			{
				nodes.add(getSignalsForExplorer(an, rootPath, "AC SIGNALS"));
				nodes.add(getSweepsForExplorer(an, "AC SWEEPS"));
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_DC)
			{
				nodes.add(getSignalsForExplorer(an, rootPath, "DC SIGNALS"));
				nodes.add(getSweepsForExplorer(an, "DC SWEEPS"));
			} else if (an.getAnalysisType() == Analysis.ANALYSIS_MEAS)
			{
				nodes.add(getSignalsForExplorer(an, rootPath, "MEASUREMENTS"));
			}
		}

		// clean possible nulls
		while (nodes.remove(null));

		return nodes;
	}

    public void loadTechnologies() {
    }
    
	private DefaultMutableTreeNode getSignalsForExplorer(Analysis an, TreePath parentPath, String analysis)
	{
		List<Signal> signals = an.getSignals();
		if (signals.size() == 0) return null;
        if (an instanceof EpicAnalysis)
        {
			DefaultMutableTreeNode analysisNode = ((EpicAnalysis)an).getSignalsForExplorer(analysis);
            TreePath path = parentPath.pathByAddingChild(analysisNode);
            for (Signal s : (List<Signal>)an.getSignals())
                treePathFromSignal.put(s, path);
			return analysisNode;
        }
		DefaultMutableTreeNode signalsExplorerTree = new DefaultMutableTreeNode(analysis);
		TreePath analysisPath = parentPath.pathByAddingChild(signalsExplorerTree);
        for (Signal s : (List<Signal>)an.getSignals())
            treePathFromSignal.put(s, analysisPath);
		Map<String,TreePath> contextMap = new HashMap<String,TreePath>();
		contextMap.put("", analysisPath);
		Collections.sort(signals, new SignalsByName());

		// add branches first
		char separatorChar = sd.getSeparatorChar();
		for(Signal sSig : signals)
		{
//			if (!(sSig instanceof TimedSignal)) continue;
			if (sSig.getSignalContext() != null)
				makeContext(sSig.getSignalContext(), contextMap, separatorChar);
		}

        String delim = SimulationTool.getSpiceExtractedNetDelimiter();
        // make a list of signal names with "#" in them
		Set<String> sharpSet = new HashSet<String>();
		for(Signal sSig : signals)
		{
//			if (!(sSig instanceof TimedSignal)) continue;
			String sigName = sSig.getSignalName();
			int hashPos = sigName.indexOf(delim);
			if (hashPos > 0)
			{
				String nodeName = sSig.getSignalContext();
				if (nodeName == null) nodeName = ""; else nodeName += separatorChar;
				nodeName += sigName.substring(0, hashPos);
				sharpSet.add(nodeName);
			}
		}

		// add all signals to the tree
		for(Signal sSig : signals)
		{
//			if (!(sSig instanceof TimedSignal)) continue;
			TreePath thisTree = analysisPath;

			String nodeName = sSig.getSignalContext();
			String nodeNameStr = nodeName;
			if (nodeNameStr == null) nodeNameStr = ""; else nodeNameStr += separatorChar;
			String sigName = sSig.getSignalName();
			int hashPos = sigName.indexOf(delim);
			if (hashPos > 0)
			{
				// force a branch with the proper name
				nodeName = nodeNameStr + sigName.substring(0, hashPos+1);
			} else
			{
				// if this is the pure name of a hash set, force a branch
				String pureSharpName = nodeNameStr + sigName;
				if (sharpSet.contains(pureSharpName))
					nodeName = pureSharpName + delim;
			}
			if (nodeName != null)
				thisTree = makeContext(nodeName, contextMap, separatorChar);

			DefaultMutableTreeNode sigLeaf = new DefaultMutableTreeNode(sSig);
			((DefaultMutableTreeNode)thisTree.getLastPathComponent()).add(sigLeaf);
		}
		return signalsExplorerTree;
	}

	/**
	 * Recursive method to locate and create branches in the Signal Explorer tree.
	 * @param branchName the name of a branch to find/create.
	 * The name has dots in it to separate levels of the hierarchy.
	 * @param contextMap a HashMap of branch names to tree paths.
	 * @return the tree path for the requested branch name.
	 */
	private TreePath makeContext(String branchName, Map<String,TreePath> contextMap, char separatorChar)
	{
		TreePath branchTree = contextMap.get(branchName);
		if (branchTree != null) return branchTree;

		// split the branch name into a leaf and parent
		String parent = "";
		String leaf = branchName;
		int dotPos = leaf.lastIndexOf(separatorChar);
		if (dotPos >= 0)
		{
			parent = leaf.substring(0, dotPos);
			leaf = leaf.substring(dotPos+1);
		}

		TreePath parentBranch = makeContext(parent, contextMap, separatorChar);
		DefaultMutableTreeNode thisTree = new DefaultMutableTreeNode(leaf);
		((DefaultMutableTreeNode)parentBranch.getLastPathComponent()).add(thisTree);
		TreePath thisPath = parentBranch.pathByAddingChild(thisTree);
		contextMap.put(branchName, thisPath);
		return thisPath;
	}

	/**
	 * Class to sort signals by their name
	 */
	private static class SignalsByName implements Comparator<Signal>
	{
		public int compare(Signal s1, Signal s2)
		{
			return TextUtils.STRING_NUMBER_ORDER.compare(s1.getFullName(), s2.getFullName());
		}
	}

	private DefaultMutableTreeNode getSweepsForExplorer(Analysis an, String analysis)
	{
		DefaultMutableTreeNode sweepsExplorerTree = null;
		boolean first = true;
		for(SweepSignal ss : sweepSignals)
		{
			if (first)
			{
				first = false;
				sweepsExplorerTree = new DefaultMutableTreeNode(analysis);
			}
			sweepsExplorerTree.add(new DefaultMutableTreeNode(ss));
		}
		return sweepsExplorerTree;
	}

	// ************************************* SIGNALS *************************************

	private Signal findSignal(String name, Analysis<Signal> an)
	{
		for(Signal sSig : an.getSignals())
		{
			String sigName = sSig.getFullName();
			if (sigName.equals(name)) return sSig;
		}
		return null;
	}

	/**
	 * Method to add a selection to the waveform display.
	 * @param h a Highlighter of what is selected.
	 * @param context the context of these networks
	 * (a string to prepend to them to get the actual simulation signal name).
	 * @param newPanel true to create new panels for each signal.
	 */
	public void showSignals(Highlighter h, VarContext context, boolean newPanel)
	{
		List<Signal> found = findSelectedSignals(h, context);
		showSignals(found, newPanel);
	}

	/**
	 * Method to add a list of signals to the waveform display.
	 * @param found the signals to add.
	 * @param newPanel true to create new panels for each signal.
	 */
	public void showSignals(List<Signal> found, boolean newPanel)
	{
		// determine the current panel
		Panel wp = null;
		for(Panel oWp : wavePanels)
		{
			if (oWp.isSelected())
			{
				wp = oWp;
				break;
			}
		}
		if (!sd.isAnalog()) newPanel = true;
		if (!newPanel && wp == null)
		{
			System.out.println("No current waveform panel to add signals");
			return;
		}

		boolean added = false;
		for(Signal sSig : found) {
			// add the signal
			if (newPanel) {
				wp = makeNewPanel();
                wp.fitToSignal(sSig);
                newPanel = false;
				if (!xAxisLocked)
					wp.setXAxisRange(sSig.getMinTime(), sSig.getMaxTime());
			} else {
				// make sure the analysis type is correct
				if (wp.wrongPanelType(sSig)) break;
			}

			// check if signal already in panel
			boolean alreadyPlotted = false;
			for(WaveSignal ws : wp.getSignals())
			{
				String name = ws.getSignal().getFullName();
				if (name.equals(sSig.getFullName())) {
					alreadyPlotted = true;
					// add it again, this will increment colors
					WaveSignal.addSignalToPanel(ws.getSignal(), wp, null);
				}
			}
			if (!alreadyPlotted) {
				new WaveSignal(wp, sSig);
			}
			added = true;
			wp.repaintContents();
		}
		if (added)
		{
			overall.validate();
			saveSignalOrder();
		}
	}

	/**
	 * Method to remove a set of Networks from the waveform display.
	 * @param nets the Set of Networks to remove.
	 * @param context the context of these networks
	 * (a string to prepend to them to get the actual simulation signal name).
	 */
	public void removeSignals(Set<Network> nets, VarContext context)
	{
		for(Network net : nets)
		{
			String netName = getSpiceNetName(context, net);
			for(Iterator<Analysis> aIt = sd.getAnalyses(); aIt.hasNext(); )
			{
				Analysis an = aIt.next();
				Signal sSig = an.findSignalForNetwork(netName);
				if (sSig == null) continue;

				boolean found = true;
				while (found)
				{
					found = false;
					for(Iterator<Panel> pIt = getPanels(); pIt.hasNext(); )
					{
						Panel wp = pIt.next();
						//if (wp.getAnalysisType() != an.getAnalysisType()) continue;
						for(WaveSignal ws : wp.getSignals())
						{
							if (ws.getSignal() != sSig) continue;
							wp.removeHighlightedSignal(ws, true);
							wp.removeSignal(ws.getButton());
							wp.getSignalButtons().validate();
							wp.getSignalButtons().repaint();
							wp.repaintContents();
							found = true;
							break;
						}
						if (found) break;
					}
				}
			}
		}
	}

	public static String getSpiceNetName(Network net)
	{
		return Spice.getSafeNetName(net.getName(), SimulationTool.getSpiceEngine());
	}

	/**
	 * Get the spice net name associated with the network and the context.
	 * If the network is null, a String describing only the context is returned.
	 * @param context the context
	 * @param net the network, or null
	 * @return a String describing the unique, global spice name for the network,
	 * or a String describing the context if net is null
	 */
	public static String getSpiceNetName(VarContext context, Network net)
	{
		return getSpiceNetName(context, net, false, false);
	}

	/**
	 * Get the spice net name associated with the network and the context.
	 * If the network is null, a String describing only the context is returned.
	 * @param context the context
	 * @param net the network, or null
	 * @param assuraRCXFormat return net assuming Assura RCX flat netlist format
	 * @param starRCXTFormat return net assuming Star RCXT flat netlist format
	 * @return a String describing the unique, global spice name for the network,
	 * or a String describing the context if net is null
	 */
	public static String getSpiceNetName(VarContext context, Network net, boolean assuraRCXFormat, boolean starRCXTFormat)
	{
		boolean isGlobal = false;

		if (net != null)
		{
			Netlist netlist = net.getNetlist();
			while (net.isExported() && (context != VarContext.globalContext))
			{
				// net is exported, find net in parent
				Network tempnet = getNetworkInParent(net, context.getNodable());
				if (tempnet == null)
					break;
				net = tempnet;
				context = context.pop();
			}

			// searching in globals
			// Code taken from NCC
			netlist = net.getNetlist();
			Global.Set globNets = netlist.getGlobals();
			for (int i=0; i<globNets.size(); i++)
			{
				Global g = globNets.get(i);
				Network netG = netlist.getNetwork(g);
				if (netG == net)
				{
					isGlobal = true;
					break;
				}
			}
		}

		// create net name
		String contextStr = context.getInstPath(".");
		if (assuraRCXFormat) {
			contextStr = "x"+context.getInstPath("/x");
		}
        if (starRCXTFormat) {
            contextStr = context.getInstPath("/");
        }
        contextStr = TextUtils.canonicString(contextStr);
		if (net == null) return contextStr;
		if (context == VarContext.globalContext || isGlobal)
			return getSpiceNetName(net);
        else if (assuraRCXFormat) return contextStr + "/" + getSpiceNetName(net);
        else if (starRCXTFormat) return contextStr + "/" + getSpiceNetName(net);
        else return contextStr + "." + getSpiceNetName(net);
	}

	/**
	 * Get the Network in the childNodable's parent that corresponds to the Network
	 * inside the childNodable.
	 * @param childNetwork the network in the childNodable
	 * @return the network in the parent that connects to the
	 * specified network, or null if no such network.
	 * null on error.
	 */
	public static Network getNetworkInParent(Network childNetwork, Nodable childNodable) {
		if (childNodable == null || childNetwork == null) return null;
		if (!childNodable.isCellInstance()) return null;
		Cell childCell = (Cell)childNodable.getProto();
		if (childCell.contentsView() != null)
			childCell = childCell.contentsView();

		// find export on network
		boolean found = false;
		Export export = null;
		int i = 0;
		for (Iterator<PortProto> it = childCell.getPorts(); it.hasNext(); )
		{
			export = (Export)it.next();
			for (i=0; i<export.getNameKey().busWidth(); i++) {
				Netlist netlist = childCell.getNetlist();
				if (netlist == null)
				{
					System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
					return null;
				}
				Network net = netlist.getNetwork(export, i);
				if (net == childNetwork) { found = true; break; }
			}
			if (found) break;
		}
		if (!found) return null;

		// find corresponding port on icon
		Export pp = (Export)childNodable.getProto().findPortProto(export.getNameKey());

		// find corresponding network in parent
		Cell parentCell = childNodable.getParent();
		//if (childNodable instanceof NodeInst) childNodable = Netlist.getNodableFor((NodeInst)childNodable, 0);
		Netlist netlist = parentCell.getNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return null;
		}
		Network parentNet = netlist.getNetwork(childNodable, pp, i);
		return parentNet;
	}

	/**
	 * Method to locate a simulation signal in the waveform.
	 * @param sSig the Signal to locate.
	 * @return the displayed WaveSignal where it is in the waveform window.
	 * Returns null if the signal is not being displayed.
	 */
	public WaveSignal findDisplayedSignal(Signal sSig)
	{
		for(Panel wp : wavePanels)
		{
			WaveSignal ws = wp.findWaveSignal(sSig);
			if (ws != null) return ws;
		}
		return null;
	}

	// ************************************* THE X AXIS *************************************

	public double getMainXPositionCursor() { return mainXPosition; }

	public void setMainXPositionCursor(double value)
	{
		mainXPosition = value;
		String amount = TextUtils.convertToEngineeringNotation(mainXPosition, "s");
		mainPos.setText("Main: " + amount);
		String diff = TextUtils.convertToEngineeringNotation(Math.abs(mainXPosition - extXPosition), "s");
		delta.setText("Delta: " + diff);
		updateAssociatedLayoutWindow();
	}

	public double getExtensionXPositionCursor() { return extXPosition; }

	public void setExtensionXPositionCursor(double value)
	{
		extXPosition = value;
		String amount = TextUtils.convertToEngineeringNotation(extXPosition, "s");
		extPos.setText("Ext: " + amount);
		String diff = TextUtils.convertToEngineeringNotation(Math.abs(mainXPosition - extXPosition), "s");
		delta.setText("Delta: " + diff);
	}

	/**
	 * Method to set the X range in all panels.
	 * @param minXPosition the low X value.
	 * @param maxXPosition the high X value.
	 */
	public void setDefaultHorizontalRange(double minXPosition, double maxXPosition)
	{
		this.minXPosition = minXPosition;
		this.maxXPosition = maxXPosition;
	}

	public double getLowDefaultHorizontalRange() { return minXPosition; }

	public double getHighDefaultHorizontalRange() { return maxXPosition; }

	/**
	 * Method to set the zoom extents for this waveform window.
	 * @param lowVert the low value of the vertical axis (for the given panel only).
	 * @param highVert the high value of the vertical axis (for the given panel only).
	 * @param lowHoriz the low value of the horizontal axis (for the given panel only unless X axes are locked).
	 * @param highHoriz the high value of the horizontal axis (for the given panel only unless X axes are locked).
	 * @param thePanel the panel being zoomed.
	 */
	public void setZoomExtents(double lowVert, double highVert, double lowHoriz, double highHoriz, Panel thePanel)
	{
		for(Panel wp : wavePanels)
		{
			boolean changed = false;
			if (wp == thePanel)
			{
				wp.setYAxisRange(lowVert, highVert);
				changed = true;
			}
			if (xAxisLocked || wp == thePanel)
			{
				if (wp.getMinXAxis() < wp.getMaxXAxis())
				{
					wp.setXAxisRange(Math.min(lowHoriz, highHoriz), Math.max(lowHoriz, highHoriz));
				} else
				{
					wp.setXAxisRange(Math.max(lowHoriz, highHoriz), Math.min(lowHoriz, highHoriz));
				}
				changed = true;
			}
			if (changed) wp.repaintWithRulers();
		}
	}

	/**
	 * Method called to toggle the lock on the horizontal axes.
	 */
	public void togglePanelXAxisLock()
	{
		xAxisLocked = ! xAxisLocked;
		if (xAxisLocked)
		{
			// X axes now locked: add main ruler, remove individual rulers
			xAxisLockButton.setIcon(iconLockXAxes);
			addMainHorizRulerPanel();
			double minXPosition = 0, maxXPosition = 0;
			int vertAxis = 0;
			boolean first = true;
			for(Panel wp : wavePanels)
			{
				wp.removeHorizRulerPanel();
				if (first)
				{
					first = false;
					minXPosition = wp.getMinXAxis();
					maxXPosition = wp.getMaxXAxis();
					vertAxis = wp.getVertAxisPos();
				} else
				{
					if (minXPosition < maxXPosition)
					{
						minXPosition = Math.min(minXPosition, wp.getMinXAxis());
						maxXPosition = Math.max(maxXPosition, wp.getMaxXAxis());
					} else
					{
						minXPosition = Math.max(minXPosition, wp.getMinXAxis());
						maxXPosition = Math.min(maxXPosition, wp.getMaxXAxis());
					}
					wp.setVertAxisPos(vertAxis);
				}
			}

			// force all panels to be at the same X position
			for(Panel wp : wavePanels)
			{
				wp.setXAxisRange(minXPosition, maxXPosition);
			}
		} else
		{
			// X axes are unlocked: put a ruler in each panel, remove main ruler
			xAxisLockButton.setIcon(iconUnLockXAxes);
			for(Panel wp : wavePanels)
			{
				wp.addHorizRulerPanel();
			}
			removeMainHorizRulerPanel();
		}
		overall.validate();
		overall.repaint();
	}

	public boolean isXAxisLocked() { return xAxisLocked; }

	// ************************************* CROSS-PROBING *************************************

	/**
	 * Method to crossprobe from an EditWindow to this WaveformWindow.
	 * @param wnd the EditWindow that changed.
	 * @param which the Highlighter in that window with current selection.
	 */
	private void crossProbeEditWindowToWaveform(EditWindow wnd, Highlighter which)
	{
		// make sure the windows are associated with each other
		Locator loc = new Locator(wnd, this);
		if (loc.getWaveformWindow() != this) return;

		freezeEditWindowHighlighting = true;

		// start by removing all highlighting in the waveform
		for(Panel wp : wavePanels)
		{
			wp.clearHighlightedSignals();
		}

		// also clear "Signals" tree highlighting
		ExplorerTree tree = wf.getExplorerTab();
		tree.setSelectionPath(null);
		tree.clearCurrentlySelectedObjects();

		// find the signal to show in the waveform window
		List<Signal> found = findSelectedSignals(which, loc.getContext());

		// show it in every panel
		boolean foundSignal = false;
		for(Panel wp : wavePanels)
		{
			for(WaveSignal ws : wp.getSignals())
			{
				for(Signal sSig : found)
				{
					if (ws.getSignal() == sSig)
					{
						wp.addHighlightedSignal(ws, false);
						foundSignal = true;
					}
				}
			}
		}
		if (foundSignal) repaint();

		// show only one in the "Signals" tree
		Collections.sort(found, new SignalsByName());
		for(Signal sSig : found)
		{
			TreePath treePath = treePathFromSignal(sSig);
			if (treePath != null) {
				tree.setSelectionPath(treePath);
				break;
			}
		}
		freezeEditWindowHighlighting = false;
	}

	private TreePath treePathFromSignal(Signal sig) {
		TreePath treePath = treePathFromSignal.get(sig);
		if (treePath == null) return null;
		String fullName = sig.getFullName();
		char separator = sd.getSeparatorChar();
		int sBeg = 0;
		while (sBeg < fullName.length()) {
			int sEnd = fullName.indexOf(separator, sBeg);
			if (sEnd < 0) sEnd = fullName.length();
			String s = fullName.substring(sBeg, sEnd);
			TreeNode parentNode = (TreeNode)treePath.getLastPathComponent();
			TreeNode child = findChild(parentNode, s);
			if (child == null) return null;
			treePath = treePath.pathByAddingChild(child);
			sBeg = sEnd + 1;
		}
		return sBeg == fullName.length() + 1 ? treePath : null;
	}

	private static TreeNode findChild(TreeNode parent, String name) {
		for (int i = 0, numChilds = parent.getChildCount(); i < numChilds; i++) {
			TreeNode child = parent.getChildAt(i);
			String s;
			if (child instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)child;
				Object o = node.getUserObject();
				if (o instanceof Signal)
					s = ((Signal)o).getSignalName();
				else
					s = o.toString();
			} else {
				s = child.toString();
			}
			if (name.equals(s)) return child;
		}
		return null;
	}

	/**
	 * Method to return a list of signals that are selected in an EditWindow.
	 * @param h a Highlighter with a selection in an EditWindow.
	 * @param context the VarContext of that window.
	 * @return a List of Signal objects in this WaveformWindow.
	 */
	private List<Signal> findSelectedSignals(Highlighter h, VarContext context)
	{
		List<Signal> found = new ArrayList<Signal>();

		// special case if a current source is selected
		List<Geometric> highlightedObjects = h.getHighlightedEObjs(true, true);
		if (highlightedObjects.size() == 1)
		{
			// if a node is highlighted that has current measured on it, use that
			Geometric geom = highlightedObjects.get(0);
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				String nodeName = "I(v" + ni.getName();
				for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
				{
					Analysis an = it.next();
					Signal sSig = an.findSignalForNetworkQuickly(nodeName);
					if (sSig != null)
					{
						found.add(sSig);
						return found;
					}
				}
			}
		}

		// convert all networks to signals
		Set<Network> nets = h.getHighlightedNetworks();
		found.addAll(findSelectedSignals(nets, context, false));
		Collections.sort(found, new CompSignals());
		return found;
	}

	private List<Signal> findSelectedSignals(Set<Network> nets, VarContext context, boolean sort)
	{
		Cell topContext = sd.getCell();
		List<Signal> found = new ArrayList<Signal>();
		for(Network net : nets)
		{
			String netName = getSpiceNetName(context, net);
			for(Iterator<Analysis> aIt = sd.getAnalyses(); aIt.hasNext(); )
			{
				Analysis an = aIt.next();
				Signal sSig = an.findSignalForNetworkQuickly(netName);
				if (sSig == null)
				{
					// try prepending the top-level cell name to the signal name
					if (topContext == null) topContext = net.getParent();
					String nameWithCell = topContext.getName() + "." + netName;
					sSig = an.findSignalForNetworkQuickly(nameWithCell);
				}
				if (sSig == null)
				{
					// when cross-probing extracted layout, hierarchy delimiter is '/x' instead of '.'
					String temp = getSpiceNetName(context, net, true, false);
					sSig = an.findSignalForNetworkQuickly(temp);
                }
                if (sSig == null)
                {
                    // when cross-probing extracted layout, hierarchy delimiter is '/' instead of '.'
                    String temp = getSpiceNetName(context, net, false, true);
                    sSig = an.findSignalForNetworkQuickly(temp);
                }
                if (sSig == null)
                {
                    // try prepending the top-level cell name and setting the hierarchy delimiter as '/' instead of '.'
                    if (topContext == null) topContext = net.getParent();
                    String temp = getSpiceNetName(context, net, false, true);
                    String nameWithCell = topContext.getName() + "." + temp;
                    sSig = an.findSignalForNetworkQuickly(nameWithCell);
                }

                if (sSig == null)
				{
					// check for equivalent layout net name
					// search up hierarchy for cell with NCC equiv info
					Cell cell = net.getParent();
					NccResult result = NccCrossProbing.getResults(cell);
					if (result == null)
					{
						for (VarContext checkContext = context; checkContext != VarContext.globalContext; checkContext = checkContext.pop())
						{
							cell = checkContext.getNodable().getParent();
							result = NccCrossProbing.getResults(cell);
							if (result != null) break;
						}
					}
					if (result != null)
					{
						HierarchyEnumerator.NetNameProxy proxy = result.getEquivalence().findEquivalentNet(context, net);
						if (proxy != null)
						{
							String otherName = getSpiceNetName(proxy.getContext(), proxy.getNet());
							System.out.println("Mapped "+netName+" to "+otherName);
							sSig = an.findSignalForNetworkQuickly(otherName);
						}
					}
				}
				if (sSig != null)
                {
                    List<Signal> signalGroup = an.getSignalsFromExtractedNet(sSig);
                    for (Signal s : signalGroup)
                        found.add(s);
                }
//					else System.out.println("Can't find net "+netName+" in cell "+context.getInstPath("."));
			}
		}

		if (sort) Collections.sort(found, new CompSignals());
		return found;
	}

	/** Test signal lookup */
	public List<Signal> findAllSignals(Cell cell, VarContext context, boolean sort, boolean recurse)
	{
		Set<Network> nets = new HashSet<Network>();
		List<Signal> found = new ArrayList<Signal>();
		for (Iterator<Network> it = cell.getNetlist().getNetworks(); it.hasNext(); )
		{
			nets.add(it.next());
		}
		found.addAll(findSelectedSignals(nets, context, false));

		if (recurse)
		{
			for (Iterator<Nodable> it = cell.getNetlist().getNodables(); it.hasNext(); )
			{
				Nodable no = it.next();
				if (!(no.getProto() instanceof Cell)) continue;
				Cell subCell = (Cell)no.getProto();
				VarContext subContext = context.push(no);
				found.addAll(findAllSignals(subCell, subContext, false, true));
			}
		}
		if (sort) Collections.sort(found, new CompSignals());
		return found;
	}

	private static class CompSignals implements Comparator<Signal>
	{
		public int compare(Signal s1, Signal s2)
		{
			return TextUtils.STRING_NUMBER_ORDER.compare(s1.getFullName(), s2.getFullName());
		}
	}

	private static Network findNetwork(Netlist netlist, String name)
	{
		// Should really use extended code, found in "simspicerun.cpp:sim_spice_signalname()"
		for(Iterator<Network> nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = nIt.next();
			if (getSpiceNetName(net).equalsIgnoreCase(name)) return net;
		}

		// try converting "@" in network names
		for(Iterator<Network> nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = nIt.next();
			String convertedName = getSpiceNetName(net).replace('@', '_');
			if (convertedName.equalsIgnoreCase(name)) return net;
		}

		// try ignoring "_" in signal names
		for(Iterator<Network> nIt = netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = nIt.next();
			String netName = getSpiceNetName(net);
			if (netName.length() != name.length()) continue;
			boolean matches = true;
			for(int i=0; i<netName.length(); i++)
			{
				char netChar = netName.charAt(i);
				char nameChar = name.charAt(i);
				if (nameChar == '_')
				{
					if (TextUtils.isLetterOrDigit(netChar)) { matches = false;   break; }
				} else
				{
					if (TextUtils.canonicChar(nameChar) != TextUtils.canonicChar(netChar)) { matches = false;   break; }
				}
			}
			if (matches) return net;
		}
		return null;
	}

	/**
	 * Method called when signal waveforms change, and equivalent should be shown in the edit window.
	 */
	public void crossProbeWaveformToEditWindow()
	{
		// check double-crossprobe locks
		if (freezeEditWindowHighlighting) return;
		freezeWaveformHighlighting = true;

		// highlight the net in any associated edit windows
		for(Iterator<WindowFrame> wIt = WindowFrame.getWindows(); wIt.hasNext(); )
		{
			WindowFrame wfr = wIt.next();
			if (!(wfr.getContent() instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)wfr.getContent();
			Locator loc = new Locator(wnd, this);
			if (loc.getWaveformWindow() != this) continue;
			VarContext context = loc.getContext();

			Cell cell = wnd.getCell();
			if (cell == null) continue;
			Highlighter hl = wnd.getHighlighter();
			Netlist netlist = cell.getNetlist();
			if (netlist == null)
			{
				System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
				freezeWaveformHighlighting = false;
				return;
			}

			hl.clear();
			for(Panel wp : wavePanels)
			{
				for(WaveSignal ws : wp.getSignals())
				{
					if (!ws.isHighlighted()) continue;
					String want = ws.getSignal().getFullName();
					Stack<Nodable> upNodables = new Stack<Nodable>();
					Network net = null;
					Cell subCell = cell;
					VarContext subContext = context;
					for (;;)
					{
						String contextStr = getSpiceNetName(subContext, null);
						if (contextStr.length() > 0)
						{
							boolean matches = false;
							contextStr += ".";
							String altContextStr = contextStr;
							if (sd.getCell() != null)
								altContextStr = sd.getCell().getName().toLowerCase() + "." + contextStr;
							if (want.startsWith(contextStr)) matches = true; else
							{
								contextStr = contextStr.replace('@', '_');
								if (want.startsWith(contextStr)) matches = true;
							}
							if (!matches)
							{
								if (want.startsWith(altContextStr)) matches = true; else
								{
									altContextStr = altContextStr.replace('@', '_');
									if (want.startsWith(altContextStr)) matches = true;
								}
								if (matches) contextStr = altContextStr;
							}
							if (!matches)
							{
								if (subContext == VarContext.globalContext) break;
								subCell = subContext.getNodable().getParent();
								upNodables.push(subContext.getNodable());
								subContext = subContext.pop();
								continue;
							}
						}
						String desired = want.substring(contextStr.length());
						net = findNetwork(netlist, desired);
						if (net != null)
						{
							// found network
							while (!upNodables.isEmpty())
							{
								Nodable no = upNodables.pop();
								net = HierarchyEnumerator.getNetworkInChild(net, no);
								if (net == null) break;
							}
							if (net != null)
								hl.addNetwork(net, subCell);
							break;
						}

						// see if this name is really a current source
						if (desired.startsWith("I(v"))
						{
							NodeInst ni = subCell.findNode(desired.substring(3));
							if (ni != null)
								hl.addElectricObject(ni, subCell);
						}

						if (subContext == VarContext.globalContext) break;

						subCell = subContext.getNodable().getParent();
						upNodables.push(subContext.getNodable());
						subContext = subContext.pop();
					}
				}
			}

			// also highlight anything selected in the "SIGNALS" tree
			String contextStr = context.getInstPath(".");
			Cell topContext = sd.getCell();
			String altContextStr = contextStr;
			if (topContext != null)
			{
				altContextStr = topContext.getName().toLowerCase();
				if (contextStr.length() > 0) altContextStr += "." + contextStr;
			}
			ExplorerTree sigTree = wf.getExplorerTab();
			Object nodeInfo = sigTree.getCurrentlySelectedObject(0);
			if (nodeInfo != null && nodeInfo instanceof Signal)
			{
				Signal sig = (Signal)nodeInfo;
				if (sig.getSignalContext() == null || sig.getSignalContext().equals(contextStr) ||
					sig.getSignalContext().equals(altContextStr))
				{
					String desired = sig.getSignalName();
					Network net = findNetwork(netlist, desired);
					if (net != null)
					{
						if (net.getParent() == cell)
							hl.addNetwork(net, cell);
					} else
					{
						// see if this name is really a current source
						if (desired.startsWith("I(v"))
						{
							NodeInst ni = cell.findNode(desired.substring(3));
							if (ni != null)
								hl.addElectricObject(ni, cell);
						}
					}
				}
			}

			hl.finished();
		}
		freezeWaveformHighlighting = false;
	}

	private Map<Network,Integer> netValues;

	/**
	 * Method to update associated layout windows when the main cursor changes.
	 */
	private void updateAssociatedLayoutWindow()
	{
		// make sure there is a layout/schematic window being simulated
		WindowFrame oWf = findSchematicsWindow();
		if (oWf == null) return;
		EditWindow schemWnd = (EditWindow)oWf.getContent();

		boolean crossProbeChanged = schemWnd.hasCrossProbeData();
		schemWnd.clearCrossProbeLevels();

		Cell cell = getCell();
		Netlist netlist = cell.getNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted crossprobing (network information unavailable).  Please try again");
			return;
		}

		// reset all values on networks
		netValues = new HashMap<Network,Integer>();

		// assign values from simulation window traces to networks
		for(Panel wp : wavePanels)
		{
			if (wp.isHidden()) continue;
			for(WaveSignal ws : wp.getSignals())
			{
				Signal<DigitalSample> ds = (Signal<DigitalSample>)ws.getSignal();
				List<Signal<DigitalSample>> bussedSignals = DigitalAnalysis.getBussedSignals(ds);
				if (bussedSignals != null)
				{
					// a digital bus trace
					for(Signal subSig : bussedSignals)
					{
						Signal<DigitalSample> subDS = (Signal<DigitalSample>)subSig;
						putValueOnTrace(subDS, cell, netValues, netlist);
					}
				} else
				{
					// single signal
					putValueOnTrace(ds, cell, netValues, netlist);
				}
			}
		}

		// light up any simulation-probe objects
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() != Generic.tech().simProbeNode) continue;
			Network net = null;
			for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = cIt.next();
				net = netlist.getNetwork(con.getArc(), 0);
				break;
			}

			if (net == null) continue;
			Integer state = netValues.get(net);
			if (state == null) continue;
			Color col = getHighlightColor(state.intValue());
			schemWnd.addCrossProbeBox(ni.getBounds(), col);
			crossProbeChanged = true;
			netValues.remove(net);
		}

		// redraw all arcs in the layout/schematic window
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			int width = netlist.getBusWidth(ai);
			for(int i=0; i<width; i++)
			{
				Network net = netlist.getNetwork(ai, i);
				Integer state = netValues.get(net);
				if (state == null) continue;
				Color col = getHighlightColor(state.intValue());
				schemWnd.addCrossProbeLine(ai.getHeadLocation(), ai.getTailLocation(), col);
				crossProbeChanged = true;
			}
		}

		// if anything changed, queue the window for redisplay
		if (crossProbeChanged)
			schemWnd.repaint();
	}

	/**
	 * Method to convert a digital state to a color.
	 * The color is used when showing cross-probed levels in the EditWindow.
	 * The colors used to be user-selectable, but are not yet.
	 * @param state the digital state from the Waveform Window.
	 * @return the color to display in the EditWindow.
	 */
	private Color getHighlightColor(int state)
	{
		// determine trace color
		switch (state & Stimuli.LOGIC)
		{
			case Stimuli.LOGIC_LOW:  return new Color(User.getColor(User.ColorPrefType.WAVE_CROSS_LOW));
			case Stimuli.LOGIC_HIGH: return new Color(User.getColor(User.ColorPrefType.WAVE_CROSS_HIGH));
			case Stimuli.LOGIC_X:    return new Color(User.getColor(User.ColorPrefType.WAVE_CROSS_UNDEF));
			case Stimuli.LOGIC_Z:    return new Color(User.getColor(User.ColorPrefType.WAVE_CROSS_FLOAT));
		}
		return Color.RED;
	}

	private void putValueOnTrace(Signal<DigitalSample> ds, Cell cell, Map<Network,Integer> netValues, Netlist netlist)
	{
		// set simulation value on the network in the associated layout/schematic window
		Network net = findNetwork(netlist, ds.getSignalName());
		if (net == null) return;

		// find the proper data for the main cursor
		int numEvents = ds.getExactView().getNumEvents();
		int state = Stimuli.LOGIC_X;
		for(int i=numEvents-1; i>=0; i--)
		{
			double xValue = ds.getExactView().getTime(i);
			if (xValue <= mainXPosition)
			{
				state = getState(ds, i) & Stimuli.LOGIC;
				break;
			}
		}
		netValues.put(net, new Integer(state));
	}

	/**
	 * Method called when the main or extension cursors should be centered.
	 * @param main true for the main cursor, false for the extension cursor.
	 */
	public void centerCursor(boolean main)
	{
		boolean havePanel = false;
		double lowXValue = 0, highXValue = 0;
		for(Panel wp : wavePanels)
		{
			double low = wp.getMinXAxis();
			double high = wp.getMaxXAxis();
			if (havePanel)
			{
				lowXValue = Math.max(lowXValue, low);
				highXValue = Math.min(highXValue, high);
			} else
			{
				lowXValue = low;
				highXValue = high;
				havePanel = true;
			}
		}
		if (!havePanel) return;
		double center = (lowXValue + highXValue) / 2;
		if (main) setMainXPositionCursor(center); else
			setExtensionXPositionCursor(center);
		for(Panel wp : wavePanels)
		{
			wp.repaintWithRulers();
		}
	}

	// ************************************* STIMULI CONTROL *************************************

	/**
	 * Method to update the Simulation data for this waveform window.
	 * When new data is read from disk, this is used.
	 * @param sd new simulation data for this window.
	 */
	public void setSimData(Stimuli sd)
	{
		if (this.sd != null)
			this.sd.finished();
		this.sd = sd;

		// reset all bounds calculations
		for(Iterator<Analysis> it = sd.getAnalyses(); it.hasNext(); )
		{
			Analysis an = it.next();
			an.setBoundsDirty();
		}

		// reload the sweeps
		resetSweeps();

		// adjust the overall X axis signal (if it is not time)
		String oldXAxisSignalAllName = null;
		if (xAxisSignalAll != null)
		{
			oldXAxisSignalAllName = xAxisSignalAll.getFullName();
			xAxisSignalAll = null;
		}

		List<Panel> panelList = new ArrayList<Panel>();
		for(Panel wp : wavePanels)
			panelList.add(wp);
		for(Panel wp : panelList)
		{
			//Analysis<Signal> an = sd.findAnalysis(wp.getAnalysisType());
			boolean redoPanel = false;

			// adjust the panel's X axis signal (if it is not time)
			if (wp.getXAxisSignal() != null)
			{
				String oldSigName = wp.getXAxisSignal().getFullName();
				wp.setXAxisSignal(null);
                /*
				for(Signal newSs : an.getSignals())
				{
					String newSigName = newSs.getFullName();
					if (!newSigName.equals(oldSigName)) continue;
					wp.setXAxisSignal(newSs);
					break;
				}
                */
				if (wp.getXAxisSignal() == null)
				{
					System.out.println("Could not find X axis signal " + oldSigName + " in the new data");
					redoPanel = true;
				}
			}

            /*
			if (oldXAxisSignalAllName != null)
			{
				for(Signal newSs : an.getSignals())
				{
					String newSigName = newSs.getFullName();
					if (!newSigName.equals(oldXAxisSignalAllName)) continue;
					xAxisSignalAll = newSs;
					break;
				}
			}
            */

			// adjust all signals inside the panel
			for(WaveSignal ws : wp.getSignals())
			{
				Signal ss = ws.getSignal();
				if (ss .isDigital() && DigitalAnalysis.getBussedSignals(((Signal<DigitalSample>)ss)) != null)
				{
					List<Signal<DigitalSample>> inBus = DigitalAnalysis.getBussedSignals(((Signal<DigitalSample>)ss));
					for(int b=0; b<inBus.size(); b++)
					{
						Signal<DigitalSample> subDS = inBus.get(b);
						String oldSigName = subDS.getFullName();
						Signal<DigitalSample> newBus = null;
                        /*
						for(Signal newSs :  an.getSignals() )
						{
							String newSigName = newSs.getFullName();
							if (!newSigName.equals(oldSigName)) continue;
							newBus = (Signal<DigitalSample>)newSs;
							break;
						}
                        */
						if (newBus == null)
						{
							inBus.remove(b);
							b--;
							System.out.println("Could not find signal " + oldSigName + " in the new data");
							redoPanel = true;
							continue;
						}
						inBus.set(b, newBus);
					}
				} else
				{
					// single signal: find the name in the new list
					String oldSigName = ss.getFullName();
					ws.setSignal(null);
                    /*
					for(Signal newSs : an.getSignals())
					{
						String newSigName = newSs.getFullName();
						if (!newSigName.equals(oldSigName)) continue;
						ws.setSignal(newSs);
						break;
					}
                    */
					if (ws.getSignal() == null)
					{
						System.out.println("Could not find signal " + oldSigName + " in the new data");
						redoPanel = true;
					}
				}
			}
			while (redoPanel)
			{
				redoPanel = false;
				for(WaveSignal ws : wp.getSignals())
				{
					Signal s = ws.getSignal();
					if (s == null ||
						(s .isDigital() &&
                         DigitalAnalysis.getBussedSignals(((Signal<DigitalSample>)s)) != null &&
                         DigitalAnalysis.getBussedSignals(((Signal<DigitalSample>)s)).size() == 0))
					{
						redoPanel = true;
						if (wp.getSignalButtons() != null)
							wp.removeSignal(ws.getButton());
						break;
					}
				}
			}
			if (wp.getNumSignals() == 0)
			{
				// removed all signals: delete the panel
				wp.getWaveWindow().closePanel(wp);
			} else
			{
				if (wp.getSignalButtons() != null)
				{
					wp.getSignalButtons().validate();
					wp.getSignalButtons().repaint();
				}
				wp.repaintContents();
			}
		}
		if (oldXAxisSignalAllName != null && xAxisSignalAll == null)
			System.out.println("Could not find main X axis signal " + oldXAxisSignalAllName + " in the new data");
		wf.wantToRedoSignalTree();
		if (sd.getEngine() != null)
			System.out.println("Simulation data reloaded from circuit"); else
				System.out.println("Simulation data reloaded from disk");
	}

    public static WaveformWindow getCurrentWaveformWindow() {
		WindowFrame current = WindowFrame.getCurrentWindowFrame();
		WindowContent content = current.getContent();
		if (!(content instanceof WaveformWindow))
		{
			System.out.println("Must select a Waveform window first");
			return null;
		}
        return (WaveformWindow)content;
    }

	/**
	 * Method to write the simulation data as a tab-separated file.
	 */
	public static void exportSimulationData()
	{
		WindowFrame current = WindowFrame.getCurrentWindowFrame();
		WindowContent content = current.getContent();
		if (!(content instanceof WaveformWindow))
		{
			System.out.println("Must select a Waveform window first");
			return;
		}
		WaveformWindow ww = (WaveformWindow)content;

		String configurationFileName = OpenFile.chooseOutputFile(FileType.TEXT, "Waveform Export File", "wavedata.txt");
		if (configurationFileName == null) return;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(configurationFileName)));

			List<Signal> dumpSignals = new ArrayList<Signal>();
			List<Integer> dumpSweeps = new ArrayList<Integer>();
			List<Signal> dumpWaveforms = new ArrayList<Signal>();
			for(Panel wp : ww.wavePanels)
			{
				if (wp.isHidden()) continue;
				Signal signalInX = ww.xAxisSignalAll;
				if (!ww.xAxisLocked) signalInX = wp.getXAxisSignal();
				if (signalInX != null) addSignalSweep(signalInX, -1, dumpSignals, dumpSweeps, dumpWaveforms);
				for(WaveSignal ws : wp.getSignals())
                    addSignalSweep(ws.getSignal(), -1, dumpSignals, dumpSweeps, dumpWaveforms);
			}
			int numEntries = dumpSignals.size() + 1;
			String [] entries = new String[numEntries];
			entries[0] = "TIME";
			for(int i=1; i<numEntries; i++)
			{
				Signal sig = dumpSignals.get(i-1);
				entries[i] = sig.getFullName();
				int s = dumpSweeps.get(i-1).intValue();
				if (s >= 0)
				{
					entries[i] += "/S=" + s;
				}
			}
			for(int i=0; i<numEntries; i++)
			{
				if (i > 0) printWriter.print("\t");
				printWriter.print(entries[i]);
			}
			printWriter.println();
			double result[] = new double[3];
			for(int j=0; ; j++)
			{
				// get signal values for this iteration
				boolean haveData = false;
				entries[0] = null;
				for(int i=1; i<numEntries; i++)
				{
					entries[i] = "";
					Signal sig = dumpSignals.get(i-1);
                    if (j < sig.getExactView().getNumEvents()) {
                        Sample sample = sig.getExactView().getSample(j);
                        if (sample == null) {
                        } else if (sample instanceof ScalarSample) {
                            double t = sig.getExactView().getTime(j);
                            double v = ((ScalarSample)sample).getValue();
							if (entries[0] == null) entries[0] = "" + t;
							entries[i] = "" + v;
							haveData = true;
						} else if (sample instanceof DigitalSample) {
							if (entries[0] == null) entries[0] = "" + sig.getExactView().getTime(j);
							entries[i] = "" + getState((Signal<DigitalSample>)sig, j);
							haveData = true;
						}
					}
				}
				if (!haveData) break;
				if (entries[0] == null) entries[0] = "";
				for(int i=0; i<numEntries; i++)
				{
					if (i > 0) printWriter.print("\t");
					printWriter.print(entries[i]);
				}
				printWriter.println();
			}
			printWriter.close();
		} catch (IOException e)
		{
			System.out.println("Error writing configuration");
			return;
		}
		System.out.println("Wrote " + configurationFileName);
	}

	private static void addSignalSweep(Signal sig, int s, List<Signal> dumpSignals, List<Integer> dumpSweeps, List<Signal> waveforms)
	{
		for(int i=0; i<dumpSignals.size(); i++)
		{
			if (dumpSignals.get(i) == sig && dumpSweeps.get(i).intValue() == s) return;
		}
		dumpSignals.add(sig);
		dumpSweeps.add(new Integer(s));
		waveforms.add(sig);
	}

	/**
	 * Method to refresh simulation data by menu in ToolMenu. This would allow to attach a KeyBinding
	 */
	public static void refreshSimulationData()
	{
		WindowFrame current = WindowFrame.getCurrentWindowFrame();
		WindowContent content = current.getContent();
		if (!(content instanceof WaveformWindow))
		{
			System.out.println("Nothing to refresh in non Waveform window");
			return; // nothing to do
		}
		((WaveformWindow)content).refreshData();
	}

	/**
	 * Method to clear all panels from the waveform window.
	 */
	public static void clearSimulationData()
	{
		WindowFrame current = WindowFrame.getCurrentWindowFrame();
		WindowContent content = current.getContent();
		if (!(content instanceof WaveformWindow))
		{
			System.out.println("Nothing to refresh in non Waveform window");
			return; // nothing to do
		}
		WaveformWindow ww = (WaveformWindow)content;

		// clear the display
		ww.clearAllPanels();
		ww.saveSignalOrder();
	}

	/**
	 * Method to remove all panels from the display.
	 */
	private void clearAllPanels()
	{
		List<Panel> closeList = new ArrayList<Panel>();
		for(Panel wp : wavePanels)
			closeList.add(wp);
		for(Panel wp : closeList)
		{
			closePanel(wp);
		}
	}

	/**
	 * Method to refresh the simulation data from disk.
	 */
	private void refreshData()
	{
//		// if there is no stimuli file, simulator is built-in: update it
//		if (sd.getFileURL() == null)
//		{
//			sd.getEngine().update();
//			return;
//		}

		// if there is a simulation engine (i.e. IRISM, ALS) ask simulator to reload circuit
		if (sd.getEngine() != null)
		{
			sd.getEngine().refresh();
			return;
		}

		// there is no simulation engine (i.e. external Spice, Verilog) reload external data
		SimulationData.plot(sd.getCell(), sd.getFileURL(), this);
	}

	/**
	 * Method to save the waveform window configuration to a disk file.
	 */
	public static void saveConfiguration()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		WaveformWindow ww = findWaveformWindow(cell);
		if (ww == null)
		{
			System.out.println("There is no waveform window to save");
			return;
		}

		String configurationFileName = OpenFile.chooseOutputFile(FileType.TEXT, "Waveform Configuration File", "waveform.txt");
		if (configurationFileName == null) return;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(configurationFileName)));
			for(int i=0; i<ww.wavePanels.size(); i++)
			{
				Panel wp = ww.wavePanels.get(i);
				if (wp.isHidden()) continue;
				boolean first = true;
				for(WaveSignal ws : wp.getSignals())
				{
					String sigName = ws.getSignal().getFullName();
					if (first)
					{
						// header
						first = false;
						String analysisName = "";
						//if (wp.getAnalysisType() != null) analysisName = " " + wp.getAnalysisType();
						String log = "";
						if (wp.isPanelLogarithmicHorizontally()) log = " xlog";
						if (wp.isPanelLogarithmicVertically()) log += " ylog";
						if (i > 0) printWriter.println();
						printWriter.println("panel" + analysisName + log);
						printWriter.println("zoom " + wp.getYAxisLowValue() + " " + wp.getYAxisHighValue() +
							" " + wp.getMinXAxis() + " " + wp.getMaxXAxis());
						Signal signalInX = ww.xAxisSignalAll;
						if (!ww.xAxisLocked) signalInX = wp.getXAxisSignal();
						if (signalInX != null) printWriter.println("x-axis " + signalInX.getFullName());
					}
					Color color = ws.getColor();
					printWriter.println("signal " + sigName + " " + color.getRed() + "," + color.getGreen() + "," + color.getBlue());
				}
			}
			printWriter.close();
		} catch (IOException e)
		{
			System.out.println("Error writing configuration");
			return;
		}
		System.out.println("Wrote " + configurationFileName);
	}

	/**
	 * Method to restore the waveform window configuration from a disk file.
	 */
	public static void restoreConfiguration()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		WaveformWindow ww = findWaveformWindow(cell);
		if (ww == null)
		{
			System.out.println("There is no waveform window to restore");
			return;
		}

		String configurationFileName = OpenFile.chooseInputFile(FileType.TEXT, "Waveform Configuration File");
		if (configurationFileName == null) return;

		// clear the display
		ww.clearAllPanels();

		// read the file
		URL url = TextUtils.makeURLToFile(configurationFileName);
		Panel curPanel = null;
		Analysis.AnalysisType oneType = null;
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String buf = lineReader.readLine();
				if (buf == null) break;
				String [] keywords = buf.split(" ");
				if (keywords.length == 0) continue;
				if (keywords[0].equals("panel"))
				{
					Analysis.AnalysisType analysisType = null;
					boolean xLog = false, yLog = false;
					for(int i=1; i<keywords.length; i++)
					{
						if (keywords[i].equals("xlog")) xLog = true; else
						if (keywords[i].equals("ylog")) yLog = true; else
						{
							analysisType = Analysis.AnalysisType.findAnalysisType(keywords[i]);
							if (analysisType != null)
							{
								if (oneType == null) oneType = analysisType;
								if (oneType != analysisType && ww.isXAxisLocked()) ww.togglePanelXAxisLock();
							}
						}
					}
					curPanel = new Panel(ww, ww.getSimData().isAnalog());
					if (xLog)
					{
						if (ww.isXAxisLocked()) ww.togglePanelXAxisLock();
						curPanel.setPanelLogarithmicHorizontally(true);
					}
					if (yLog) curPanel.setPanelLogarithmicVertically(true);
					continue;
				}
				if (keywords[0].equals("zoom"))
				{
					if (curPanel == null) continue;
					double lowYValue = TextUtils.atof(keywords[1]);
					double highYValue = TextUtils.atof(keywords[2]);
					double lowXValue = TextUtils.atof(keywords[3]);
					double highXValue = TextUtils.atof(keywords[4]);
					curPanel.setXAxisRange(lowXValue, highXValue);
					if (curPanel.isAnalog())
						curPanel.setYAxisRange(lowYValue, highYValue);
					continue;
				}
				if (keywords[0].equals("x-axis"))
				{
					if (curPanel == null) continue;
					Stimuli sd = ww.getSimData();
					Analysis an = sd.getAnalyses().next();
					//if (curPanel.getAnalysisType() != null) an = sd.findAnalysis(curPanel.getAnalysisType());
					if (an == null) continue;
					Signal sig = an.findSignalForNetwork(keywords[1]);
					if (sig == null) continue;
					if (ww.isXAxisLocked()) ww.togglePanelXAxisLock();
					curPanel.setXAxisSignal(sig);
					continue;
				}
				if (keywords[0].equals("signal"))
				{
					if (curPanel == null) continue;
					Stimuli sd = ww.getSimData();
					Analysis an = sd.getAnalyses().next();
					//if (curPanel.getAnalysisType() != null) an = sd.findAnalysis(curPanel.getAnalysisType());
					if (an == null) continue;
					Signal sig = an.findSignalForNetwork(keywords[1]);
					if (sig == null) continue;
					String [] colorNames = keywords[2].split(",");
					int red = TextUtils.atoi(colorNames[0]);
					int green = TextUtils.atoi(colorNames[1]);
					int blue = TextUtils.atoi(colorNames[2]);
					Color color = new Color(red, green, blue);
					WaveSignal ws = new WaveSignal(curPanel, sig);
					ws.setColor(color);
					continue;
				}
			}
			lineReader.close();
			for(Panel panel : ww.wavePanels)
			{
				panel.repaintWithRulers();
			}
			ww.saveSignalOrder();
		} catch (IOException e)
		{
			System.out.println("Error reading " + configurationFileName);
			return;
		}
	}

	private static Map<CellId,String> savedSignalOrder = new HashMap<CellId,String>();

	/**
	 * Method to save the signal ordering on the cell.<BR>
	 *
	 * Saved signals are stored in the Preferences as a single string.
	 * The string is located in com.sun.electric.database.hierarchy.<LibraryName>
	 * in a Preference named SavedSignalsForCell<CellName>.<BR>
	 *
	 * The format of the string is as follows:
	 * Each panel in the waveform window is terminated by "\n", so for example two panels would look like this:<BR>
	 *   <PanelInfo> \n <PanelInfo> \n<BR>
	 * Each PanelInfo section starts with information about the analysis in that panel,
	 * and then lists the signals in that panel.  The format is:<BR>
	 *    \t [<AnalysisName>] [(<HorizontalSignal>)] <SignalList><BR>
	 * Where <SignalList> is one or more of this:<BR>
	 *    \t <SignalName> { <Red> , <Green> , <Blue> }<BR>
	 *
	 * Example:<BR>
	 *    \t\t1:net_198 {255,0,0}\n<BR>
	 * This has just one \n in it so it defines a single panel.
	 * The first \t introduces the analysis type which is blank, meaning that it is
	 * a Transient analysis and has Time as the horizontal axis.
	 * There is just one signal listed (1:net_198) and its color is red (255,0,0).
	 */
	public void saveSignalOrder()
	{
		Cell cell = getCell();
		if (cell == null) return;
		StringBuffer sb = new StringBuffer();
		for(Panel wp : wavePanels)
		{
			boolean first = true;
			for(WaveSignal ws : wp.getSignals())
			{
				String sigName = ws.getSignal().getFullName();
				if (first)
				{
					// header begins with a tab
					//sb.append("\t" + wp.getAnalysisType());
					sb.append("\t");
					Signal signalInX = xAxisSignalAll;
					if (!xAxisLocked) signalInX = wp.getXAxisSignal();
					first = false;
					if (signalInX != null) sb.append("(" + signalInX.getFullName() + ")");
				}
				sb.append("\t");
				sb.append(sigName);
				Color color = ws.getColor();
				sb.append(" {" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "}");
			}
			sb.append("\n");
		}
		savedSignalOrder.put(cell.getId(), sb.toString());
	}

	/**
	 * Method called when the program exits to preserve signal ordering in cells.
	 */
	public static void preserveSignalOrder()
	{
		Pref.delayPrefFlushing();
		for(Map.Entry<CellId,String> e: savedSignalOrder.entrySet())
		{
            CellId cellId = e.getKey();
			String savedOrder = e.getValue();
            Preferences libPrefs = Pref.getLibraryPreferences(cellId.libId);
            String key = "SavedSignalsForCell" + cellId.cellName.getName();
            if (savedOrder.length() == 0)
                libPrefs.remove(key);
            else
                libPrefs.put(key, savedOrder);
		}
		Pref.resumePrefFlushing();
	}

	/**
	 * Method to get the saved signal information for a cell.
	 * @param cell the Cell to query.
	 * @return a list of strings, one per waveform window panel, with tab-separated signal names in that panel.
	 * Returns an empty array if nothing is saved.
	 */
	public static String [] getSignalOrder(Cell cell)
	{
        CellId cellId = cell.getId();
		String savedOrder = savedSignalOrder.get(cellId);
		if (savedOrder == null)
		{
            Preferences libPrefs = Pref.getLibraryPreferences(cellId.libId);
            String key = "SavedSignalsForCell" + cellId.cellName.getName();
			savedOrder = libPrefs.get(key, "");
			if (savedOrder.length() == 0) return new String[0];
		}

		// convert a single string into an array of strings
		List<String> panels = new ArrayList<String>();
		int startPos = 0;
		for(;;)
		{
			int endCh = savedOrder.indexOf('\n', startPos);
			if (endCh < 0) break;
			String panel = savedOrder.substring(startPos, endCh);
			panels.add(panel);
			startPos = endCh + 1;
		}
		String [] ret = new String[panels.size()];
		int i=0;
		for(String s : panels)
			ret[i++] = s;
		return ret;
	}

	// ************************************* DISPLAY CONTROL *************************************

	public Font getFont() { return waveWindowFont; }

	public FontRenderContext getFontRenderContext() { return waveWindowFRC; }

	public Color getOffStrengthColor() { return offStrengthColor; }

	public Color getNodeStrengthColor() { return nodeStrengthColor; }

	public Color getGateStrengthColor() { return gateStrengthColor; }

	public Color getPowerStrengthColor() { return powerStrengthColor; }

	/**
	 * Method called to toggle the display of vertex points.
	 */
	public void toggleShowPoints()
	{
		linePointMode = (linePointMode+1) % 3;
		switch (linePointMode)
		{
			case 0: showPoints.setIcon(iconLineOnPointOff);   break;
			case 1: showPoints.setIcon(iconLineOnPointOn);    break;
			case 2: showPoints.setIcon(iconLineOffPointOn);   break;
		}
		for(Panel wp : wavePanels)
		{
			wp.repaintWithRulers();
		}
	}

	/**
	 * Method to return the drawing mode for analog waves.
	 * @return the drawing mode for analog waves.
	 * 0 means draw lines only; 1 means draw lines and points; 2 means draw points only.
	 */
	public int getLinePointMode() { return linePointMode;}

	/**
	 * Method called to toggle the display of a grid.
	 */
	public void toggleGridPoints()
	{
		showGrid = !showGrid;
		for(Panel wp : wavePanels)
		{
			wp.repaintWithRulers();
		}
	}

	public boolean isShowGrid() { return showGrid; }

	/**
	 * Method to add a signal to the display.
	 * Called when the user double-clicks on the signal in the explorer tree.
	 * @param sig the Signal to add to the display
	 */
	public void addSignal(Signal sig)
	{
		if (sig .isAnalog())
		{
			// add analog signal on top of current panel
			Signal as = (Signal)sig;
			boolean found = false;
			for(Panel wp : wavePanels)
			{
				if (wp.isSelected())
				{
					if (wp.wrongPanelType(as)) return;
					WaveSignal.addSignalToPanel(sig, wp, null);
					if (getMainHorizRuler() != null)
						getMainHorizRuler().repaint();
					found = true;
					break;
				}
			}
			if (!found)
			{
				// create a new panel for the signal
				Panel wp = makeNewPanel();
                Signal sSig = as;
                wp.fitToSignal(as);
                if (!xAxisLocked)
                    wp.setXAxisRange(as.getMinTime(), as.getMaxTime());
                WaveSignal.addSignalToPanel(sig, wp, null);
                if (getMainHorizRuler() != null)
                    getMainHorizRuler().repaint();
			}
		} else
		{
			// add digital signal in new panel
			Panel wp = makeNewPanel();
			new WaveSignal(wp, sig);
			overall.validate();
			wp.repaintContents();
		}
		saveSignalOrder();
	}

	/**
	 * Method called when "delete" command (or key) is given.
	 * If a control point is selected, delete it.
	 * If a single signal of an analog window is selected, remove it.
	 */
	public void deleteSelectedSignals()
	{
		for(Panel wp : wavePanels)
		{
			if (!wp.isSelected()) continue;

			boolean removedSingleStimuli = false;
			for(WaveSignal ws : wp.getSignals())
			{
				if (ws.getSelectedControlPoints() != null)
				{
					if (sd.getEngine() != null)
					{
						if (sd.getEngine().removeSelectedStimuli())
							removedSingleStimuli = true;
					}
				}
			}
			if (/*wp.getAnalysisType() != null &&*/ !removedSingleStimuli) deleteSignalFromPanel(wp);
			break;
		}
	}

	/**
	 * Method to make the waveform window/panel fill in X only.
	 */
	public static void fillInX()
	{
		WindowFrame current = WindowFrame.getCurrentWindowFrame();
		WindowContent content = current.getContent();
		if (!(content instanceof WaveformWindow))
		{
			System.out.println("Must select a Waveform window first");
			return;
		}
		WaveformWindow ww = (WaveformWindow)content;
		ww.fillWaveform(1);
	}

	/**
	 * Method to make the waveform window/panel fill in Y only.
	 */
	public static void fillInY()
	{
		WindowFrame current = WindowFrame.getCurrentWindowFrame();
		WindowContent content = current.getContent();
		if (!(content instanceof WaveformWindow))
		{
			System.out.println("Must select a Waveform window first");
			return;
		}
		WaveformWindow ww = (WaveformWindow)content;
		ww.fillWaveform(2);
	}

	/**
	 * Method to make the waveform window/panel fill in X and Y.
	 */
	public void fillScreen()
	{
		fillWaveform(3);
	}

	/**
	 * Method to make the stimuli fill the waveform window.
	 * @param how what to fill: 1=X, 2=Y, 3=both.
	 */
	private void fillWaveform(int how)
	{
		// accumulate bounds for all displayed panels
		double leftEdge=0, rightEdge=0;
		for(Panel wp : wavePanels) {
			if (wp.getXAxisSignal() != null) continue;
			for(WaveSignal ws : wp.getSignals()) {
				if (leftEdge == rightEdge) {
					leftEdge  = ws.getSignal().getMinTime();
					rightEdge = ws.getSignal().getMaxTime();
				} else {
					leftEdge  = Math.min(leftEdge,  ws.getSignal().getMinTime());
					rightEdge = Math.max(rightEdge, ws.getSignal().getMaxTime());
				}
			}
//			Analysis an = sd.findAnalysis(wp.getAnalysisType());
//			if (leftEdge == rightEdge)
//			{
//				leftEdge = an.getMinTime();
//				rightEdge = an.getMaxTime();
//			} else
//			{
//				if (leftEdge < rightEdge)
//				{
//					leftEdge = Math.min(leftEdge, an.getMinTime());
//					rightEdge = Math.max(rightEdge, an.getMaxTime());
//				} else
//				{
//					leftEdge = Math.max(leftEdge, an.getMinTime());
//					rightEdge = Math.min(rightEdge, an.getMaxTime());
//				}
//			}
		}

		// if there is an overriding signal on the X axis, use its bounds
		if (xAxisLocked && xAxisSignalAll != null) {
			leftEdge = xAxisSignalAll.getMinTime();
			rightEdge = xAxisSignalAll.getMaxTime();
		}

		for(Panel wp : wavePanels) {
            if (!xAxisLocked && !wp.isSelected()) continue;
            if ((how&2)!=0) wp.fitToSignal(null);
            if (leftEdge != rightEdge && (wp.getMinXAxis() != leftEdge || wp.getMaxXAxis() != rightEdge) && (how&1) != 0) {
                wp.setXAxisRange(leftEdge, rightEdge);
                wp.repaintWithRulers();
            }
        }
	}

	public void zoomOutContents()
	{
		for(Panel wp : wavePanels)
		{
			if (!xAxisLocked && !wp.isSelected()) continue;

			boolean timeInXAxis = true;
			if (xAxisLocked)
			{
				if (xAxisSignalAll != null) timeInXAxis = false;
			} else
			{
				if (wp.getXAxisSignal() != null) timeInXAxis = false;
			}
			double range = wp.getMaxXAxis() - wp.getMinXAxis();
			wp.setXAxisRange(wp.getMinXAxis() - range/2, wp.getMaxXAxis() + range/2);
			if (wp.getMinXAxis() < 0 && timeInXAxis)
			{
				wp.setXAxisRange(0, wp.getMaxXAxis() - wp.getMinXAxis());
			}
			wp.repaintWithRulers();
		}
	}

	public void zoomInContents()
	{
		for(Panel wp : wavePanels)
		{
			if (!xAxisLocked && !wp.isSelected()) continue;

			double range = wp.getMaxXAxis() - wp.getMinXAxis();
			wp.setXAxisRange(wp.getMinXAxis() + range/4, wp.getMaxXAxis() - range/4);
			wp.repaintWithRulers();
		}
	}

	public void focusOnHighlighted()
	{
		if (mainXPosition == extXPosition) return;
		double maxXPosition, minXPosition;
		if (mainXPosition > extXPosition)
		{
			double size = (mainXPosition-extXPosition) / 20.0;
			maxXPosition = mainXPosition + size;
			minXPosition = extXPosition - size;
		} else
		{
			double size = (extXPosition-mainXPosition) / 20.0;
			maxXPosition = extXPosition + size;
			minXPosition = mainXPosition - size;
		}
		for(Panel wp : wavePanels)
		{
			if (!xAxisLocked && !wp.isSelected()) continue;
			if (wp.getMinXAxis() != minXPosition || wp.getMaxXAxis() != maxXPosition)
			{
				if (wp.getMinXAxis() > wp.getMaxXAxis()) wp.setXAxisRange(maxXPosition, minXPosition); else
					wp.setXAxisRange(minXPosition, maxXPosition);
				wp.repaintWithRulers();
			}
		}
	}

	public boolean isWaveWindowLogarithmic() { return mainHorizRulerPanelLogarithmic; }

	public void setWaveWindowLogarithmic(boolean logarithmic)
	{
		mainHorizRulerPanelLogarithmic = logarithmic;
		mainHorizRulerPanel.repaint();
	}

	// ************************************* DRAG AND DROP CLASSES *************************************

	/**
	 * This class extends JPanel so that components of the Waveform window can be identified by the Drag and Drop system
	 * and by the key binding manager.
	 */
	public static class OnePanel extends JPanel
	{
		Panel panel;
		WaveformWindow ww;

		public OnePanel(Panel panel, WaveformWindow ww)
		{
			super();
			this.panel = panel;
			this.ww = ww;
		}

		public Panel getPanel() { return panel; }

		public WaveformWindow getWaveformWindow() { return ww; }
	}

	private static class WaveFormDropTarget implements DropTargetListener
	{
		public void dragEnter(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragOver(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dropActionChanged(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragExit(DropTargetEvent e) {}

		/**
		 * Entry point when something is dropped onto the waveform window.
		 * The data that is transferred is always a string with these values:
		 *
		 * "PANEL #" means an entire panel has been dragged (user has rearranged panels)
		 *    Drag originates in Panel.DragLabel.dragGestureRecognized().
		 *
		 * "TRANS sig" / "DC sig" / "AC sig" / "MEASUREMENT sig" means
		 *    a transient/DC/AC/measurement signal has been dragged (from the Explorer tree)
		 *    Drag originates in ExplorerTree.dragGestureRecognized().
		 *    Multiple signals may be combined, separated by Newline.
		 *
		 * "PANEL # MOVEBUTTON sig" / "PANEL # COPYBUTTON sig" means a signal has been
		 *    moved or copied (from one panel to another).
		 *    Drag originates in DragButton.dragGestureRecognized().
		 */
		public void drop(DropTargetDropEvent dtde)
		{
			// get information about the drop (such as the signal name)
			Object data = null;
			try
			{
				dtde.acceptDrop(DnDConstants.ACTION_LINK);
				data = dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
				if (data == null)
				{
					dtde.dropComplete(false);
					return;
				}
			} catch (Throwable t)
			{
				ActivityLogger.logException(t);
				dtde.dropComplete(false);
				return;
			}
			if (!(data instanceof String))
			{
				dtde.dropComplete(false);
				return;
			}
			String sigNameData = (String)data;
			String [] sigNames = sigNameData.split("\n");
			Analysis.AnalysisType analysisType = null;
			for(int i=0; i<sigNames.length; i++)
			{
				Analysis.AnalysisType anAnalysisType = Analysis.ANALYSIS_SIGNALS;
				String aSigName = sigNames[i];
				if (aSigName.startsWith("TRANS "))
				{
					sigNames[i] = aSigName.substring(6);
					anAnalysisType = Analysis.ANALYSIS_TRANS;
				} else if (aSigName.startsWith("MEASUREMENT "))
				{
					sigNames[i] = aSigName.substring(12);
					anAnalysisType = Analysis.ANALYSIS_MEAS;
				} else if (aSigName.startsWith("AC "))
				{
					sigNames[i] = aSigName.substring(3);
					anAnalysisType = Analysis.ANALYSIS_AC;
				} else if (aSigName.startsWith("DC "))
				{
					sigNames[i] = aSigName.substring(3);
					anAnalysisType = Analysis.ANALYSIS_DC;
				}
				if (analysisType == null) analysisType = anAnalysisType; else
				{
					if (analysisType != anAnalysisType)
					{
						Job.getUserInterface().showErrorMessage("All signals must be the same type", "Incorrect Signal Selection");
						dtde.dropComplete(false);
						return;
					}
				}
			}
			if (analysisType == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// see if the signal was dropped onto a ruler panel (setting x-axis)
			DropTarget dt = (DropTarget)dtde.getSource();
			if (dt.getComponent() instanceof HorizRuler)
			{
				// dragged a signal to the ruler panel: make sure only one signal was selected
				if (sigNames.length != 1)
				{
					Job.getUserInterface().showErrorMessage("Only one signal can be dragged to a ruler", "Too Much Selected");
					dtde.dropComplete(false);
					return;
				}
				HorizRuler hr = (HorizRuler)dt.getComponent();
				Panel panel = hr.getPanel();
				WaveformWindow ww = hr.getWaveformWindow();

				// find the signal that was dragged
				Signal sSig = null;
				if (sigNames[0].startsWith("PANEL "))
				{
					// get signal when dragged from inside the waveform window
					int sigPos = Math.max(sigNames[0].indexOf("MOVEBUTTON "), sigNames[0].indexOf("COPYBUTTON "));
					if (sigPos >= 0)
					{
						// dragging from waveform window signal to horizontal ruler
						int panelNumber = TextUtils.atoi(sigNames[0].substring(6));
						Panel sourcePanel = ww.getPanelFromNumber(panelNumber);
						String signalName = sigNames[0].substring(sigPos + 11);
						for(WaveSignal ws : sourcePanel.getSignals())
						{
							if (!ws.getSignal().getFullName().equals(signalName)) continue;
							sSig = ws.getSignal();
							break;
						}
						analysisType = sSig.getAnalysisType();
					}
				} else
				{
					Analysis an = ww.getSimData().findAnalysis(analysisType);
					if (an == null)
					{
						System.out.println("Cannot find " + analysisType + " data");
						dtde.dropComplete(true);
						return;
					}
					sSig = ww.findSignal(sigNames[0], an);
				}
				if (sSig != null)
				{
					if (panel == null)
					{
						// dropped signal onto main time ruler: make sure it is the right type
						boolean warn = false;
                        /*
						for(Panel wp : ww.wavePanels)
						{
							if (wp.getAnalysisType() != analysisType && wp.getNumSignals() > 0) warn = true;
						}
						if (warn)
						{
							String warning = "The waveform window is not showing " + analysisType +
								" data.  Remove all traces and convert panels to show " + analysisType + " data?";
							int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), warning);
							if (response != JOptionPane.YES_OPTION)
							{
								dtde.dropComplete(true);
								return;
							}
							for(Panel wp : ww.wavePanels)
								ww.deleteAllSignalsFromPanel(wp);
						}
                        */
						ww.xAxisSignalAll = sSig;
						for(Panel wp : ww.wavePanels)
						{
							//wp.setAnalysisType(analysisType);
							wp.setXAxisRange(sSig.getMinTime(), sSig.getMaxTime());
						}
						ww.redrawAllPanels();
					} else
					{
                        /*
						// dropped signal onto a single panel's time ruler
						if (panel.getAnalysisType() != analysisType)
						{
							JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
								"Cannot drop a " + analysisType + " signal onto the horizontal ruler of a " +
								panel.getAnalysisType() + " panel.  " +
								"First convert the panel with the popup in the upper-left.",
								"Error Displaying Signals", JOptionPane.ERROR_MESSAGE);
							dtde.dropComplete(true);
							return;
						}
                        */
						panel.setXAxisSignal(sSig);
						panel.setXAxisRange(sSig.getMinTime(), sSig.getMaxTime());
						panel.repaintContents();
					}
					hr.repaint();
					ww.saveSignalOrder();
				}
				dtde.dropComplete(false);
				return;
			}

			// determine which panel was the target of the drop
			WaveformWindow ww = null;
			Panel panel = null;
			if (dt.getComponent() instanceof Panel)
			{
				panel = (Panel)dt.getComponent();
				ww = panel.getWaveWindow();
			}
			if (dt.getComponent() instanceof OnePanel)
			{
				OnePanel op = (OnePanel)dt.getComponent();
				ww = op.getWaveformWindow();
				panel = op.getPanel();
			}
			if (dt.getComponent() instanceof WaveTable)
			{
				WaveTable table = (WaveTable)dt.getComponent();
				ww = table.ww;
				int row = table.rowAtPoint(dtde.getLocation());
				if (row != -1)
				{
					Object obj = ww.tableModel.getValueAt(row, 0);
					OnePanel op = (OnePanel)obj;
					panel = op.getPanel();
				}
			}
			if (panel == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// see if rearranging the waveform window
			if (sigNames[0].startsWith("PANEL "))
			{
				// rearranging signals and panels
				int panelNumber = TextUtils.atoi(sigNames[0].substring(6));
				Panel sourcePanel = ww.getPanelFromNumber(panelNumber);
				if (sourcePanel == panel)
				{
					// moved to same panel
					dtde.dropComplete(false);
					return;
				}

				// see if a signal button was grabbed
				int sigMovePos = sigNames[0].indexOf("MOVEBUTTON ");
				int sigCopyPos = sigNames[0].indexOf("COPYBUTTON ");
				if (!panel.isAnalog()) sigMovePos = sigCopyPos = -1;
				if (sigMovePos < 0 && sigCopyPos < 0)
				{
					// moving the entire panel
					if (USETABLES)
					{
						ww.stopEditing();

						ww.wavePanels.remove(sourcePanel);
						int destIndex = ww.wavePanels.indexOf(panel);
						if (dtde.getLocation().y > panel.getBounds().height/2)
							destIndex++;
						ww.wavePanels.add(destIndex, sourcePanel);
						ww.reloadTable();
						ww.table.repaint();
					} else
					{
						ww.left.remove(sourcePanel.getLeftHalf());
						ww.right.remove(sourcePanel.getRightHalf());
						int destIndex = 0;
						Component [] lefts = ww.left.getComponents();
						for(destIndex=0; destIndex < lefts.length; destIndex++)
						{
							if (lefts[destIndex] == panel.getLeftHalf()) break;
						}
						if (dtde.getLocation().y > panel.getBounds().height/2)
							destIndex++;
						ww.left.add(sourcePanel.getLeftHalf(), destIndex);
						ww.right.add(sourcePanel.getRightHalf(), destIndex);
					}

					ww.getPanel().validate();
					dtde.dropComplete(true);
					ww.saveSignalOrder();
					return;
				}

				// moving/copying a signal (analog only)
				int sigPos = Math.max(sigMovePos, sigCopyPos);
				String signalName = sigNames[0].substring(sigPos + 11);
				Signal sSig = null;
				Color oldColor = null;
				for(WaveSignal ws : sourcePanel.getSignals())
				{
					if (!ws.getSignal().getFullName().equals(signalName)) continue;
					sSig = ws.getSignal();
					if (panel.wrongPanelType(sSig))
					{
						dtde.dropComplete(true);
						return;
					}
					oldColor = ws.getColor();
					if (sigCopyPos < 0)
					{
						sourcePanel.removeHighlightedSignal(ws, true);
						sourcePanel.removeSignal(ws.getButton());
					}
					break;
				}
				if (sSig != null)
				{
					sourcePanel.getSignalButtons().validate();
					sourcePanel.getSignalButtons().repaint();
					sourcePanel.repaintContents();
					WaveSignal.addSignalToPanel(sSig, panel, oldColor);
				}
				ww.saveSignalOrder();
				dtde.dropComplete(true);
				return;
			}

			// not rearranging: dropped a signal onto a panel
			Analysis an = ww.getSimData().findAnalysis(analysisType);
			for(int i=0; i<sigNames.length; i++)
			{
				Signal sSig = ww.findSignal(sigNames[i], an);
				if (sSig == null)
				{
					dtde.dropComplete(false);
					return;
				}

				// digital signals are always added in new panels
				if (sSig .isDigital()) panel = null;
				if (panel != null)
				{
					// overlay this signal onto an existing panel
					if (panel.wrongPanelType(sSig))
					{
						dtde.dropComplete(true);
						return;
					}
					WaveSignal.addSignalToPanel(sSig, panel, null);
					panel.makeSelectedPanel(-1, -1);
					continue;
				}

				// add this signal in a new panel
				panel = ww.makeNewPanel();
				panel.fitToSignal(sSig);
				new WaveSignal(panel, sSig);
			}
			ww.overall.validate();
			panel.repaintContents();
			panel.getWaveWindow().saveSignalOrder();
			dtde.dropComplete(true);
		}
	}

	// ************************************* CLASS TO ASSOCIATE WAVEFORM WINDOWS WITH EDIT WINDOWS *************************************

	/**
	 * Class to find the WaveformWindow associated with the cell in a given EditWindow.
	 * May have to climb the hierarchy to find the top-level cell that is being simulated.
	 */
	public static class Locator
	{
		private WaveformWindow ww;
		private VarContext context;

		/**
		 * The constructor takes an EditWindow and locates the associated WaveformWindow.
		 * It may have to climb the hierarchy to find it.
		 * @param wnd the EditWindow that is being simulated.
		 */
		public Locator(EditWindow wnd)
		{
			Cell cellInWindow = wnd.getCell();
			VarContext curContext = wnd.getVarContext();
			ww = null;
			Stack<Nodable> contextStack = new Stack<Nodable>();
			for(;;)
			{
				ww = WaveformWindow.findWaveformWindow(cellInWindow);
				if (ww != null) break;
				Nodable no = curContext.getNodable();
				if (no == null) break;
				contextStack.push(no);
				cellInWindow = no.getParent();
				curContext = curContext.pop();
				//context = no.getName() + "." + context;
			}
			context = VarContext.globalContext;
			while (!contextStack.isEmpty()) {
				context = context.push(contextStack.pop());
			}
		}

		/**
		 * The constructor takes an EditWindow and a WaveformWindow and determines whether they are associated.
		 * It may have to climb the hierarchy to find out.
		 * @param wnd the EditWindow that is being simulated.
		 * @param wantWW the WaveformWindow that is being associated.
		 */
		public Locator(EditWindow wnd, WaveformWindow wantWW)
		{
			Cell cellInWindow = wnd.getCell();
			VarContext curContext = wnd.getVarContext();
			ww = null;
			Stack<Nodable> contextStack = new Stack<Nodable>();
			for(;;)
			{
				if (wantWW.getCell() == cellInWindow) { ww = wantWW;   break; }
				Nodable no = curContext.getNodable();
				if (no == null) break;
				contextStack.push(no);
				cellInWindow = no.getParent();
				curContext = curContext.pop();
			}
			context = VarContext.globalContext;
			while (!contextStack.isEmpty()) {
				context = context.push(contextStack.pop());
			}
		}

		/**
		 * Method to return the WaveformWindow found by this locator class.
		 * @return the WaveformWindow associated with the EditWindow given to the contructor.
		 * Returns null if no WaveformWindow could be found.
		 */
		public WaveformWindow getWaveformWindow() { return ww; }

		/**
		 * Method to return the context of all signals in the EditWindow given to the constructor.
		 * @return the context to prepend to all signals in the EditWindow.
		 * If the EditWindow is directly associated with a WaveformWindow, returns "".
		 */
		public VarContext getContext() { return context; }
	}

	// ************************************* HIGHLIGHT LISTENER FOR ALL WAVEFORM WINDOWS *************************************

	private static class WaveformWindowHighlightListener implements HighlightListener
	{
		/**
		 * Method to highlight waveform signals corresponding to circuit networks that are highlighted.
		 * Method is called when any edit window changes its highlighting.
		 */
		public void highlightChanged(Highlighter which)
		{
			// if this is a response to crossprobing from waveform to schematic, stop now
			if (freezeWaveformHighlighting) return;

			// find the EditWindow that this change comes from
			WindowFrame highWF = which.getWindowFrame();
			if (highWF == null) return;
			if (!(highWF.getContent() instanceof EditWindow)) return;
			EditWindow wnd = (EditWindow)highWF.getContent();

			// loop through all windows, looking for waveform windows
			if (which == wnd.getHighlighter())
			{
				for(Iterator<WindowFrame> wIt = WindowFrame.getWindows(); wIt.hasNext(); )
				{
					WindowFrame wf = wIt.next();
					if (!(wf.getContent() instanceof WaveformWindow)) continue;
					WaveformWindow ww = (WaveformWindow)wf.getContent();
					ww.crossProbeEditWindowToWaveform(wnd, which);
				}
			}
		}

		/**
		 * Called when by a Highlighter when it loses focus. The argument
		 * is the Highlighter that has gained focus (may be null).
		 * @param highlighterGainedFocus the highlighter for the current window (may be null).
		 */
		public void highlighterLostFocus(Highlighter highlighterGainedFocus) {}
	}

	// ************************************* HELPER CLASS FOR WAVEFORM WINDOW *************************************

	private static class WaveComponentListener implements ComponentListener
	{
		private JPanel panel;

		public WaveComponentListener(JPanel panel) { this.panel = panel; }

		public void componentHidden(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentResized(ComponentEvent e)
		{
			panel.repaint();
		}
		public void componentShown(ComponentEvent e) {}
	}

	public void propertyChange(PropertyChangeEvent e)
	{
		if (!USETABLES)
		{
			if (e.getPropertyName().equals("dividerLocation"))
			{
				if (mainHorizRulerPanel != null)
					mainHorizRulerPanel.repaint();
			}
		}
	}

	/**
	 * Method to display simulation data in an existing waveform window; ignores preferences.
	 * @param sd the simulation data to display.
	 * @param ww the waveform window to load.
	 * If null, create a new waveform window.
	 */
	public static void refreshSimulationData(Stimuli sd, WaveformWindow ww) {
		// if the window already exists, update the data
        ww.setSimData(sd);
    }

	/**
	 * Method to display simulation data in a new waveform window.
	 * @param sd the simulation data to display.
	 * @param ww the waveform window to load.
	 * If null, create a new waveform window.
	 */
    public static void showSimulationDataInNewWindow(Stimuli sd) {
        WaveformWindow ww = null;
		Iterator<Analysis> anIt = sd.getAnalyses();
		if (!anIt.hasNext())
		{
			System.out.println("ERROR: No simulation data found: waveform window not shown");
			return;
		}
		Analysis an = anIt.next();

		// create a waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		ww = (WaveformWindow)wf.getContent();

		// if the data has an associated cell, see if that cell remembers the signals that were in the waveform window
		if (sd.getCell() != null)
		{
			String [] signalNames = WaveformWindow.getSignalOrder(sd.getCell());
			boolean showedSomething = false;
			boolean wantUnlockedTime = false;
			Analysis.AnalysisType onlyType = null;
			for(int i=0; i<signalNames.length; i++)
			{
				String signalName = signalNames[i];
				Signal xAxisSignal = null;
				int start = 0;
				if (signalName.startsWith("\t"))
				{
					// has panel type and X axis information
					int openPos = signalName.indexOf('(');
					int tabPos = signalName.indexOf('\t', 1);
					start = tabPos+1;
					if (openPos >= 0) tabPos = openPos;
					String analysisName = signalName.substring(1, tabPos);
                    /*
					Analysis.AnalysisType analysisType = Analysis.AnalysisType.findAnalysisType(analysisName);
					if (analysisType == null) continue;
					an = sd.findAnalysis(analysisType);
					if (an == null) continue;
                    */
                        /*
					if (openPos >= 0)
					{
						int closePos = signalName.indexOf(')');
						String sigName = signalName.substring(openPos+1, closePos);
						xAxisSignal = an.findSignalForNetwork(sigName);
						wantUnlockedTime = true;
					}
                        */
				}
				if (onlyType == null) onlyType = an.getAnalysisType();
                /*
				if (an.getAnalysisType() != onlyType) wantUnlockedTime = true;
                */
				Panel wp = null;
				boolean firstSignal = true;

				// add signals to the panel
				for(;;)
				{
					int tabPos = signalName.indexOf('\t', start);
					String sigName = null;
					if (tabPos < 0) sigName = signalName.substring(start); else
					{
						sigName = signalName.substring(start, tabPos);
						start = tabPos+1;
					}
					Color sigColor = null;
					int colorPos = sigName.indexOf(" {");
					if (colorPos >= 0)
					{
						String [] colorNames = sigName.substring(colorPos+2).split(",");
						int red = TextUtils.atoi(colorNames[0]);
						int green = TextUtils.atoi(colorNames[1]);
						int blue = TextUtils.atoi(colorNames[2]);
						sigColor = new Color(red, green, blue);
						sigName = sigName.substring(0, colorPos);
					}
					Signal sSig = an.findSignalForNetwork(sigName);
					if (sSig != null)
					{
						if (firstSignal)
						{
							firstSignal = false;
							wp = new Panel(ww, sd.isAnalog());
							if (xAxisSignal != null)
								wp.setXAxisSignal(xAxisSignal);
							wp.makeSelectedPanel(-1, -1);
							showedSomething = true;
						}
						WaveSignal ws = new WaveSignal(wp, sSig);
						if (sigColor != null)
							ws.setColor(sigColor);
					}
					if (tabPos < 0) break;
				}
			}
			if (showedSomething)
			{
				if (wantUnlockedTime)
				{
					ww.togglePanelXAxisLock();
					for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
					{
						Panel panel = it.next();
						panel.makeSelectedPanel(-1, -1);
						ww.fillScreen();
					}
				} else
				{
					ww.fillScreen();
				}
				return;
			}
		}

		if (an == null) // wrong format?
		{
			System.out.println("ERROR: No simulation data found: waveform window not shown");
			return;
		}

		// nothing saved, so show a default set of signals (if it even exists)
		if (sd.isAnalog())
		{
			Panel wp = new Panel(ww, sd.isAnalog());
			wp.makeSelectedPanel(-1, -1);
		} else
		{
			// put all top-level signals in, up to a limit
			int numSignals = 0;
			List<Signal> allSignals = an.getSignals();
			makeBussedSignals((DigitalAnalysis)an);
			for(int i=0; i<allSignals.size(); i++)
			{
				Signal<DigitalSample> sDSig = (Signal<DigitalSample>)allSignals.get(i);
				if (sDSig.getSignalContext() != null) continue;
				if (DigitalAnalysis.isInBus(sDSig)) continue;
				if (sDSig.getSignalName().indexOf('@') >= 0) continue;
				Panel wp = new Panel(ww, sd.isAnalog());
				wp.makeSelectedPanel(-1, -1);
				new WaveSignal(wp, sDSig);
				numSignals++;
				if (numSignals > 15) break;
			}
		}
		ww.getPanel().validate();
		ww.fillScreen();
	}

	private static void makeBussedSignals(DigitalAnalysis an)
	{
		List<Signal<DigitalSample>> signals = an.getSignals();
		for(int i=0; i<signals.size(); i++)
		{
			Signal sSig = signals.get(i);
			int thisBracketPos = sSig.getSignalName().indexOf('[');
			if (thisBracketPos < 0) continue;
			String prefix = sSig.getSignalName().substring(0, thisBracketPos);

			// see how many of the following signals are part of the bus
			int j = i+1;
			for( ; j<signals.size(); j++)
			{
				Signal nextSig = signals.get(j);

				// other signal must have the same root
				int nextBracketPos = nextSig.getSignalName().indexOf('[');
				if (nextBracketPos < 0) break;
				if (thisBracketPos != nextBracketPos) break;
				if (!prefix.equals(nextSig.getSignalName().substring(0, nextBracketPos))) break;

				// other signal must have the same context
				if (sSig.getSignalContext() == null ^ nextSig.getSignalContext() == null) break;
				if (sSig.getSignalContext() != null)
				{
					if (!sSig.getSignalContext().equals(nextSig.getSignalContext())) break;
				}
			}

			// see how many signals are part of the bus
			int numSignals = j - i;
			if (numSignals <= 1) continue;

			// found a bus of signals: create the bus for it
			Signal<DigitalSample> busSig = DigitalSample.createSignal(an, prefix, sSig.getSignalContext());
			an.buildBussedSignalList(busSig);
			for(int k=i; k<j; k++)
			{
				Signal<DigitalSample> subSig = signals.get(k);
				DigitalAnalysis.addToBussedSignalList(busSig, subSig);
			}
			i = j - 1;
		}
	}
	static int getState(Signal<DigitalSample> dsig, int index) {
        DigitalSample ds = dsig.getExactView().getSample(index);
        if (ds.isLogic0()) return Stimuli.LOGIC_LOW;
        if (ds.isLogic1()) return Stimuli.LOGIC_HIGH;
        if (ds.isLogicX()) return Stimuli.LOGIC_X;
        if (ds.isLogicZ()) return Stimuli.LOGIC_Z;
        throw new RuntimeException("ack!");
    }
}
