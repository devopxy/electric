/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EFIDO.java
 * Digital Filter technology
 * Generated automatically from a library
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
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;

import java.awt.Color;

/**
 * This is the Digital Filter Technology.
 */
public class EFIDO extends Technology
{
	// -------------------- private and protected methods ------------------------
	public EFIDO(Generic generic, TechFactory techFactory)
	{
		super(generic, techFactory);
		setTechDesc("Digital Filters");
		setFactoryScale(10000, false);   // in nanometers: really 10 microns
		setNoNegatedArcs();
		setStaticTechnology();

		setFactoryTransparentLayers(new Color []
		{
			new Color(255,  0,  0), // layer 1
			new Color(  0,  0,255), // layer 2
			new Color(  0,155, 80), // layer 3
			new Color(  0,  0,  0), // layer 4
			new Color(  0,  0,  0), // layer 5
		});

		//**************************************** LAYERS ****************************************

		/** N layer */
		Layer N_lay = Layer.newInstance(this, "Node",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1, 255,  0,  0,/*0,155,80,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** A layer */
		Layer A_lay = Layer.newInstance(this, "Arc",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2,   0,  0,255,/*0,0,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** O layer */
		Layer O_lay = Layer.newInstance(this, "Outpad",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3,   0,155, 80,/*0,0,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		N_lay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);	// Node
		A_lay.setFunction(Layer.Function.UNKNOWN);						// Arc
		O_lay.setFunction(Layer.Function.OVERGLASS);					// Outpad

		//******************** ARCS ********************

		/** wire arc */
		ArcProto wire_arc = newArcProto("wire", 0, 0, ArcProto.Function.METAL1,
			new Technology.ArcLayer(A_lay, 0, Poly.Type.CLOSED)
		);
		wire_arc.setFactoryFixedAngle(true);
		wire_arc.setFactoryAngleIncrement(45);

		/** bus arc */
		ArcProto bus_arc = newArcProto("bus", 0, 2, ArcProto.Function.BUS,
			new Technology.ArcLayer(A_lay, 2, Poly.Type.CLOSED)
		);
		bus_arc.setFactoryFixedAngle(true);
		bus_arc.setFactoryAngleIncrement(45);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.25, 0), new EdgeV(0.25, 0)),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.05, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.05, 0), new EdgeV(0.25, 0)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.05, 0), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.05, 0), new EdgeV(-0.25, 0)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.25, 0), EdgeV.makeCenter()),
			new Technology.TechPoint(new EdgeH(0.25, 0), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.25, 0), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(0.25, 0)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.2, 0), new EdgeV(-0.05, 0)),
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(-0.1, 0), new EdgeV(-0.3, 0)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(0.4, 0), new EdgeV(0.1, 0)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
			new Technology.TechPoint(new EdgeH(0.4, 0), new EdgeV(-0.1, 0)),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(0.3, 0), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), new EdgeV(0.25, 0)),
			new Technology.TechPoint(EdgeH.makeCenter(), new EdgeV(-0.25, 0)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.25, 0), new EdgeV(0.25, 0)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.25, 0), new EdgeV(-0.25, 0)),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(0.25, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(-0.25, 0)),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(0.4, 0), new EdgeV(0.1, 0)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
			new Technology.TechPoint(new EdgeH(0.4, 0), new EdgeV(-0.1, 0)),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(0.3, 0), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(new EdgeH(0.25, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(new EdgeH(-0.25, 0), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.25, 0), new EdgeV(-0.25, 0))
		};
		Technology.TechPoint [] box_17 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_18 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** wire_pin */
		PrimitiveNode wp_node = PrimitiveNode.newInstance("wire_pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(A_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_17)
			});
		wp_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, wp_node, new ArcProto [] {wire_arc}, "wire", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		wp_node.setFunction(PrimitiveNode.Function.PIN);
		wp_node.setWipeOn1or2();
		wp_node.setSquare();

		/** bus_pin */
		PrimitiveNode bp_node = PrimitiveNode.newInstance("bus_pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_18)
			});
		bp_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bp_node, new ArcProto [] {bus_arc}, "bus", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		bp_node.setFunction(PrimitiveNode.Function.PIN);
		bp_node.setArcsWipe();
		bp_node.setArcsShrink();

		/** adder */
		PrimitiveNode a_node = PrimitiveNode.newInstance("adder", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_16),
				new Technology.NodeLayer(N_lay, 5, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_15),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_17),
				new Technology.NodeLayer(N_lay, 5, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_14)
			});
		a_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {wire_arc}, "in1", 270,15, 4, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {wire_arc}, "in2", 225,15, 0, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0)),
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {wire_arc}, "in3", 180,15, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {wire_arc}, "in4", 135,15, 2, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0)),
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {wire_arc}, "in5", 90,15, 3, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {wire_arc}, "out", 0,45, 5, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		a_node.setFunction(PrimitiveNode.Function.UNKNOWN);
		a_node.setSquare();

		/** multiplier */
		PrimitiveNode m_node = PrimitiveNode.newInstance("multiplier", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_13),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_12),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_17),
				new Technology.NodeLayer(N_lay, 5, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_15),
				new Technology.NodeLayer(N_lay, 5, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_14)
			});
		m_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, m_node, new ArcProto [] {wire_arc}, "in1", 270,15, 4, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, m_node, new ArcProto [] {wire_arc}, "in2", 225,15, 0, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0)),
				PrimitivePort.newInstance(this, m_node, new ArcProto [] {wire_arc}, "in3", 180,15, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, m_node, new ArcProto [] {wire_arc}, "in4", 135,15, 2, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0)),
				PrimitivePort.newInstance(this, m_node, new ArcProto [] {wire_arc}, "in5", 90,15, 3, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, m_node, new ArcProto [] {wire_arc}, "out", 0,45, 5, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		m_node.setFunction(PrimitiveNode.Function.UNKNOWN);
		m_node.setSquare();

		/** timedelay */
		PrimitiveNode t_node = PrimitiveNode.newInstance("timedelay", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_11),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_10),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_9),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_18),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_8)
			});
		t_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {wire_arc}, "in", 180,15, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {wire_arc}, "out", 0,45, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		t_node.setFunction(PrimitiveNode.Function.UNKNOWN);

		/** multiplexer */
		PrimitiveNode m0_node = PrimitiveNode.newInstance("multiplexer", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_7),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_6),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_5),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_18)
			});
		m0_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, m0_node, new ArcProto [] {wire_arc}, "in1", 270,15, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), new EdgeV(0.25, 0), EdgeH.makeLeftEdge(), new EdgeV(0.25, 0)),
				PrimitivePort.newInstance(this, m0_node, new ArcProto [] {wire_arc}, "in2", 135,15, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), new EdgeV(-0.25, 0), EdgeH.makeLeftEdge(), new EdgeV(-0.25, 0)),
				PrimitivePort.newInstance(this, m0_node, new ArcProto [] {wire_arc}, "out", 0,45, 2, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		m0_node.setFunction(PrimitiveNode.Function.UNKNOWN);

		/** subtractor */
		PrimitiveNode s_node = PrimitiveNode.newInstance("subtractor", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_4),
				new Technology.NodeLayer(N_lay, 5, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_15),
				new Technology.NodeLayer(N_lay, 5, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_14),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_17)
			});
		s_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {wire_arc}, "in1", 270,15, 4, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {wire_arc}, "in2", 225,15, 0, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0)),
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {wire_arc}, "in3", 180,15, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {wire_arc}, "in4", 135,15, 2, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0)),
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {wire_arc}, "in5", 90,15, 3, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {wire_arc}, "out", 0,45, 5, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		s_node.setFunction(PrimitiveNode.Function.UNKNOWN);
		s_node.setSquare();

		/** divider */
		PrimitiveNode d_node = PrimitiveNode.newInstance("divider", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_3),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_4),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2),
				new Technology.NodeLayer(N_lay, 4, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_14),
				new Technology.NodeLayer(N_lay, 4, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_15),
				new Technology.NodeLayer(N_lay, -1, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_17)
			});
		d_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, d_node, new ArcProto [] {wire_arc}, "in1", 270,15, 4, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, d_node, new ArcProto [] {wire_arc}, "in2", 225,15, 0, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(0.3535534, 0)),
				PrimitivePort.newInstance(this, d_node, new ArcProto [] {wire_arc}, "in3", 180,15, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, d_node, new ArcProto [] {wire_arc}, "in4", 135,15, 2, PortCharacteristic.UNKNOWN,
					new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0), new EdgeH(-0.3535534, 0), new EdgeV(-0.3535534, 0)),
				PrimitivePort.newInstance(this, d_node, new ArcProto [] {wire_arc}, "in5", 90,15, 3, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, d_node, new ArcProto [] {wire_arc}, "out", 0,45, 5, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		d_node.setFunction(PrimitiveNode.Function.UNKNOWN);
		d_node.setSquare();

		/** padin */
		PrimitiveNode p_node = PrimitiveNode.newInstance("padin", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_1),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_18)
			});
		p_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {wire_arc}, "out", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		p_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** padout */
		PrimitiveNode p0_node = PrimitiveNode.newInstance("padout", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(O_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_1),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_18)
			});
		p0_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, p0_node, new ArcProto [] {wire_arc}, "in", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		p0_node.setFunction(PrimitiveNode.Function.CONNECT);

        // Building information for palette
        loadFactoryMenuPalette(EFIDO.class.getResource("efidoMenu.xml"));

        //Foundry
        newFoundry(Foundry.Type.NONE, null);
	};
}
