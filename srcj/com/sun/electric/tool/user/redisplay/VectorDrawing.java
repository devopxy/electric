/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VectorDrawing.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.redisplay;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.LayerVisibility;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.util.concurrent.utils.ElapseTimer;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.math.DBMath;
import com.sun.electric.util.math.GenMath;
import com.sun.electric.util.math.GenMath.MutableDouble;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to do rapid redraw by caching the vector coordinates of all objects.
 */
class VectorDrawing {
	private static final boolean TAKE_STATS = false;
	private static final boolean DEBUGIMAGES = false;
	private static final int MAXGREEKSIZE = 25;
	private static final int SCALE_SH = 20;

	/** the rendering object */
	private PixelDrawing offscreen;
	/** the window scale */
	private float scale;
	/** the window scale */
	private float scale_;
	/** the window scale and pan factor */
	private float factorX, factorY;
	private int factorX_, factorY_;
	private int scale_int;
	/** true if "peeking" and expanding to the bottom */
	private boolean fullInstantiate;
	/** A List of NodeInsts to the cell being in-place edited. */
	private List<NodeInst> inPlaceNodePath;
	/** The current cell being in-place edited. */
	private Cell inPlaceCurrent;
	/** time that rendering started */
	private ElapseTimer timer = ElapseTimer.createInstance();
	/** true if the user has been told of delays */
	private boolean takingLongTime;
	/** true to stop rendering */
	private boolean stopRendering;
	/** the half-sizes of the window (in pixels) */
	private int szHalfWidth, szHalfHeight;
	/** the screen clipping */
	private int screenLX, screenHX, screenLY, screenHY;
	/** statistics */
	private int boxCount, tinyBoxCount, lineBoxCount, lineCount, polygonCount;
	/** statistics */
	private int crossCount, textCount, circleCount, arcCount;
	/** statistics */
	private int subCellCount, tinySubCellCount;
	/** the threshold of object sizes */
	private float maxObjectSize;
	/** true to use cell greeking images */
	private boolean useCellGreekingImages;
	/** the threshold of text sizes */
	private float maxTextSize;
	/** the maximum cell size above which no greeking */
	private float maxCellSize;

	/** temporary objects (saves allocation) */
	private Point tempPt1 = new Point(), tempPt2 = new Point();
	/** temporary objects (saves allocation) */
	private Point tempPt3 = new Point();
	/** temporary object (saves allocation) */
	private Rectangle tempRect = new Rectangle();

	/** the object that draws the rendered screen */
	private static VectorDrawing topVD;
	/** location for debugging icon displays */
	private static int debugXP, debugYP;

	// ************************************* TOP LEVEL
	// *************************************

	/**
	 * Constructor creates a VectorDrawing object for a given EditWindow.
	 * 
	 * @param wnd
	 *            the EditWindow associated with this VectorDrawing.
	 */
	public VectorDrawing(boolean useCellGreekingImages) {
		this.useCellGreekingImages = useCellGreekingImages;
	}

	/**
	 * Main entry point for drawing a cell.
	 * 
	 * @param offscreen
	 *            offscreen buffer
	 * @param scale
	 *            edit window scale
	 * @param offset
	 *            the offset factor for this window
	 * @param cell
	 *            the cell to draw
	 * @param fullInstantiate
	 *            true to draw all the way to the bottom of the hierarchy.
	 * @param inPlaceNodePath
	 *            a List of NodeInsts to the cell being in-place edited
	 * @param screenLimit
	 *            the area in the cell to display (null to show all).
	 */
	public void render(PixelDrawing offscreen, double scale, Point2D offset, Cell cell, boolean fullInstantiate,
			List<NodeInst> inPlaceNodePath, Cell inPlaceCurrent, Rectangle screenLimit, VarContext context,
			double greekSizeLimit, double greekCellSizeLimit, LayerVisibility lv) {
		// see if any layers are being highlighted/dimmed
		this.offscreen = offscreen;
		offscreen.highlightingLayers = false;
		for (Iterator<Layer> it = Technology.getCurrent().getLayers(); it.hasNext();) {
			Layer layer = it.next();
			if (PixelDrawing.lv.isHighlighted(layer)) {
				offscreen.highlightingLayers = true;
				break;
			}
		}

		// set size limit
		Dimension sz = offscreen.getSize();
		this.scale = (float) scale;
		scale_ = (float) (scale / DBMath.GRID);
		maxObjectSize = (float) greekSizeLimit / this.scale;
		maxTextSize = (float) (maxObjectSize / PixelDrawing.dp.globalTextScale);
		double screenArea = sz.getWidth() / scale * sz.getHeight() / scale;
		maxCellSize = (float) (greekCellSizeLimit * screenArea);

		// statistics
		timer.start();
		long initialUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		takingLongTime = false;
		boxCount = tinyBoxCount = lineBoxCount = lineCount = polygonCount = 0;
		crossCount = textCount = circleCount = arcCount = 0;
		subCellCount = tinySubCellCount = 0;

		// draw recursively
		this.fullInstantiate = fullInstantiate;
		this.inPlaceNodePath = inPlaceNodePath;
		this.inPlaceCurrent = inPlaceCurrent;
		szHalfWidth = sz.width / 2;
		szHalfHeight = sz.height / 2;
		screenLX = 0;
		screenHX = sz.width;
		screenLY = 0;
		screenHY = sz.height;
		factorX = (float) (offset.getX() * DBMath.GRID - szHalfWidth / scale_);
		factorY = (float) (offset.getY() * DBMath.GRID + szHalfHeight / scale_);
		factorX_ = (int) factorX;
		factorY_ = (int) factorY;
		scale_int = (int) (scale_ * (1 << SCALE_SH));
		if (screenLimit != null) {
			screenLX = screenLimit.x;
			if (screenLX < 0)
				screenLX = 0;
			screenHX = screenLimit.x + screenLimit.width;
			if (screenHX >= sz.width)
				screenHX = sz.width - 1;
			screenLY = screenLimit.y;
			if (screenLY < 0)
				screenLY = 0;
			screenHY = screenLimit.y + screenLimit.height;
			if (screenHY >= sz.height)
				screenHY = sz.height - 1;
		}

		// draw the screen, starting with the top cell
		stopRendering = false;
		try {
			VectorCache.VectorCell topVC = drawCell(cell, Orientation.IDENT, context);
			topVD = this;
			render(topVC, 0, 0, context, 0, lv);
			drawList(0, 0, topVC.getTopOnlyShapes(), 0, false);
		} catch (AbortRenderingException e) {
		}
		topVD = null;

		if (takingLongTime) {
			TopLevel.setBusyCursor(false);
			System.out.println("Done");
		}

		if (TAKE_STATS && Job.getDebug()) {
			long curUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long memUsed = curUsed - initialUsed;
			timer.end();
			System.out.println("Time to render: " + timer + "    Memory Used: " + memUsed);
			System.out.println("   Rendered " + boxCount + " boxes (" + tinyBoxCount + " tiny, " + lineBoxCount
					+ " lines), " + lineCount + " lines, " + polygonCount + " polys, " + crossCount + " crosses, "
					+ textCount + " texts, " + circleCount + " circles, " + arcCount + " arcs, " + subCellCount
					+ " subcells (" + tinySubCellCount + " tiny)");
		}
	}

