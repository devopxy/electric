/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2DesignJobs.java
 *
 * Copyright (c) 2017, Static Free Software. All rights reserved.
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
package com.sun.electric.tool.simulation.acl2.mods;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.File;
import java.io.IOException;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dump serialized file with SVEX design
 */
public class ACL2DesignJobs
{
    public static void dump(File saoFile, String outFileName)
    {
        new DumpDesignJob(saoFile, outFileName).startJob();
    }

    private static class DumpDesignJob extends Job
    {
        private final File saoFile;
        private final String outFileName;
        private final String clkName = "l2clk";

        private DumpDesignJob(File saoFile, String outFileName)
        {
            super("Dump SV Design", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.saoFile = saoFile;
            this.outFileName = outFileName;
        }

        @Override
        public boolean doIt() throws JobException
        {
            try
            {
                ACL2Reader sr = new ACL2Reader(saoFile);
                Design design = new Design(sr.root);
                try (PrintStream out = new PrintStream(outFileName))
                {
                    int totalUseCount = 0;
                    for (Map.Entry<ModName, Module> e : design.downTop.entrySet())
                    {
                        ModName nm = e.getKey();
                        Module m = e.getValue();
                        Map<SVarExt, Set<SVarExt>> graph0 = evalAssigns(m, clkName, Vec2.ZERO);
                        Map<SVarExt, Set<SVarExt>> graph1 = evalAssigns(m, clkName, Vec2.ONE);
//                        Map<SVarExt, Set<SVarExt>> closure0 = closure(graph0);
//                        Map<SVarExt, Set<SVarExt>> closure1 = closure(graph1);
                        
                        out.println("module " + nm + " has "
                            + m.wires.size() + " wires "
                            + m.insts.size() + " insts "
                            + m.assigns.size() + " assigns "
                            + m.aliaspairs.size() + " aliaspairs "
                            + m.useCount + " useCount");
                        totalUseCount += m.useCount;
                        out.println(" wires");
                        for (Wire w : m.wires)
                        {
                            if (w.isAssigned())
                            {
                                out.print(w.used ? "  out    " : "  output ");
                                if (w.assignedBits != null && !BigIntegerUtil.logheadMask(w.width).equals(w.assignedBits))
                                {
                                    out.print("#x" + w.getAssignedBits().toString(16));
                                }
                            } else
                            {
                                Util.check(w.getAssignedBits().signum() == 0);
                                out.print(w.used ? "  input  " : "  unused ");
                            }
                            out.print(w.isGlobal() ? "! " : w.exported ? "* " : "  ");
                            out.println(w);
                                SVarExt svar = m.newVar(w.name.impl);
                            out.println(" | 0 => " + graph0.get(svar) + " | 1 => " + graph1.get(svar));
//                            out.println(" | 0 => " + closure0.get(svar) + " | 1 => " + closure1.get(svar));
                        }
                        out.println(" insts");
                        for (ModInst mi : m.insts)
                        {
                            out.println("  " + mi);
                        }
                        out.println(" assigns");
                        for (Map.Entry<Lhs, Driver> e1 : m.assigns.entrySet())
                        {
                            Lhs l = e1.getKey();
                            Driver d = e1.getValue();
                            assert !l.ranges.isEmpty();
                            for (int i = 0; i < l.ranges.size(); i++)
                            {
                                Lhrange lr = l.ranges.get(i);
                                Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
                                SVarExt svar = atomVar.name;
                                assert svar.getDelay() == 0;
                                assert !svar.isNonblocking();
                                out.print((i == 0 ? "  " : ",") + lr);
                            }

                            out.print(" = " + d);
                            Set<Svar> deps = d.svex.collectVars();
                            Set<SVarExt.LocalWire> dep0 = getDeps(d.svex, m, clkName, Vec2.ZERO);
                            Set<SVarExt.LocalWire> dep1 = getDeps(d.svex, m, clkName, Vec2.ONE);
                            if (!dep0.equals(deps) || !dep1.equals(deps))
                            {
                                if (dep0.equals(dep1))
                                {
                                    out.print(" | => " + dep0);
                                } else
                                {
                                    out.print(" | 0 => " + dep0 + " | 1 => " + dep1);
                                }
                            }
                            out.println();
                        }
                        out.println(" aliaspairs");
                        for (Map.Entry<Lhs, Lhs> e1 : m.aliaspairs.entrySet())
                        {
                            Lhs l = e1.getKey();
                            Lhs r = e1.getValue();
                            assert l.ranges.size() == 1;
                            Lhrange lr = l.ranges.get(0);
                            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
                            assert atomVar.rsh == 0;
                            SVarExt svar = atomVar.name;
                            assert svar.getDelay() == 0;
                            assert !svar.isNonblocking();
                            out.print("  " + lr + " <->");
                            for (Lhrange lr1 : r.ranges)
                            {
                                atomVar = (Lhatom.Var)lr1.atom;
                                svar = atomVar.name;
                                assert svar.getDelay() == 0;
                                assert !svar.isNonblocking();
                                out.print(" " + lr1);
                            }
                            out.println();
                        }
                    }
                    out.println("totalUseCount=" + totalUseCount);
                    out.println("design.top=" + design.top);
                }
            } catch (IOException e)
            {
                return false;
            }
            return true;
        }
/*
        private Map<SVarExt, Set<SVarExt>> closure(Map<SVarExt, Set<SVarExt>> rel)
        {
            Map<SVarExt, Set<SVarExt>> closure = new HashMap<>();
            Set<SVarExt> visited = new HashSet<>();
            for (SVarExt svar: rel.keySet())
            {
                closure(svar, rel, closure, visited);
            }
            Util.check(closure.size() == visited.size());
            return closure;
        }
        
        private Set<SVarExt> closure(SVarExt top,
            Map<SVarExt, Set<SVarExt>> rel,
            Map<SVarExt, Set<SVarExt>> closure,
            Set<SVarExt> visited)
        {
            Set<SVarExt> ret = closure.get(top);
            if (ret == null)
            {
                boolean ok = visited.add(top);
                if (!ok)
                {
                    System.out.println("CombinationalLoop!!! in wire " + top);
                }
                Set<SVarExt> dep = rel.get(top);
                if (dep == null)
                {
                    ret = Collections.singleton(top);
                } else {
                    ret = new LinkedHashSet<>();
                    for (SVarExt svar: dep)
                    {
                        ret.addAll(closure(svar, rel, closure, visited));
                    }
                }
                closure.put(top, ret);
            }
            return ret;
        }
*/        
        private Map<SVarExt, Set<SVarExt>> evalAssigns(Module m, String clkName, Vec2 val)
        {
            Map<SVarExt, Set<SVarExt>> graph = new HashMap<>();
            evalAssigns(m, clkName, val, graph);
            return graph;
        }

        private void evalAssigns(Module m, String clkName, Vec2 val, Map<SVarExt, Set<SVarExt>> graph)
        {
            for (Map.Entry<Lhs, Driver> e1 : m.assigns.entrySet())
            {
                Lhs l = e1.getKey();
                Driver d = e1.getValue();
                Set<SVarExt.LocalWire> dDeps = getDeps(d.svex, m, clkName, val);
                assert !l.ranges.isEmpty();
                for (int i = 0; i < l.ranges.size(); i++)
                {
                    Lhrange lr = l.ranges.get(i);
                    Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
                    SVarExt svar = atomVar.name;
                    assert svar.getDelay() == 0;
                    assert !svar.isNonblocking();
                    Set<SVarExt> decendants = graph.get(svar);
                    if (decendants == null)
                    {
                        decendants = new LinkedHashSet<>();
                        graph.put(svar, decendants);
                    }
                    decendants.addAll(dDeps);
                }
            }
        }

        private Set<SVarExt.LocalWire> getDeps(Svex svex, Module m, String clkName, Vec2 val)
        {
            Map<Svar, Vec2> env = new HashMap<>();
            for (Wire w : m.wires)
            {
                if (w.isGlobal() && w.global.equals(clkName))
                {
                    SVarExt svar = m.newVar(w.name.impl);
                    env.put(svar, val);
                }
            }

            Set<SVarExt.LocalWire> dep = svex.collectVars(SVarExt.LocalWire.class, env);
            for (Iterator<SVarExt.LocalWire> it = dep.iterator(); it.hasNext();)
            {
                SVarExt.LocalWire svar = it.next();
                if (svar.getDelay() != 0)
                {
                    it.remove();
                }
            }
            return dep;
        }

        private String showDeps(Svex svex, Module m, String clkName)
        {
            if (clkName == null)
            {
                return "";
            }
            Set<SVarExt.LocalWire> dep0 = getDeps(svex, m, clkName, Vec2.ZERO);
            Set<SVarExt.LocalWire> dep1 = getDeps(svex, m, clkName, Vec2.ONE);
            return " | 0 => " + dep0 + " | 1 => " + dep1;
        }
    }

