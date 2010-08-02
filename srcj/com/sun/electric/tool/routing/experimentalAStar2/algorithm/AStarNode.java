/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarNode.java
 * Written by: Christian Harnisch, Ingo Besenfelder, Michael Neumann (Team 3)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalAStar2.algorithm;

public class AStarNode extends AStarNodeBase<AStarNode>
{

  private int horizontalCapacityPathNumber = 0;

  private int verticalCapacityPathNumber = 0;

  public AStarNode(int x, int y, int z)
  {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public AStarNode()
  {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(AStarNode o)
  {
    return this.getTotalCost() - o.getTotalCost();
  }

  public boolean equals(Object o)
  {
    return this.x == ((AStarNode) o).getX() && this.y == ((AStarNode) o).getY() && this.z == ((AStarNode) o).getZ();
  }

  public void setHorizontalCapacityPathNumber(int horizontalCapacityPathNumber)
  {
    this.horizontalCapacityPathNumber = horizontalCapacityPathNumber;
  }

  public int getHorizontalCapacityPathNumber()
  {
    return this.horizontalCapacityPathNumber;
  }

  public void setVerticalCapacityPathNumber(int verticalCapacityPathNumber)
  {
    this.verticalCapacityPathNumber = verticalCapacityPathNumber;
  }

  public int getVerticalCapacityPathNumber()
  {
    return this.verticalCapacityPathNumber;
  }
}
