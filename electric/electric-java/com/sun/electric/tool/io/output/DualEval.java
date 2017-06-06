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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.IconNodeInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.IOTool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Write ACL2 .lisp file with DualEval Network
 */
public class DualEval extends Output
{
    private final List<Cell> downTop = new ArrayList<>();
    private final Map<String, String> primNames = new HashMap<>();

    
    {
        primNames.put("inv", "b-not");
        primNames.put("invn", "b-not");
        primNames.put("nand2", "b-nand");
        primNames.put("nor2", "b-nor");
        primNames.put("xor2", "b-xor");
    }

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
        String prevName = null;
        for (Cell c : downTop)
        {
            String name = c.getName();
            List<IconNodeInst> nodes = new ArrayList<>();
            for (Iterator<NodeInst> it = c.getNodes(); it.hasNext();)
            {
                NodeInst ni = it.next();
                if (ni instanceof IconNodeInst && !ni.isIconOfParent())
                {
                    nodes.add((IconNodeInst)ni);
                }
            }
            Map<IconNodeInst, Set<IconNodeInst>> graph = makeDepGraph(nodes);
            Collection<IconNodeInst> sortedNodes = toposort(nodes, graph);
            printWriter.println();
            printWriter.println("(defconst *" + name + "*");
            printWriter.println("  (cons");
            printWriter.println("     '(" + name);
            writeExports(c, PortCharacteristic.IN);
            writeExports(c, PortCharacteristic.OUT);
            printWriter.println("       ()");
            writeNodes(sortedNodes);
            printWriter.println("       )");
            printWriter.println("     " + (prevName != null ? "*" + prevName + "*" : "nil") + "))");
            prevName = name;
        }
    }

    private void writeExports(Cell cell, PortCharacteristic pc)
    {
        printWriter.print("       (");
        boolean first = true;
        for (Iterator<Export> it = cell.getExports(); it.hasNext();)
        {
            Export export = it.next();

            if (export.getCharacteristic() == pc)
            {
                if (first)
                {
                    first = false;
                } else
                {
                    printWriter.print(" ");
                }
                printWriter.print(export.getName());
            }
        }
        printWriter.println(")");
    }

    private Map<IconNodeInst, Set<IconNodeInst>> makeDepGraph(List<IconNodeInst> nodes)
    {
        Map<IconNodeInst, Set<IconNodeInst>> graph = new LinkedHashMap<>();
        for (IconNodeInst ni : nodes)
        {
            Netlist netlist = ni.getParent().getNetlist();
            for (Iterator<Export> it = ni.getProto().getExports(); it.hasNext();)
            {
                Export e = it.next();
                if (e.getCharacteristic() == PortCharacteristic.OUT)
                {
                    PortInst pi = ni.findPortInstFromProto(e);
                    Network net = netlist.getNetwork(pi);
                    for (PortInst pi2 : net.getPortsList())
                    {
                        if (!pi.equals(pi2) && pi2.getNodeInst() instanceof IconNodeInst)
                        {
                            IconNodeInst ni2 = (IconNodeInst)pi2.getNodeInst();
                            Export e2 = (Export)pi2.getPortProto();
                            if (e2.getCharacteristic() != PortCharacteristic.IN)
                            {
                                System.out.println("Multiple drivers of " + net);
                            }
                            Set<IconNodeInst> dep = graph.get(ni2);
                            if (dep == null)
                            {
                                dep = new LinkedHashSet<>();
                                graph.put(ni2, dep);
                            }
                            dep.add(ni);
                        }
                    }
                }
            }
        }
        return graph;
    }

    private Collection<IconNodeInst> toposort(List<IconNodeInst> nodes, Map<IconNodeInst, Set<IconNodeInst>> graph)
    {
        Set<IconNodeInst> visited = new HashSet<>();
        Set<IconNodeInst> sort = new LinkedHashSet<>();
        for (IconNodeInst ni : nodes)
        {
            toposort(ni, graph, visited, sort);
        }
        return sort;
    }

    private void toposort(IconNodeInst top, Map<IconNodeInst, Set<IconNodeInst>> graph, Set<IconNodeInst> visited, Set<IconNodeInst> sort)
    {
        if (sort.contains(top))
        {
            return;
        }
        if (visited.contains(top))
        {
            System.out.println("Combinational loop at " + top);
            return;
        }
        visited.add(top);
        Set<IconNodeInst> deps = graph.get(top);
        if (deps != null)
        {
            for (IconNodeInst dep : deps)
            {
                toposort(dep, graph, visited, sort);
            }
        }
        sort.add(top);
    }

    private void writeNodes(Collection<IconNodeInst> nodes)
    {
        printWriter.println("       (");
        for (IconNodeInst node : nodes)
        {
            printWriter.print("        (" + node.getName());
            String protoName = node.getProto().getName();
            if (primNames.containsKey(protoName))
            {
                protoName = primNames.get(protoName);
            }
            printWriter.println(" " + portListStr(node, PortCharacteristic.OUT)
                + " " + protoName + " "
                + portListStr(node, PortCharacteristic.IN)
                + ")");
        }
        printWriter.println("       )");
    }

    private String portListStr(IconNodeInst ni, PortCharacteristic pc)
    {
        StringBuilder sb = new StringBuilder();
        Netlist netlist = ni.getParent().getNetlist();
        sb.append("(");
        boolean first = true;
        for (Iterator<Export> it = ni.getProto().getExports(); it.hasNext();)
        {
            Export e = it.next();
            if (e.getCharacteristic() == pc)
            {
                PortInst pi = ni.findPortInstFromProto(e);
                Network net = netlist.getNetwork(pi);
                if (first)
                {
                    first = false;
                } else
                {
                    sb.append(" ");
                }
                sb.append(net.getName());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private void enumDownTop(Cell topIcon, Set<Cell> visitedIcons)
    {
        if (visitedIcons.contains(topIcon))
        {
            return;
        }
        String funName = primNames.get(topIcon.getName());
        visitedIcons.add(topIcon);
        if (funName != null)
        {
            return;
        }
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
        downTop.add(schem != null ? schem : topIcon);
    }
}