	/**
	 * Main entry point for drawing a tech menu entry.
	 * 
	 * @param offscreen
	 *            offscreen buffer
	 * @param scale
	 *            edit window scale
	 * @param offset
	 *            the offset factor for this window
	 * @param shapes
	 *            shapes of tech menu
	 */
	public void render(PixelDrawing offscreen, double scale, Point2D offset, VectorCache.VectorBase[] shapes) {
		// set colors to use
		PixelDrawing.textGraphics = PixelDrawing.textGraphics.withColor(PixelDrawing.gp
				.getColor(User.ColorPrefType.TEXT));

		// see if any layers are being highlighted/dimmed
		this.offscreen = offscreen;

		// set size limit
		Dimension sz = offscreen.getSize();
		this.scale = (float) scale;
		scale_ = (float) (scale / DBMath.GRID);

		// draw recursively
		szHalfWidth = sz.width / 2;
		szHalfHeight = sz.height / 2;
		screenLX = 0;
		screenHX = sz.width;
		screenLY = 0;
		screenHY = sz.height;
		factorX = (float) (offset.getX() * DBMath.GRID - szHalfWidth / scale_);
		factorY = (float) (offset.getY() * DBMath.GRID + szHalfHeight / scale_);
		factorX_ = (int) factorX;
		factorY_ = (int) factorY;
		scale_int = (int) (scale_ * (1 << SCALE_SH));

		// draw the screen, starting with the top cell
		try {
			List<VectorCache.VectorBase> shapeList = Arrays.asList(shapes);
			drawList(0, 0, shapeList, 0, true);
		} catch (AbortRenderingException e) {
		}
	}

	/**
	 * Class to define a signal to abort rendering.
	 */
	class AbortRenderingException extends Exception {
	}

	/**
	 * Method to request that the current rendering be aborted because it must
	 * be restarted.
	 * 
	 */
	public void abortRendering() {
		stopRendering = true;
	}

