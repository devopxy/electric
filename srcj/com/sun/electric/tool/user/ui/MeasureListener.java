/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MeasureListener.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to make measurements in a window.
 */
public class MeasureListener implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public static MeasureListener theOne = new MeasureListener();

	private static double lastMeasuredDistanceX = 0, lastMeasuredDistanceY = 0;
	private static double lastValidMeasuredDistanceX = 0, lastValidMeasuredDistanceY = 0;
	private static boolean measuring = false; // true if drawing measure line
	private static List<Highlight> lastHighlights = new ArrayList<Highlight>();
	private Point2D dbStart; // start of measure in database units

	private MeasureListener() {}

	public static Dimension2D getLastMeasuredDistance()
	{
		Dimension2D dim = new Dimension2D.Double(lastValidMeasuredDistanceX,
			lastValidMeasuredDistanceY);
		return dim;
	}

	private void startMeasure(Point2D dbStart)
	{
		lastValidMeasuredDistanceX = lastMeasuredDistanceX;
		lastValidMeasuredDistanceY = lastMeasuredDistanceY;
		this.dbStart = dbStart;
		measuring = true;
		lastHighlights.clear();
	}

	private void dragOutMeasure(EditWindow wnd, Point2D dbPoint)
	{
		if (measuring && dbStart != null)
		{
			// Highlight.clear();
			Point2D start = dbStart;
			Point2D end = dbPoint;
			Highlighter highlighter = wnd.getRulerHighlighter();

			for (Highlight h : lastHighlights)
			{
				highlighter.remove(h);
			}
			lastHighlights.clear();

			// show coords at start and end point
            Cell cell = wnd.getCell();
            if (cell == null)
            {
//                System.out.println("No cell available for measure");
                return; // nothing available
            }

            Technology tech = cell.getTechnology();
			lastHighlights.add(highlighter.addMessage(cell, "("
				+ TextUtils.formatDistance(start.getX(), tech) + "," + TextUtils.formatDistance(start.getY(), tech)
				+ ")", start));
			lastHighlights.add(highlighter.addMessage(cell, "("
				+ TextUtils.formatDistance(end.getX(), tech) + "," + TextUtils.formatDistance(end.getY(), tech)
				+ ")", end));
			// add in line
			lastHighlights.add(highlighter.addLine(start, end, cell));

			lastMeasuredDistanceX = Math.abs(start.getX() - end.getX());
			lastMeasuredDistanceY = Math.abs(start.getY() - end.getY());
			Point2D center = new Point2D.Double((start.getX() + end.getX()) / 2,
				(start.getY() + end.getY()) / 2);
			double dist = start.distance(end);
			String show = TextUtils.formatDistance(dist, tech) + " (dX="
				+ TextUtils.formatDistance(lastMeasuredDistanceX, tech) + " dY="
				+ TextUtils.formatDistance(lastMeasuredDistanceY, tech) + ")";
			lastHighlights.add(highlighter.addMessage(cell, show, center, 1));
			highlighter.finished();
			wnd.clearDoingAreaDrag();
			wnd.repaint();
		}
	}

	public void reset()
    {
        if (measuring) measuring = false;

        // clear measurements in the current window
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf.getContent() instanceof EditWindow)
        {
            EditWindow wnd = (EditWindow)wf.getContent();
	        Highlighter highlighter = wnd.getRulerHighlighter();
	        highlighter.clear();
	        highlighter.finished();
	        wnd.repaint();
        } else if (wf.getContent() instanceof WaveformWindow)
        {
        	WaveformWindow ww = (WaveformWindow)wf.getContent();
        	for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
        	{
        		Panel p = it.next();
        		p.clearMeasurements();
        	}
        }
    }

	private void finishMeasure(EditWindow wnd)
	{
		Highlighter highlighter = wnd.getRulerHighlighter();
		if (measuring)
		{
			for (Highlight h : lastHighlights)
			{
				highlighter.remove(h);
			}
			lastHighlights.clear();
			measuring = false;
		} else
		{
			// clear measures from the screen if user cancels twice in a row
			highlighter.clear();
		}
		highlighter.finished();
		wnd.repaint();
	}

	// ------------------------ Mouse Listener Stuff -------------------------

	public void mousePressed(MouseEvent evt)
	{
		boolean ctrl = (evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;

		if (evt.getSource() instanceof EditWindow)
		{
			int newX = evt.getX();
			int newY = evt.getY();
			EditWindow wnd = (EditWindow)evt.getSource();
			Point2D dbMouse = wnd.screenToDatabase(newX, newY);
			EditWindow.gridAlign(dbMouse);
			if (isLeftMouse(evt))
			{
				if (measuring && ctrl && dbStart != null)
				{
					// orthogonal only
					dbMouse = convertToOrthogonal(dbStart, dbMouse);
				}
				startMeasure(dbMouse);
			}
			if (ClickZoomWireListener.isRightMouse(evt))
			{
				finishMeasure(wnd);
			}
		}
	}

	public void mouseDragged(MouseEvent evt)
	{
		boolean ctrl = (evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;

		if (evt.getSource() instanceof EditWindow)
		{
			int newX = evt.getX();
			int newY = evt.getY();
			EditWindow wnd = (EditWindow)evt.getSource();
			Point2D dbMouse = wnd.screenToDatabase(newX, newY);
			if (ctrl && dbStart != null)
			{
				dbMouse = convertToOrthogonal(dbStart, dbMouse);
			}
			EditWindow.gridAlign(dbMouse);
			dragOutMeasure(wnd, dbMouse);
		}
	}

	public void mouseMoved(MouseEvent evt)
	{
		mouseDragged(evt);
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}
	public void mouseWheelMoved(MouseWheelEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();

		if (evt.getSource() instanceof EditWindow)
		{
			EditWindow wnd = (EditWindow)evt.getSource();

			if (chr == KeyEvent.VK_ESCAPE)
			{
				finishMeasure(wnd);
			}
		}
	}

	// mac stuff
	private static final boolean isMac = Client.isOSMac();

	/**
	 * See if event is a left mouse click. Platform independent.
	 */
	private boolean isLeftMouse(MouseEvent evt)
	{
		if (isMac)
		{
			if (!evt.isMetaDown())
			{
				if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) return true;
			}
		} else
		{
			if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) return true;
		}
		return false;
	}

	/**
	 * Convert the mousePoint to be orthogonal to the startPoint. Chooses
	 * direction which is orthogonally farther from startPoint
	 * @param startPoint the reference point
	 * @param mousePoint the mouse point
	 * @return a new point orthogonal to startPoint
	 */
	public static Point2D convertToOrthogonal(Point2D startPoint, Point2D mousePoint)
	{
		// move in direction that is farther
		double xdist, ydist;
		xdist = Math.abs(mousePoint.getX() - startPoint.getX());
		ydist = Math.abs(mousePoint.getY() - startPoint.getY());
		if (ydist > xdist)
			return new Point2D.Double(startPoint.getX(), mousePoint.getY());
		return new Point2D.Double(mousePoint.getX(), startPoint.getY());
	}

}
