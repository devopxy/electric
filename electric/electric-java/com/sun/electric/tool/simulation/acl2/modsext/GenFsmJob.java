/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenFsmJob.java
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
package com.sun.electric.tool.simulation.acl2.modsext;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.SvexVar;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Concat;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class GenFsmJob extends Job
{
    private final File saoFile;
    private final String designName;
    private final String outFileName;
    private final String[] clockNames =
    {
        "l2clk"
    };

    public static void genFsm(File saoFile, String designName, String outFileName)
    {
        new GenFsmJob(saoFile, designName, outFileName).startJob();
    }

    private GenFsmJob(File saoFile, String designName, String outFileName)
    {
        super("Dump SV Design", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
        this.saoFile = saoFile;
        this.designName = designName;
        this.outFileName = outFileName;
    }

    @Override
    public boolean doIt() throws JobException
    {
        try
        {
            ACL2Reader sr = new ACL2Reader(saoFile);
            DesignExt design = new DesignExt(sr.root);
            printPhaseStates(design);
            ModuleExt topModule = design.downTop.get(design.getTop());
            String clockName = null;
            for (String c : clockNames)
            {
                Name name = new Name(ACL2Object.valueOf(c));
                if (topModule.wiresIndex.containsKey(name))
                {
                    clockName = c;
                    break;
                }
            }
            design.computeCombinationalInputs(clockName);
            try (PrintStream out = new PrintStream(outFileName))
            {
                out.println("(in-package \"SV\")");
                out.println("(include-book \"centaur/fty/top\" :dir :system)");
                out.println("(include-book \"4vec-nnn\")");
                out.println();
                out.println("(set-rewrite-stack-limit 10000)");
                for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
                {
                    ModName modName = e.getKey();
                    ModuleExt m = e.getValue();
                    out.println();
                    out.println("; " + modName);
                    if (m.hasState)
                    {
                        out.println();
                        out.println("(defprod |" + modName + "-phase-st|");
                        out.print("  (");
                        boolean first = true;
                        for (WireExt wire : m.stateWires)
                        {
                            if (first)
                            {
                                first = false;
                            } else
                            {
                                out.println();
                                out.print("   ");
                            }
                            out.print("(|" + wire + "| 4vec-" + wire.getWidth() + ")");
                        }
                        for (ModInstExt inst : m.insts)
                        {
                            if (inst.proto.hasState)
                            {
                                if (first)
                                {
                                    first = false;
                                } else
                                {
                                    out.println();
                                    out.print("   ");
                                }
                                out.print("(|" + inst.getInstname() + "| |" + inst.proto.modName + "-phase-st|)");
                            }
                        }
                        out.println("))");
                    }
                    for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : m.assigns.entrySet())
                    {
                        Lhs<PathExt> lhs = e1.getKey();
                        DriverExt drv = e1.getValue();
                        out.println();
                        out.println("(define |" + modName + "-" + lhs + "-loc|");
                        out.print("  (");
                        boolean firstArg = true;
                        Set<Svar<PathExt>> svars = drv.collectVars();
                        for (Svar<PathExt> svar : svars)
                        {
                            if (firstArg)
                            {
                                firstArg = false;
                            } else
                            {
                                {
                                    out.println();
                                    out.print("   ");
                                }
                            }
                            PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                            out.print("(|" + svar + "| 4vec-" + lw.wire.getWidth() + "-p)");
                        }
                        out.println(")");
                        out.print("  :returns (|" + lhs + "| 4vec-" + lhs.width() + "-p)");
//                        out.print("  (declare (ignore");
//                        for (Svar<PathExt> svar : svars)
//                        {
//                            out.print(" |" + svar + "|");
//                        }
//                        out.print("))");
                        printSvex(out, drv.getSvex(), lhs.width());
                        if (firstArg)
                        {
                            out.println("(in-theory (disable (|" + modName + "-" + lhs + "-loc|)))");
                        }
                    }
                }
            }
        } catch (IOException e)
        {
            return false;
        }
        return true;
    }

    private void printPhaseStates(DesignExt design) throws IOException
    {
        File outFile = new File(outFileName);
        File outDir = outFile.getParentFile();
        File statesFile = new File(outDir, designName + "-phase-states.lisp");

        try (PrintStream out = new PrintStream(statesFile))
        {
            out.println("(in-package \"SV\")");
            out.println("(include-book \"centaur/fty/top\" :dir :system)");
            out.println("(include-book \"4vec-nnn\")");
            out.println();
            out.println("(set-rewrite-stack-limit 10000)");
            for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
            {
                ModName modName = e.getKey();
                ModuleExt m = e.getValue();
                if (m.hasState)
                {
                    out.println();
                    out.println("; " + modName);
                    printPhaseState(out, modName, m);
                }
            }
        }
    }

    private void printPhaseState(PrintStream out, ModName modName, ModuleExt m)
    {
        out.println();
        out.println("(defprod |" + modName + "-phase-st|");
        out.print("  (");
        boolean first = true;
        for (WireExt wire : m.stateWires)
        {
            if (first)
            {
                first = false;
            } else
            {
                out.println();
                out.print("   ");
            }
            out.print("(|" + wire + "| 4vec-" + wire.getWidth() + ")");
        }
        for (ModInstExt inst : m.insts)
        {
            if (inst.proto.hasState)
            {
                if (first)
                {
                    first = false;
                } else
                {
                    out.println();
                    out.print("   ");
                }
                out.print("(|" + inst.getInstname() + "| |" + inst.proto.modName + "-phase-st|)");
            }
        }
        out.println(")");
        out.println("  :layout :fulltree)");
    }

    private void printSvex(PrintStream out, Svex<PathExt> top, int width)
    {
        top = SvexCall.newCall(Vec4Concat.FUNCTION,
            new SvexQuote<>(new Vec2(width)),
            top,
            new SvexQuote<>(Vec2.ZERO));
        Set<SvexCall<PathExt>> multirefs = top.multirefs();
        int indent = 2;
        Map<Svex<PathExt>, String> multirefNames = Collections.emptyMap();
        if (!multirefs.isEmpty())
        {
            out.println(" ;; MULTIREFS " + multirefs.size());
            out.print("  (let* (");
            multirefNames = new HashMap<>();
            Svex<PathExt>[] toposort = top.toposort();
//            for (int i = 0; i < toposort.length; i++)
            for (int i = toposort.length - 1; i >= 0; i--)
            {
                Svex<PathExt> svex = toposort[i];
                if (svex instanceof SvexCall && multirefs.contains((SvexCall<PathExt>)svex))
                {
                    String name = "temp" + multirefNames.size();
                    out.println();
                    out.print("   (" + name);
                    printSvex(out, svex, multirefNames, 4);
                    out.print(')');
                    multirefNames.put(svex, name);
                }
            }
            out.print(')');
            indent = 4;
        }
        printSvex(out, top, multirefNames, indent);
        out.println(multirefs.isEmpty() ? ")" : "))");
    }

    private void printSvex(PrintStream out, Svex<PathExt> top, Map<Svex<PathExt>, String> multirefsNames, int indent)
    {
        out.println();
        for (int i = 0; i < indent; i++)
        {
            out.print(' ');
        }
        if (top instanceof SvexQuote)
        {
            SvexQuote<PathExt> sq = (SvexQuote<PathExt>)top;
            if (sq.val.isVec2())
            {
                Vec2 val = (Vec2)sq.val;
                out.print(val.getVal());
            } else if (sq.val.equals(Vec4.X))
            {
                out.print("(4vec-x)");
            } else if (sq.val.equals(Vec4.Z))
            {
                out.print("(4vec-z)");
            } else
            {
                out.print("'(" + sq.val.getUpper() + " . " + sq.val.getLower() + ")");
            }
        } else if (top instanceof SvexVar)
        {
            SvexVar<PathExt> sv = (SvexVar<PathExt>)top;
            out.print("|" + sv.svar + "|");
        } else
        {
            String name = multirefsNames.get(top);
            if (name != null)
            {
                out.print(name);
            } else
            {
                SvexCall<PathExt> sc = (SvexCall<PathExt>)top;
                out.print("(" + sc.fun.applyFn);
                for (Svex<PathExt> arg : sc.getArgs())
                {
                    printSvex(out, arg, multirefsNames, indent + 1);
                }
                out.print(')');
            }
        }
    }
}
