/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import com.sun.electric.tool.user.User;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class GenFsmJob extends Job
{
    private final File saoFile;
    private final String outFileName;
    private final String[] clockNames =
    {
        "l2clk"
    };

    public static void genFsm(File saoFile, String outFileName)
    {
        new GenFsmJob(saoFile, outFileName).startJob();
    }

    private GenFsmJob(File saoFile, String outFileName)
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
            DesignExt design = new DesignExt(sr.root);
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
                        boolean prParen = false;
                        for (WireExt wire : m.stateWires)
                        {
                            if (prParen)
                            {
                                out.println();
                                out.print("   ");
                            } else
                            {
                                out.print("  (");
                                prParen = true;
                            }
                            out.print("(|" + wire + "| 4vec-" + wire.getWidth() + "-p)");
                        }
                        for (ModInstExt inst : m.insts)
                        {
                            if (inst.proto.hasState)
                            {
                                if (prParen)
                                {
                                    out.println();
                                    out.print("   ");
                                } else
                                {
                                    out.print("  (");
                                    prParen = true;
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
                        boolean prParen = false;
                        Set<Svar<PathExt>> svars = drv.collectVars();
                        for (Svar<PathExt> svar : svars)
                        {
                            if (prParen)
                            {
                                out.println();
                                out.print("   ");
                            } else
                            {
                                out.print("  (");
                                prParen = true;
                            }
                            PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                            out.print("(|" + svar + "| 4vec-" + lw.wire.getWidth() + "-p)");
                        }
                        out.println(")");
                        out.println("  :returns (|" + lhs + "| 4vec-" + lhs.width() + "-p)");
//                        out.print("  (declare (ignore");
//                        for (Svar<PathExt> svar : svars)
//                        {
//                            out.print(" |" + svar + "|");
//                        }
//                        out.print("))");
                        out.print("  (4vec-concat " + lhs.width());
                        printSvex(out, drv.getSvex(), 3);
                        out.println();
                        out.println("   0))");
                    }
                }
            }
        } catch (IOException e)
        {
            return false;
        }
        return true;
    }

    private void printSvex(PrintStream out, Svex<PathExt> top, int indent)
    {
        out.println();
        for (int i = 0; i < indent; i++)
        {
            out.print(' ');
        }
        if (top instanceof SvexQuote)
        {
            SvexQuote<PathExt> sq = (SvexQuote<PathExt>)top;
            out.print(sq.val.isVec2()
                ? ((Vec2)sq.val).getVal()
                : "'(" + sq.val.getUpper() + " . " + sq.val.getLower() + ")");
        } else if (top instanceof SvexVar)
        {
            SvexVar<PathExt> sv = (SvexVar<PathExt>)top;
            out.print("|" + sv.svar + "|");
        } else
        {
            SvexCall<PathExt> sc = (SvexCall<PathExt>)top;
            out.print("(" + sc.fun.applyFn);
            for (Svex<PathExt> arg : sc.getArgs())
            {
                printSvex(out, arg, indent + 1);
            }
            out.print(')');
        }
    }
}
