/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DualEval.java
 * Input/output tool: ACL2 DualEval output
 *
 * Copyright (c) 2004, Static Free Software. All rights reserved.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.IconNodeInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.IOTool;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Write ACL2 .lisp file with DualEval Network
 */
public class DualEval extends Output
{
    private List<Cell> downTop = new ArrayList<>();

    private DualEvalPreferences localPrefs;

    public static class DualEvalPreferences extends OutputPreferences
    {
        // DXF Settings
        int dxfScale = IOTool.getDXFScale();
        public Technology tech;

        public DualEvalPreferences(boolean factory)
        {
            super(factory);
            tech = Technology.getCurrent();
        }

        @Override
        public Output doOutput(Cell cell, VarContext context, String filePath)
        {
            DualEval out = new DualEval(this);
            if (out.openTextOutputStream(filePath))
                return out.finishWrite();

            out.writeDualEval(cell);

            if (out.closeTextOutputStream())
                return out.finishWrite();
            System.out.println(filePath + " written");
            return out.finishWrite();
        }
    }

    /**
     * Creates a new instance of the DualEval netlister.
     */
    DualEval(DualEval.DualEvalPreferences dp)
    {
        localPrefs = dp;
    }

    private void writeDualEval(Cell cell)
    {
        enumDownTop(cell, new HashSet<>());
        for (Cell c : downTop)
        {
            printWriter.println(c);
        }
//		printWriter.print("  0\nENDSEC\n");
//		printWriter.print("  0\nSECTION\n");
//		printWriter.print("  2\nENTITIES\n");
//		writeDXFCell(cell, false);
//		printWriter.print("  0\nENDSEC\n");
//		printWriter.print("  0\nEOF\n");
    }

    private void enumDownTop(Cell topIcon, Set<Cell> visitedIcons)
    {
        if (visitedIcons.contains(topIcon))
        {
            return;
        }
        visitedIcons.add(topIcon);
        Cell schem = topIcon.getCellGroup().getMainSchematics();
        if (schem != null)
        {
            for (Iterator<NodeInst> it = schem.getNodes(); it.hasNext();)
            {
                NodeInst ni = it.next();

                if (ni instanceof IconNodeInst && !ni.isIconOfParent())
                {
                    enumDownTop((Cell)ni.getProto(), visitedIcons);
                }
            }
        }
        downTop.add(topIcon);
    }
}