	/**
	 * Method to recursively render a cached cell.
	 * 
	 * @param vc
	 *            the cached cell to render
	 * @param oX
	 *            the X offset for rendering the cell (in database grid
	 *            coordinates).
	 * @param oY
	 *            the Y offset for rendering the cell (in database grid
	 *            coordinates).
	 * @param context
	 *            the VarContext for this point in the rendering.
	 * @param level
	 *            : 0=top-level cell in window; 1=low level cell; -1=greeked
	 *            cell.
	 * @param lv
	 *            current layer visibility.
	 */
	private void render(VectorCache.VectorCell vc, int oX, int oY, VarContext context, int level, LayerVisibility lv)
			throws AbortRenderingException {
		// render main list of shapes
		drawList(oX, oY, vc.shapes, level, false);

		// now render subcells
		Cell cell = VectorCache.theCache.database.getCell(vc.vcg.cellId);
		for (VectorCache.VectorSubCell vsc : vc.subCells) {
			if (stopRendering)
				throw new AbortRenderingException();
			// NodeInst ni = cell.getNodeById(vsc.n.nodeId);
			// Cell subCell = (Cell)ni.getProto();
			Cell subCell = cell.getDatabase().getCell((CellId) vsc.n.protoId);
			subCellCount++;

			// get instance location
			int soX = vsc.offsetX + oX;
			int soY = vsc.offsetY + oY;
			VectorCache.VectorCell subVC = VectorCache.theCache.findVectorCell(vsc.subCellId, vc.orient
					.concatenate(vsc.n.orient));
			gridToScreen(subVC.lX + soX, subVC.hY + soY, tempPt1);
			gridToScreen(subVC.hX + soX, subVC.lY + soY, tempPt2);
			int lX = tempPt1.x;
			int lY = tempPt1.y;
			int hX = tempPt2.x;
			int hY = tempPt2.y;

			// see if the subcell is clipped
			if (hX < screenLX || lX >= screenHX)
				continue;
			if (hY < screenLY || lY >= screenHY)
				continue;

			// see if the cell is too tiny to draw
			if (subVC.vcg.cellMinSize < maxObjectSize) {
				Orientation thisOrient = vsc.n.orient;
				Orientation recurseTrans = vc.orient.concatenate(thisOrient);
				VarContext subContext = context.push(cell, vsc.n);
				VectorCache.VectorCell subVC_ = drawCell(subCell, recurseTrans, subContext);
				assert subVC_ == subVC;
				makeGreekedImage(subVC, lv);

				int fadeColor = getFadeColor(subVC, subContext, lv);
				drawTinyBox(lX, hX, lY, hY, fadeColor, subVC);
				tinySubCellCount++;
				continue;
			}

			// see if drawing "down in place"
			boolean onPathDown = false;
			if (inPlaceNodePath != null) {
				for (NodeInst niOnPath : inPlaceNodePath) {
					if (niOnPath.getProto().getId() == vsc.subCellId) {
						onPathDown = true;
						break;
					}
				}
			}

			// see if cell contents should be drawn
			boolean isExpanded = cell.isExpanded(vsc.n.nodeId);
			boolean expanded = isExpanded || fullInstantiate;

			// if not expanded, but viewing this cell in-place, expand it
			if (!expanded && onPathDown)
				expanded = true;

			if (expanded) {
				Orientation thisOrient = vsc.n.orient;
				Orientation recurseTrans = vc.orient.concatenate(thisOrient);
				VarContext subContext = context.push(cell, vsc.n);
				VectorCache.VectorCell subVC_ = drawCell(subCell, recurseTrans, subContext);
				assert subVC_ == subVC;

				// expanded cells may be replaced with greeked versions (not
				// icons)
				if (!subCell.isIcon()) {
					// may also be "tiny" if all features in the cell are tiny
					boolean allFeaturesTiny = subVC.maxFeatureSize > 0 && subVC.maxFeatureSize < maxObjectSize
							&& subVC.vcg.cellArea < maxCellSize
							&& isContentsTiny(subCell, subVC, recurseTrans, context);

					// may also be "tiny" if the cell is smaller than the
					// greeked image
					boolean smallerThanGreek = useCellGreekingImages && hX - lX <= MAXGREEKSIZE
							&& hY - lY <= MAXGREEKSIZE;
					if (allFeaturesTiny || smallerThanGreek) {
						makeGreekedImage(subVC, lv);
						int fadeColor = getFadeColor(subVC, context, lv);
						drawTinyBox(lX, hX, lY, hY, fadeColor, subVC);
						tinySubCellCount++;
						continue;
					}
				}

				int subLevel = level;
				if (subLevel == 0)
					subLevel = 1;
				render(subVC, soX, soY, subContext, subLevel, lv);
			} else {
				// now draw with the proper line type
				int[] op = subVC.outlinePoints;
				int p1x = op[0] + soX;
				int p1y = op[1] + soY;
				int p2x = op[2] + soX;
				int p2y = op[3] + soY;
				int p3x = op[4] + soX;
				int p3y = op[5] + soY;
				int p4x = op[6] + soX;
				int p4y = op[7] + soY;
				gridToScreen(p1x, p1y, tempPt1);
				gridToScreen(p2x, p2y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, PixelDrawing.instanceGraphics, 0, false);
				gridToScreen(p2x, p2y, tempPt1);
				gridToScreen(p3x, p3y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, PixelDrawing.instanceGraphics, 0, false);
				gridToScreen(p3x, p3y, tempPt1);
				gridToScreen(p4x, p4y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, PixelDrawing.instanceGraphics, 0, false);
				gridToScreen(p1x, p1y, tempPt1);
				gridToScreen(p4x, p4y, tempPt2);
				offscreen.drawLine(tempPt1, tempPt2, null, PixelDrawing.instanceGraphics, 0, false);

				// draw the instance name
				if (PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.INSTANCE)) {
					tempRect.setBounds(lX, lY, hX - lX, hY - lY);
					TextDescriptor descript = vsc.n.protoDescriptor;
					offscreen.drawText(tempRect, Poly.Type.TEXTBOX, descript, subCell.describe(false), null,
							PixelDrawing.textGraphics, false);
				}
			}
			if (level == 0 || onPathDown || inPlaceCurrent == cell)
				drawPortList(vsc, subVC, soX, soY, expanded, onPathDown);
		}
	}

	/**
	 * Method to draw a list of cached shapes.
	 * 
	 * @param oX
	 *            the X offset to draw the shapes (in database grid
	 *            coordinates).
	 * @param oY
	 *            the Y offset to draw the shapes (in database grid
	 *            coordinates).
	 * @param shapes
	 *            the List of shapes (VectorBase objects).
	 * @param level
	 *            : 0=top-level cell in window; 1=low level cell; -1=greeked
	 *            cell.
	 * @param forceVisible
	 *            true to force all layers to be drawn (regardless of user
	 *            settings)
	 */
	private void drawList(int oX, int oY, List<VectorCache.VectorBase> shapes, int level, boolean forceVisible)
			throws AbortRenderingException {
		// render all shapes in reverse order (because PixelDrawing don't
		// overwrite opaque layers)
		for (int k = shapes.size() - 1; k >= 0; k--) {
			VectorCache.VectorBase vb = shapes.get(k);
			if (stopRendering)
				throw new AbortRenderingException();

			// get visual characteristics of shape
			Layer layer = vb.layer;
			boolean dimmed = false;
			if (layer != null) {
				if (level < 0) {
					// greeked cells ignore cut and implant layers
					Layer.Function fun = layer.getFunction();
					if (fun.isContact() || fun.isWell() || fun.isSubstrate())
						continue;
				}
				if (!forceVisible) {
					if (!PixelDrawing.lv.isVisible(layer))
						continue;
					dimmed = !PixelDrawing.lv.isHighlighted(layer);
				}
			}
			byte[][] layerBitMap = null;
			EGraphics graphics = vb.graphicsOverride;
			if (graphics == null && layer != null)
				graphics = PixelDrawing.gp.getGraphics(layer);
			if (graphics != null) {
				int layerNum = graphics.getTransparentLayer() - 1;
				if (layerNum < offscreen.numLayerBitMaps)
					layerBitMap = offscreen.getLayerBitMap(layerNum);
			}

			// handle each shape
			if (vb instanceof VectorCache.VectorManhattan) {
				boxCount++;
				VectorCache.VectorManhattan vm = (VectorCache.VectorManhattan) vb;

				double maxSize = maxObjectSize * DBMath.GRID;
				int fadeCol = -1;
				if (layer != null) {
					Layer.Function fun = layer.getFunction();
					if (fun.isImplant() || fun.isSubstrate()) {
						// well and substrate layers are made smaller so that
						// they "greek" sooner
						if (!vm.pureLayer)
							maxSize *= 10;
					} else if (graphics != null) {
						fadeCol = graphics.getRGB();
					}
				}
				for (int i = 0; i < vm.coords.length; i += 4) {
					int c1X = vm.coords[i];
					int c1Y = vm.coords[i + 1];
					int c2X = vm.coords[i + 2];
					int c2Y = vm.coords[i + 3];
					long dX = c2X - c1X;
					long dY = c2Y - c1Y;
					if (dX < maxSize || dY < maxSize) {
						if (fadeCol < 0)
							continue;
						if (dX < maxSize && dY < maxSize) {
							// both dimensions tiny: just draw a dot
							gridToScreen(c1X + oX, c1Y + oY, tempPt1);
							int x = tempPt1.x;
							int y = tempPt1.y;
							if (x < screenLX || x >= screenHX)
								continue;
							if (y < screenLY || y >= screenHY)
								continue;
							offscreen.drawPoint(x, y, null, fadeCol);
							tinyBoxCount++;
						} else {
							// one dimension tiny: draw a line
							gridToScreen(c1X + oX, c2Y + oY, tempPt1);
							gridToScreen(c2X + oX, c1Y + oY, tempPt2);
							assert tempPt1.x <= tempPt2.x && tempPt1.y <= tempPt2.y;
							// if (!(tempPt1.x <= tempPt2.x && tempPt1.y <=
							// tempPt2.y))
							// {
							// System.out.println("Error: too small box?. Check X:"
							// + tempPt1.x + " with " + tempPt2.x);
							// System.out.println("Error: too small box?. Check Y:"
							// + tempPt1.y + " with " + tempPt2.y);
							// }
							int lX = tempPt1.x;
							int hX = tempPt2.x;
							int lY = tempPt1.y;
							int hY = tempPt2.y;
							if (hX < screenLX || lX >= screenHX)
								continue;
							if (hY < screenLY || lY >= screenHY)
								continue;
							drawTinyBox(lX, hX, lY, hY, fadeCol, null);
							lineBoxCount++;
						}
						continue;
					}

					// determine coordinates of rectangle on the screen
					gridToScreen(c1X + oX, c2Y + oY, tempPt1);
					gridToScreen(c2X + oX, c1Y + oY, tempPt2);
					assert tempPt1.x <= tempPt2.x && tempPt1.y <= tempPt2.y;
					int lX = tempPt1.x;
					int hX = tempPt2.x;
					int lY = tempPt1.y;
					int hY = tempPt2.y;

					// reject if completely off the screen
					if (hX < screenLX || lX >= screenHX)
						continue;
					if (hY < screenLY || lY >= screenHY)
						continue;

					// clip to screen
					if (lX < screenLX)
						lX = screenLX;
					if (hX >= screenHX)
						hX = screenHX - 1;
					if (lY < screenLY)
						lY = screenLY;
					if (hY >= screenHY)
						hY = screenHY - 1;

					// draw the box
					offscreen.drawBox(lX, hX, lY, hY, layerBitMap, graphics, dimmed);
				}
			} else if (vb instanceof VectorCache.VectorLine) {
				lineCount++;
				VectorCache.VectorLine vl = (VectorCache.VectorLine) vb;

				// determine coordinates of line on the screen
				gridToScreen(vl.fX + oX, vl.fY + oY, tempPt1);
				gridToScreen(vl.tX + oX, vl.tY + oY, tempPt2);

				// clip and draw the line
				offscreen.drawLine(tempPt1, tempPt2, layerBitMap, graphics, vl.texture, dimmed);
			} else if (vb instanceof VectorCache.VectorPolygon) {
				polygonCount++;
				VectorCache.VectorPolygon vp = (VectorCache.VectorPolygon) vb;
				Point[] intPoints = new Point[vp.points.length];
				for (int i = 0; i < vp.points.length; i++) {
					intPoints[i] = new Point();
					gridToScreen(vp.points[i].x + oX, vp.points[i].y + oY, intPoints[i]);
				}
				Point[] clippedPoints = GenMath.clipPoly(intPoints, screenLX, screenHX - 1, screenLY, screenHY - 1);
				offscreen.drawPolygon(clippedPoints, layerBitMap, graphics, dimmed);
			} else if (vb instanceof VectorCache.VectorCross) {
				crossCount++;
				VectorCache.VectorCross vcr = (VectorCache.VectorCross) vb;
				gridToScreen(vcr.x + oX, vcr.y + oY, tempPt1);
				int size = 5;
				if (vcr.small)
					size = 3;
				offscreen.drawLine(new Point(tempPt1.x - size, tempPt1.y), new Point(tempPt1.x + size, tempPt1.y),
						null, graphics, 0, dimmed);
				offscreen.drawLine(new Point(tempPt1.x, tempPt1.y - size), new Point(tempPt1.x, tempPt1.y + size),
						null, graphics, 0, dimmed);
			} else if (vb instanceof VectorCache.VectorText) {
				VectorCache.VectorText vt = (VectorCache.VectorText) vb;
				switch (vt.textType) {
				case VectorCache.VectorText.TEXTTYPEARC:
					if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.ARC))
						continue;
					break;
				case VectorCache.VectorText.TEXTTYPENODE:
					if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.NODE))
						continue;
					break;
				case VectorCache.VectorText.TEXTTYPECELL:
					if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.CELL))
						continue;
					break;
				case VectorCache.VectorText.TEXTTYPEEXPORT:
					if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.EXPORT))
						continue;
					break;
				case VectorCache.VectorText.TEXTTYPEANNOTATION:
					if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.ANNOTATION))
						continue;
					break;
				case VectorCache.VectorText.TEXTTYPEINSTANCE:
					if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.INSTANCE))
						continue;
					break;
				}
				if (vt.height < maxTextSize)
					continue;

				String drawString = vt.str;
				int lX = vt.bounds.x;
				int lY = vt.bounds.y;
				int hX = lX + vt.bounds.width;
				int hY = lY + vt.bounds.height;
				gridToScreen(lX + oX, hY + oY, tempPt1);
				gridToScreen(hX + oX, lY + oY, tempPt2);
				lX = tempPt1.x;
				lY = tempPt1.y;
				hX = tempPt2.x;
				hY = tempPt2.y;
				// int lX, hX, lY, hY;
				// if (tempPt1.x < tempPt2.x) { lX = tempPt1.x; hX = tempPt2.x;
				// } else
				// { lX = tempPt2.x; hX = tempPt1.x; }
				// if (tempPt1.y < tempPt2.y) { lY = tempPt1.y; hY = tempPt2.y;
				// } else
				// { lY = tempPt2.y; hY = tempPt1.y; }

				// for ports, switch between the different port display methods
				// if (vt.textType == VectorCache.VectorText.TEXTTYPEPORT)
				// {
				// int portDisplayLevel = User.getPortDisplayLevel();
				// Color portColor = vt.e.getBasePort().getPortColor();
				// if (vt.ni.isExpanded()) portColor = textColor;
				// if (portColor != null) portGraphics.setColor(portColor);
				// int cX = (lX + hX) / 2;
				// int cY = (lY + hY) / 2;
				// if (portDisplayLevel == 2)
				// {
				// // draw port as a cross
				// int size = 3;
				// offscreen.drawLine(new Point(cX-size, cY), new Point(cX+size,
				// cY), null, portGraphics, 0, false);
				// offscreen.drawLine(new Point(cX, cY-size), new Point(cX,
				// cY+size), null, portGraphics, 0, false);
				// crossCount++;
				// continue;
				// }
				//
				// // draw port as text
				// if (portDisplayLevel == 1) drawString = vt.e.getShortName();
				// else
				// drawString = vt.e.getName();
				// graphics = portGraphics;
				// layerBitMap = null;
				// lX = hX = cX;
				// lY = hY = cY;
				// } else
				if (vt.textType == VectorCache.VectorText.TEXTTYPEEXPORT && vt.basePort != null) {
					if (!PixelDrawing.lv.isVisible(vt.basePort.getParent()))
						continue;
					graphics = PixelDrawing.textGraphics;
					int exportDisplayLevel = PixelDrawing.gp.exportDisplayLevel;
					if (exportDisplayLevel == 2) {
						// draw export as a cross
						int cX = (lX + hX) / 2;
						int cY = (lY + hY) / 2;
						int size = 3;
						offscreen
								.drawLine(new Point(cX - size, cY), new Point(cX + size, cY), null, graphics, 0, false);
						offscreen
								.drawLine(new Point(cX, cY - size), new Point(cX, cY + size), null, graphics, 0, false);
						crossCount++;
						continue;
					}

					// draw export as text
					if (exportDisplayLevel == 1)
						drawString = Export.getShortName(drawString);
					layerBitMap = null;
				}

				textCount++;
				tempRect.setBounds(lX, lY, hX - lX, hY - lY);
				offscreen.drawText(tempRect, vt.style, vt.descript, drawString, layerBitMap, graphics, dimmed);
			} else if (vb instanceof VectorCache.VectorCircle) {
				circleCount++;
				VectorCache.VectorCircle vci = (VectorCache.VectorCircle) vb;
				gridToScreen(vci.cX + oX, vci.cY + oY, tempPt1);
				gridToScreen(vci.eX + oX, vci.eY + oY, tempPt2);
				switch (vci.nature) {
				case 0:
					offscreen.drawCircle(tempPt1, tempPt2, layerBitMap, graphics, dimmed);
					break;
				case 1:
					offscreen.drawThickCircle(tempPt1, tempPt2, layerBitMap, graphics, dimmed);
					break;
				case 2:
					offscreen.drawDisc(tempPt1, tempPt2, layerBitMap, graphics, dimmed);
					break;
				}
			} else if (vb instanceof VectorCache.VectorCircleArc) {
				arcCount++;
				VectorCache.VectorCircleArc vca = (VectorCache.VectorCircleArc) vb;
				gridToScreen(vca.cX + oX, vca.cY + oY, tempPt1);
				gridToScreen(vca.eX1 + oX, vca.eY1 + oY, tempPt2);
				gridToScreen(vca.eX2 + oX, vca.eY2 + oY, tempPt3);
				offscreen
						.drawCircleArc(tempPt1, tempPt2, tempPt3, vca.thick, vca.bigArc, layerBitMap, graphics, dimmed);
			}
		}
	}

	/**
	 * Method to draw a list of cached port shapes.
	 * 
	 * @param oX
	 *            the X offset to draw the shapes (in database grid
	 *            coordinates).
	 * @param oY
	 *            the Y offset to draw the shapes (in database grid
	 *            coordinates).
	 * @param expanded
	 *            true to draw a list on expanded instance.
	 * @param onPathDown
	 *            true if this level of hierarchy is the current one in
	 *            "down-in-place" editing.
	 */
	private void drawPortList(VectorCache.VectorSubCell vsc, VectorCache.VectorCell subVC_, int oX, int oY,
			boolean expanded, boolean onPathDown) throws AbortRenderingException {
		if (!PixelDrawing.gp.isTextVisibilityOn(TextDescriptor.TextType.PORT))
			return;
		// render all shapes
		List<VectorCache.VectorCellExport> portShapes = subVC_.vcg.getPortShapes();
		int[] portCenters = subVC_.getPortCenters();
		assert portShapes.size() * 2 == portCenters.length;
		for (int i = 0; i < portShapes.size(); i++) {
			VectorCache.VectorCellExport vce = portShapes.get(i);
			if (stopRendering)
				throw new AbortRenderingException();

			// get visual characteristics of shape
			if (!onPathDown && vsc.shownPorts.get(vce.getChronIndex()))
				continue;
			if (vce.height < maxTextSize)
				continue;

			int cX = portCenters[i * 2];
			int cY = portCenters[i * 2 + 1];
			gridToScreen(cX + oX, cY + oY, tempPt1);
			cX = tempPt1.x;
			cY = tempPt1.y;

			int portDisplayLevel = PixelDrawing.gp.portDisplayLevel;
			EGraphics portGraphics = expanded ? PixelDrawing.textGraphics : offscreen
					.getPortGraphics(vce.getBasePort());
			if (portDisplayLevel == 2) {
				// draw port as a cross
				int size = 3;
				offscreen.drawLine(new Point(cX - size, cY), new Point(cX + size, cY), null, portGraphics, 0, false);
				offscreen.drawLine(new Point(cX, cY - size), new Point(cX, cY + size), null, portGraphics, 0, false);
				crossCount++;
				continue;
			}

			// draw port as text
			boolean shortName = portDisplayLevel == 1;
			String drawString = vce.getName(shortName);

			textCount++;
			tempRect.setBounds(cX, cY, 0, 0);
			offscreen.drawText(tempRect, vce.style, vce.descript, drawString, null, portGraphics, false);
		}
	}

	/**
	 * Method to convert a database grid coordinate to screen coordinates.
	 * 
	 * @param dbX
	 *            the X coordinate (in database grid units).
	 * @param dbY
	 *            the Y coordinate (in database grid units).
	 * @param result
	 *            the Point in which to store the screen coordinates.
	 */
	private void gridToScreen(int dbX, int dbY, Point result) {
		if (false) {
			result.x = ((dbX - factorX_) * scale_int) >> SCALE_SH;
			result.y = ((factorY_ - dbY) * scale_int) >> SCALE_SH;
		} else {
			double scrX = (dbX - factorX) * scale_;
			double scrY = (factorY - dbY) * scale_;
			result.x = (int) (scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
			result.y = (int) (scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
		}
	}

	/**
	 * Method to draw a tiny box on the screen in a given color. Done when the
	 * object is too small to draw in full detail.
	 * 
	 * @param lX
	 *            the low X coordinate of the box.
	 * @param hX
	 *            the high X coordinate of the box.
	 * @param lY
	 *            the low Y coordinate of the box.
	 * @param hY
	 *            the high Y coordinate of the box.
	 * @param col
	 *            the color to draw.
	 */
	private void drawTinyBox(int lX, int hX, int lY, int hY, int col, VectorCache.VectorCell greekedCell) {
		if (lX < screenLX)
			lX = screenLX;
		if (hX >= screenHX)
			hX = screenHX - 1;
		if (lY < screenLY)
			lY = screenLY;
		if (hY >= screenHY)
			hY = screenHY - 1;
		if (useCellGreekingImages) {
			if (greekedCell != null && greekedCell.fadeImageColors != null) {
				int backgroundColor = PixelDrawing.gp.getColor(User.ColorPrefType.BACKGROUND).getRGB();
				int backgroundRed = (backgroundColor >> 16) & 0xFF;
				int backgroundGreen = (backgroundColor >> 8) & 0xFF;
				int backgroundBlue = backgroundColor & 0xFF;

				// render the icon properly with scale
				int greekWid = greekedCell.fadeImageWid;
				int greekHei = greekedCell.fadeImageHei;
				int wid = hX - lX;
				int hei = hY - lY;
				float xInc = greekWid / (float) wid;
				float yInc = greekHei / (float) hei;
				float yPos = 0;
				for (int y = 0; y < hei; y++) {
					float yEndPos = yPos + yInc;
					int yS = (int) yPos;
					int yE = (int) yEndPos;

					float xPos = 0;
					for (int x = 0; x < wid; x++) {
						float xEndPos = xPos + xInc;
						int xS = (int) xPos;
						int xE = (int) xEndPos;

						float r = 0, g = 0, b = 0;
						float totalArea = 0;
						for (int yGrab = yS; yGrab <= yE; yGrab++) {
							if (yGrab >= greekHei)
								continue;
							float yArea = 1;
							if (yGrab == yS)
								yArea = (1 - (yPos - yS));
							if (yGrab == yE)
								yArea *= (yEndPos - yE);

							for (int xGrab = xS; xGrab <= xE; xGrab++) {
								if (xGrab >= greekWid)
									continue;
								int index = xGrab + yGrab * greekedCell.fadeImageWid;
								if (greekedCell.fadeImageColors == null || index >= greekedCell.fadeImageColors.length)
									continue;
								int value = greekedCell.fadeImageColors[index];
								int red = (value >> 16) & 0xFF;
								int green = (value >> 8) & 0xFF;
								int blue = value & 0xFF;
								float area = yArea;
								if (xGrab == xS)
									area *= (1 - (xPos - xS));
								if (xGrab == xE)
									area *= (xEndPos - xE);
								if (area <= 0)
									continue;
								r += red * area;
								g += green * area;
								b += blue * area;
								totalArea += area;
							}
						}
						if (totalArea > 0) {
							int red = (int) (r / totalArea);
							if (red > 255)
								red = 255;
							int green = (int) (g / totalArea);
							if (green > 255)
								green = 255;
							int blue = (int) (b / totalArea);
							if (blue > 255)
								blue = 255;
							if (Math.abs(backgroundRed - red) > 2 || Math.abs(backgroundGreen - green) > 2
									|| Math.abs(backgroundBlue - blue) > 2) {
								offscreen.drawPoint(lX + x, lY + y, null, (red << 16) | (green << 8) | blue);
							}
						}
						xPos = xEndPos;
					}
					yPos = yEndPos;
				}
				if (DEBUGIMAGES) {
					for (int y = 0; y < greekedCell.fadeImageHei; y++) {
						for (int x = 0; x < greekedCell.fadeImageWid; x++) {
							int valToSet = greekedCell.fadeImageColors[x + y * greekedCell.fadeImageWid];
							topVD.offscreen.drawPoint(greekedCell.fadeOffsetX + x + 1, greekedCell.fadeOffsetY + y + 1,
									null, valToSet);
						}
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX, greekedCell.fadeOffsetY + y + 1, null, 0);
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX + greekedCell.fadeImageWid + 1,
								greekedCell.fadeOffsetY + y + 1, null, 0);
					}
					for (int x = 0; x < greekedCell.fadeImageWid; x++) {
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX + x, greekedCell.fadeOffsetY, null, 0);
						topVD.offscreen.drawPoint(greekedCell.fadeOffsetX + x, greekedCell.fadeOffsetY
								+ greekedCell.fadeImageHei + 1, null, 0);
					}
				}
				return;
			}
		}

		// no greeked image: just use the greeked color
		for (int y = lY; y <= hY; y++) {
			for (int x = lX; x <= hX; x++)
				offscreen.drawPoint(x, y, null, col);
		}
	}

	/**
	 * Method to determine whether a cell has tiny contents. Recursively
	 * examines the cache of this and all subcells to see if the maximum feature
	 * sizes are all below the global threshold "maxObjectSize".
	 * 
	 * @param cell
	 *            the Cell in question.
	 * @param vc
	 *            the cached representation of the cell.
	 * @param trans
	 *            the Orientation of the cell.
	 * @return true if the cell has all tiny contents.
	 */
	private boolean isContentsTiny(Cell cell, VectorCache.VectorCell vc, Orientation trans, VarContext context)
			throws AbortRenderingException {
		if (vc.maxFeatureSize > maxObjectSize)
			return false;
		for (VectorCache.VectorSubCell vsc : vc.subCells) {
			boolean isExpanded = cell.isExpanded(vsc.n.nodeId);
			// NodeInst ni = cell.getNodeById(vsc.n.nodeId);
			VectorCache.VectorCell subVC = VectorCache.theCache.findVectorCell(vsc.subCellId, vc.orient
					.concatenate(vsc.n.orient));
			if (isExpanded || fullInstantiate) {
				Orientation thisOrient = vsc.n.orient;
				Orientation recurseTrans = trans.concatenate(thisOrient);
				VarContext subContext = context.push(cell, vsc.n);
				Cell subCell = VectorCache.theCache.database.getCell(vsc.subCellId);
				VectorCache.VectorCell subVC_ = drawCell(subCell, recurseTrans, subContext);
				assert subVC_ == subVC;
				boolean subCellTiny = isContentsTiny(subCell, subVC, recurseTrans, subContext);
				if (!subCellTiny)
					return false;
				continue;
			}
			if (subVC.vcg.cellMinSize > maxObjectSize)
				return false;
		}
		return true;
	}

	private void makeGreekedImage(VectorCache.VectorCell subVC, LayerVisibility lv) throws AbortRenderingException {
		if (subVC.fadeImage)
			return;
		if (!useCellGreekingImages)
			return;

		// determine size and scale of greeked cell image
		Rectangle2D cellBounds = subVC.vcg.bounds;
		Rectangle2D ownBounds = new Rectangle2D.Double(cellBounds.getMinX(), cellBounds.getMinY(), cellBounds
				.getWidth(), cellBounds.getHeight());
		AffineTransform trans = subVC.orient.rotateAbout(0, 0);
		DBMath.transformRect(ownBounds, trans);
		double greekScale = MAXGREEKSIZE / ownBounds.getHeight();
		if (ownBounds.getWidth() > ownBounds.getHeight())
			greekScale = MAXGREEKSIZE / ownBounds.getWidth();
		int lX = (int) Math.floor(cellBounds.getMinX() * greekScale);
		int hX = (int) Math.ceil(cellBounds.getMaxX() * greekScale);
		int lY = (int) Math.floor(cellBounds.getMinY() * greekScale);
		int hY = (int) Math.ceil(cellBounds.getMaxY() * greekScale);
		if (hX <= lX)
			hX = lX + 1;
		int greekWid = hX - lX;
		if (hY <= lY)
			hY = lY + 1;
		int greekHei = hY - lY;
		Rectangle screenBounds = new Rectangle(lX, lY, greekWid, greekHei);

		// construct the offscreen buffers for the greeked cell image
		PixelDrawing offscreen = new PixelDrawing(greekScale, screenBounds);
		Point2D cellCtr = new Point2D.Double(ownBounds.getCenterX(), ownBounds.getCenterY());
		VectorDrawing subVD = new VectorDrawing(useCellGreekingImages);

		subVC.fadeOffsetX = debugXP;
		subVC.fadeOffsetY = debugYP;
		debugXP += MAXGREEKSIZE + 5;
		if (topVD != null) {
			if (debugXP + MAXGREEKSIZE + 2 >= topVD.offscreen.getSize().width) {
				debugXP = 0;
				debugYP += MAXGREEKSIZE + 5;
			}
		}

		// set rendering information for the greeked cell image
		subVD.offscreen = offscreen;
		subVD.screenLX = 0;
		subVD.screenHX = greekWid;
		subVD.screenLY = 0;
		subVD.screenHY = greekHei;
		subVD.szHalfWidth = greekWid / 2;
		subVD.szHalfHeight = greekHei / 2;
		subVD.maxObjectSize = 0;
		subVD.maxTextSize = 0;
		subVD.scale = (float) greekScale;
		subVD.scale_ = (float) (greekScale / DBMath.GRID);
		subVD.factorX = (float) (cellCtr.getX() * DBMath.GRID - subVD.szHalfWidth / subVD.scale_);
		subVD.factorY = (float) (cellCtr.getY() * DBMath.GRID + subVD.szHalfHeight / subVD.scale_);
		subVD.factorX_ = (int) subVD.factorX;
		subVD.factorY_ = (int) subVD.factorY;
		subVD.scale_int = (int) (subVD.scale_ * (1 << SCALE_SH));
		subVD.fullInstantiate = true;
		subVD.takingLongTime = true;

		// render the greeked cell
		subVD.offscreen.clearImage(null, null);
		subVD.render(subVC, 0, 0, VarContext.globalContext, -1, lv);
		subVD.offscreen.composite(null);

		// remember the greeked cell image
		int[] img = offscreen.getOpaqueData();
		subVC.fadeImageWid = greekWid;
		subVC.fadeImageHei = greekHei;
		subVC.fadeImageColors = new int[subVC.fadeImageWid * subVC.fadeImageHei];
		int i = 0;
		for (int y = 0; y < subVC.fadeImageHei; y++) {
			for (int x = 0; x < subVC.fadeImageWid; x++) {
				int value = img[i];
				subVC.fadeImageColors[i++] = value & 0xFFFFFF;
			}
		}
		subVC.fadeImage = true;
	}

	/**
	 * Method to determine the "fade" color for a cached cell. Fading is done
	 * when the cell is too tiny to draw (or all of its contents are too tiny).
	 * Instead of drawing the cell contents, the entire cell is painted with the
	 * "fade" color.
	 * 
	 * @param vc
	 *            the cached cell.
	 * @return the fade color (an integer with red/green/blue).
	 */
	private int getFadeColor(VectorCache.VectorCell vc, VarContext context, LayerVisibility lv)
			throws AbortRenderingException {
		if (vc.hasFadeColor)
			return vc.fadeColor;

		// examine all shapes
		Map<Layer, MutableDouble> layerAreas = new HashMap<Layer, MutableDouble>();
		gatherContents(vc, layerAreas, context);

		// now compute the color
		Set<Layer> keys = layerAreas.keySet();
		double totalArea = 0;
		for (Layer layer : keys) {
			if (!lv.isVisible(layer))
				continue;
			MutableDouble md = layerAreas.get(layer);
			totalArea += md.doubleValue();
		}
		double r = 0, g = 0, b = 0;
		if (totalArea == 0) {
			// no fade color, make it the background color
			vc.fadeColor = PixelDrawing.gp.getColor(User.ColorPrefType.BACKGROUND).getRGB();
		} else {
			for (Layer layer : keys) {
				if (!lv.isVisible(layer))
					continue;
				MutableDouble md = layerAreas.get(layer);
				double portion = md.doubleValue() / totalArea;
				EGraphics desc = PixelDrawing.gp.getGraphics(layer);
				Color col = desc.getColor();
				r += col.getRed() * portion;
				g += col.getGreen() * portion;
				b += col.getBlue() * portion;
			}
			if (r < 0)
				r = 0;
			if (r > 255)
				r = 255;
			if (g < 0)
				g = 0;
			if (g > 255)
				g = 255;
			if (b < 0)
				b = 0;
			if (b > 255)
				b = 255;
			vc.fadeColor = (((int) r) << 16) | (((int) g) << 8) | (int) b;
		}
		vc.hasFadeColor = true;
		return vc.fadeColor;
	}

	/**
	 * Helper method to recursively examine a cached cell and its subcells and
	 * compute the coverage of each layer.
	 * 
	 * @param vc
	 *            the cached cell to examine.
	 * @param layerAreas
	 *            a HashMap of all layers and the areas they cover.
	 */
	private void gatherContents(VectorCache.VectorCell vc, Map<Layer, MutableDouble> layerAreas, VarContext context)
			throws AbortRenderingException {
		for (VectorCache.VectorBase vb : vc.shapes) {
			Layer layer = vb.layer;
			if (layer == null)
				continue;
			Layer.Function fun = layer.getFunction();
			if (fun.isImplant() || fun.isSubstrate())
				continue;

			// handle each shape
			double area = 0;
			if (vb instanceof VectorCache.VectorManhattan) {
				VectorCache.VectorManhattan vm = (VectorCache.VectorManhattan) vb;
				for (int i = 0; i < vm.coords.length; i += 4) {
					double c1X = vm.coords[i];
					double c1Y = vm.coords[i + 1];
					double c2X = vm.coords[i + 2];
					double c2Y = vm.coords[i + 3];
					area += (c1X - c2X) * (c1Y - c2Y);
				}
			} else if (vb instanceof VectorCache.VectorPolygon) {
				VectorCache.VectorPolygon vp = (VectorCache.VectorPolygon) vb;
				area = GenMath.getAreaOfPoints(vp.points);
			} else if (vb instanceof VectorCache.VectorCircle) {
				VectorCache.VectorCircle vci = (VectorCache.VectorCircle) vb;
				double radius = new Point2D.Double(vci.cX, vci.cY).distance(new Point2D.Double(vci.eX, vci.eY));
				area = radius * radius * Math.PI;
			}
			if (area == 0)
				continue;
			MutableDouble md = layerAreas.get(layer);
			if (md == null) {
				md = new MutableDouble(0);
				layerAreas.put(layer, md);
			}
			md.setValue(md.doubleValue() + area);
		}

		Cell cell = VectorCache.theCache.database.getCell(vc.vcg.cellId);
		for (VectorCache.VectorSubCell vsc : vc.subCells) {
			VectorCache.VectorCellGroup vcg = VectorCache.theCache.findCellGroup(vsc.subCellId);
			VectorCache.VectorCell subVC = vcg.getAnyCell();
			VarContext subContext = context.push(cell, vsc.n);
			if (subVC == null) {
				// NodeInst ni = cell.getNodeById(vsc.n.nodeId);
				Cell nodeProto = cell.getDatabase().getCell((CellId) vsc.n.protoId);
				subVC = drawCell(nodeProto, Orientation.IDENT, subContext);
			}
			gatherContents(subVC, layerAreas, subContext);
		}
	}

	// ************************************* CACHE CREATION
	// *************************************

	/**
	 * Method to cache the contents of a cell.
	 * 
	 * @param cell
	 *            the Cell to cache
	 * @param prevTrans
	 *            the orientation of the cell (just a rotation, no offsets
	 *            here).
	 * @return a cached cell object for the given Cell.
	 */
	private VectorCache.VectorCell drawCell(Cell cell, Orientation prevTrans, VarContext context)
			throws AbortRenderingException {
		// caching the cell: check for abort and delay reporting
		if (stopRendering)
			throw new AbortRenderingException();
		if (!takingLongTime) {
			long currentTime = System.currentTimeMillis();
			if (timer.currentTimeLong() > 1000) {
				System.out.print("Display caching, please wait...");
				TopLevel.setBusyCursor(true);
				takingLongTime = true;
			}
		}

		return VectorCache.theCache.drawCell(cell.getId(), prevTrans, context, scale);
	}
}