    public static void genAlu(File saoFile, String outFileName)
    {
        new GenFsmJob<>(Alu.class, saoFile, outFileName).startJob();
    }

    public static class GenFsmJob<T extends GenFsm> extends Job
    {
        private final Class<T> cls;
        private final File saoFile;
        private final String outFileName;

        public GenFsmJob(Class<T> cls, File saoFile, String outFileName)
        {
            super("Gen Fsm in ACL2", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.cls = cls;
            this.saoFile = saoFile;
            this.outFileName = outFileName;
        }

        @Override
        public boolean doIt() throws JobException
        {
            try
            {
                GenFsm gen = cls.newInstance();
                gen.gen(saoFile, outFileName);
            } catch (InstantiationException | IllegalAccessException | IOException e)
            {
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        }
    }

    private static class Alu extends GenFsm
    {
        private static String[] inputs =
        {
            "opcode",
            "abus",
            "bbus"
        };

        @Override
        protected boolean ignore_wire(Wire w)
        {
            String s = w.name.impl.stringValueExact();
            for (String is : inputs)
            {
                if (is.equals(s))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected boolean isFlipFlopIn(String modname, String wireName)
        {
            return modname.startsWith("flop$width=")
                && wireName.equals("d");
        }

        @Override
        protected boolean isFlipFlopOut(String modname, String wireName)
        {
            return modname.startsWith("flop$width=")
                && wireName.equals("q");
        }

        public Alu()
        {
            super("alu", "alu16");
        }
        /*
Used Svex functions:
SV::BITNOT
SV::CONCAT
SV::BITAND
SV::RSH
SV::?
SV::ZEROX
SV::==
SV::PARTSEL
COMMON-LISP::+
COMMON-LISP::*
COMMON-LISP::<
SV::BITXOR
SV::BITOR
SV::B-
         */
    }
}
