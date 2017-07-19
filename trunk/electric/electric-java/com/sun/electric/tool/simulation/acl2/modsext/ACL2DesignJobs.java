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
package com.sun.electric.tool.simulation.acl2.modsext;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.simulation.acl2.mods.Address;
import com.sun.electric.tool.simulation.acl2.mods.Design;
import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModDb;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Util;
import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.SvexVar;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.user.User;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import com.sun.electric.util.acl2.ACL2Writer;
import java.io.File;
import java.io.IOException;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dump serialized file with SVEX design
 */
public class ACL2DesignJobs
{
    private static final boolean VERBOSE_DUMP = false;

    public static void dump(File saoFile, String outFileName, List<String> specialOutputs)
    {
        new DumpDesignJob(saoFile, outFileName, specialOutputs).startJob();
    }

    private static class DumpDesignJob extends Job
    {
        private final File saoFile;
        private final String outFileName;
        private final String[] clockNames =
        {
            "l2clk"//, "clk"
        };
        private final List<String> specialOutputs;

        private DumpDesignJob(File saoFile, String outFileName, List<String> specialOutputs)
        {
            super("Dump SV Design", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.saoFile = saoFile;
            this.outFileName = outFileName;
            this.specialOutputs = specialOutputs;
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
                for (String specialOutput : specialOutputs)
                {
                    int indexOfDot = specialOutputs.indexOf('.');
                    String modStr = specialOutput.substring(0, indexOfDot);
                    String outputStr = specialOutput.substring(indexOfDot + 1);
                    ModuleExt specModule = design.downTop.get(ModName.valueOf(ACL2Object.valueOf(modStr)));
                    if (specModule != null)
                    {
                        WireExt specWire = specModule.wiresIndex.get(new Name(ACL2Object.valueOf(outputStr)));
                        if (specWire != null)
                        {
                            Util.check(specWire.isAssigned());
                            specWire.specialOutput = true;
                        }
                    }
                }
                design.computeCombinationalInputs(clockName);
                try (PrintStream out = new PrintStream(outFileName))
                {
                    int totalUseCount = 0;
                    for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
                    {
                        ModName nm = e.getKey();
                        ModuleExt m = e.getValue();
                        Map<Object, Set<Object>> crudeGraph0 = m.computeDepsGraph(false);
                        Map<Object, Set<Object>> crudeGraph1 = m.computeDepsGraph(true);
                        Map<Object, Set<Object>> crudeClosure0 = m.closure(crudeGraph0);
                        Map<Object, Set<Object>> crudeClosure1 = m.closure(crudeGraph1);
                        Map<Object, Set<Object>> fineGraph0 = m.computeFineDepsGraph(false);
                        Map<Object, Set<Object>> fineGraph1 = m.computeFineDepsGraph(true);
                        Map<Object, Set<Object>> fineClosure0 = m.closure(fineGraph0);
                        Map<Object, Set<Object>> fineClosure1 = m.closure(fineGraph1);

                        out.println("module " + nm + " // has "
                            + m.wires.size() + " wires "
                            + m.insts.size() + " insts "
                            + m.assigns.size() + " assigns "
                            + m.aliaspairs.size() + " aliaspairs "
                            + m.useCount + " useCount");
                        totalUseCount += m.useCount;
                        out.println(" wires");
                        for (WireExt w : m.wires)
                        {
                            if (w.isAssigned())
                            {
                                out.print(w.used ? "  out    " : "  output ");
                                if (w.assignedBits != null && !BigIntegerUtil.logheadMask(w.getWidth()).equals(w.assignedBits))
                                {
                                    out.print("#x" + w.getAssignedBits().toString(16));
                                }
                            } else
                            {
                                Util.check(w.getAssignedBits().signum() == 0);
                                out.print(w.used ? "  input  " : "  unused ");
                            }
                            out.print(w.isGlobal() ? "! " : w.exported ? "* " : "  ");
                            out.print(w + " //");

                            for (Map.Entry<Lhrange<PathExt>, WireExt.WireDriver> e1 : w.drivers.entrySet())
                            {
                                Lhrange<PathExt> lhr = e1.getKey();
                                WireExt.WireDriver wd = e1.getValue();
                                out.print(" " + lhr + "<=");
                                if (wd.driver != null)
                                {
                                    out.print(wd.driver.name);
                                }
                                if (wd.pi != null)
                                {
                                    out.print(wd.pi);
                                }
                            }
                            out.println();
                            if (VERBOSE_DUMP)
                            {
                                if (w.isAssigned() || !m.isTop && !w.exported)
                                {
                                    List<Map<Svar<PathExt>, BigInteger>> fineDeps0 = w.gatherFineDeps(fineGraph0);
                                    List<Map<Svar<PathExt>, BigInteger>> fineDeps1 = w.gatherFineDeps(fineGraph1);
                                    List<Map<Svar<PathExt>, BigInteger>> closureDeps0 = w.gatherFineDeps(fineClosure0);
                                    List<Map<Svar<PathExt>, BigInteger>> closureDeps1 = w.gatherFineDeps(fineClosure1);
                                    for (int i = 0; i < w.getWidth(); i++)
                                    {
                                        WireExt.Bit wb = w.getBit(i);
                                        out.println("    // " + wb + " depends on " + WireExt.showFineDeps(fineDeps0, fineDeps1, i));
                                    }
                                    for (int i = 0; i < w.getWidth(); i++)
                                    {
                                        WireExt.Bit wb = w.getBit(i);
                                        out.println("    // " + wb + " depends* on " + WireExt.showFineDeps(closureDeps0, closureDeps1, i));
                                        if (w.exported && w.isAssigned())
                                        {
                                            assert WireExt.showFineDeps(closureDeps0, closureDeps1, i).equals(w.showFineDeps(i));
                                        }
                                    }
                                }
                            } else
                            {
                                if (w.exported && w.isAssigned())
                                {
                                    for (int i = 0; i < w.getWidth(); i++)
                                    {
                                        WireExt.Bit wb = w.getBit(i);
                                        out.println("    // " + wb + " depends on " + w.showFineDeps(i));
                                    }
                                }
                            }
                        }
                        if (VERBOSE_DUMP)
                        {
                            if (!m.crudeCombinationalInputs0.equals(m.fineCombinationalInputs0)
                                || !m.crudeCombinationalInputs1.equals(m.fineCombinationalInputs1))
                            {
                                out.println("// 0 crude combinational inputs: " + m.crudeCombinationalInputs0);
                                out.println("// 1 crude combinational inputs: " + m.crudeCombinationalInputs1);
                            }
                            out.println("// 0 fine combinational inputs:  " + m.fineCombinationalInputs0);
                            out.println("// 1 fine combinational inputs:  " + m.fineCombinationalInputs1);
                        } else
                        {
                            out.println("// 0 combinational inputs: " + m.fineCombinationalInputs0);
                            out.println("// 1 combinational inputs: " + m.fineCombinationalInputs1);
                        }
                        out.println("// insts");
                        for (ModInstExt mi : m.insts)
                        {
                            out.print("  " + mi.getModname() + " " + mi.getInstname() + " (");
                            for (PathExt.PortInst pi : mi.portInsts.values())
                            {
                                if (!pi.wire.isAssigned())
                                {
                                    out.print("." + pi.wire.getName() + "(" + pi.driver + "), ");
                                }
                            }
                            out.println();
                            boolean first = true;
                            out.print("    ");
                            for (PathExt.PortInst pi : mi.portInsts.values())
                            {
                                if (pi.wire.isAssigned())
                                {
                                    if (first)
                                    {
                                        first = false;
                                    } else
                                    {
                                        out.print(", ");
                                    }
                                    out.print("." + pi.wire.getName() + "(" + pi.source + ")");
                                }
                            }
                            out.println(");");
                            if (VERBOSE_DUMP)
                            {
                                if (mi.splitIt)
                                {
                                    for (PathExt.PortInst piOut : mi.portInsts.values())
                                    {
                                        if (piOut.wire.isAssigned())
                                        {
                                            for (int bit = 0; bit < piOut.wire.getWidth(); bit++)
                                            {
                                                WireExt.Bit wb = piOut.getParentBit(bit);
                                                out.println("    // crude " + wb + " depends  on " + showCrude(crudeGraph0.get(wb), crudeGraph1.get(wb)));
                                                out.println("    // crude " + wb + " depends* on " + showCrude(crudeClosure0.get(wb), crudeClosure1.get(wb)));
                                            }
                                        }
                                    }
                                } else
                                {
                                    out.println("    // crude depends  on " + showCrude(crudeGraph0.get(mi), crudeGraph1.get(mi)));
                                    out.println("    // crude depends* on " + showCrude(crudeClosure0.get(mi), crudeClosure1.get(mi)));
                                }
                            }
//                            out.println("    // 0 depends on " + graph0.get(mi));
//                            out.println("    // 1 depends on " + graph1.get(mi));
//                            out.println("    // 0 closure " + closure0.get(mi));
//                            out.println("    // 1 closure " + closure1.get(mi));
                        }
                        out.println(" assigns");
                        for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : m.assigns.entrySet())
                        {
                            Lhs<PathExt> l = e1.getKey();
                            DriverExt d = e1.getValue();
                            assert !l.ranges.isEmpty();
                            for (int i = 0; i < l.ranges.size(); i++)
                            {
                                Lhrange<PathExt> lr = l.ranges.get(i);
                                Svar<PathExt> svar = lr.getVar();
                                assert svar.getDelay() == 0;
                                assert !svar.isNonblocking();
                                out.print((i == 0 ? "  " : ",") + lr);
                            }

                            out.print(" = " + d);
//                            Set<Svar> deps = d.svex.collectVars();
//                            Set<SVarExt.LocalWire> dep0 = getDeps(d.svex, m, clkName, Vec2.ZERO);
//                            Set<SVarExt.LocalWire> dep1 = getDeps(d.svex, m, clkName, Vec2.ONE);
//                            if (!dep0.equals(deps) || !dep1.equals(deps))
//                            {
//                                if (dep0.equals(dep1))
//                                {
//                                    out.print(" | => " + dep0);
//                                } else
//                                {
//                                    out.print(" | 0 => " + dep0 + " | 1 => " + dep1);
//                                }
//                            }
                            out.print(" // " + d.name);
                            if (VERBOSE_DUMP)
                            {
                                String sState = "";
                                for (Map.Entry<Svar<PathExt>, BigInteger> e2 : d.getCrudeDeps(false).entrySet())
                                {
                                    Svar<PathExt> svar = e2.getKey();
                                    BigInteger mask = e2.getValue();
                                    if (svar.getDelay() == 0 || mask == null || mask.signum() == 0)
                                    {
                                        continue;
                                    }
                                    if (!sState.isEmpty())
                                    {
                                        sState += ",";
                                    }
                                    sState += svar.toString(mask);
                                }
                                if (!sState.isEmpty())
                                {
                                    out.print(" STATE " + sState);
                                }
                            }
                            out.println();
                            if (VERBOSE_DUMP)
                            {
                                if (d.splitIt)
                                {
                                    for (int bit = 0; bit < l.width(); bit++)
                                    {
                                        WireExt.Bit wb = d.wireBits[bit];
                                        out.println("    // crude " + bit + " depends  on " + showCrude(crudeGraph0.get(wb), crudeGraph1.get(wb)));
                                        out.println("    // crude " + bit + " depends* on " + showCrude(crudeClosure0.get(wb), crudeClosure1.get(wb)));
                                    }
                                } else
                                {
                                    out.println("    // crude depends  on " + showCrude(crudeGraph0.get(d.name), crudeGraph1.get(d.name)));
                                    out.println("    // crude depends* on " + showCrude(crudeClosure0.get(d.name), crudeClosure1.get(d.name)));
                                }
                                if (l.ranges.size() == 1 && l.ranges.get(0).getVar().getName() instanceof PathExt.PortInst)
                                {
                                    List<Map<Svar<PathExt>, BigInteger>> fineDeps0 = d.gatherFineDeps(fineGraph0);
                                    List<Map<Svar<PathExt>, BigInteger>> fineDeps1 = d.gatherFineDeps(fineGraph1);
                                    List<Map<Svar<PathExt>, BigInteger>> closureDeps0 = d.gatherFineDeps(fineClosure0);
                                    List<Map<Svar<PathExt>, BigInteger>> closureDeps1 = d.gatherFineDeps(fineClosure1);
                                    for (int i = 0; i < l.width(); i++)
                                    {
                                        WireExt.Bit wb = d.wireBits[i];
                                        out.println("    // " + wb + " depends on " + WireExt.showFineDeps(fineDeps0, fineDeps1, i));
                                    }
                                    for (int i = 0; i < l.width(); i++)
                                    {
                                        WireExt.Bit wb = d.wireBits[i];
                                        out.println("    // " + wb + " depends* on " + WireExt.showFineDeps(closureDeps0, closureDeps1, i));
                                    }
                                }
                            }
//                            out.println("    // 0 depends on " + graph0.get(d.name));
//                            out.println("    // 1 depends on " + graph1.get(d.name));
//                            out.println("    // 0 closure " + closure0.get(d.name));
//                            out.println("    // 1 closure " + closure1.get(d.name));
                        }
//                        out.println(" aliaspairs");
                        for (Map.Entry<Lhs<PathExt>, Lhs<PathExt>> e1 : m.aliaspairs.entrySet())
                        {
                            Lhs<PathExt> l = e1.getKey();
                            Lhs<PathExt> r = e1.getValue();
                            assert l.ranges.size() == 1;
                            Lhrange<PathExt> lr = l.ranges.get(0);
                            assert lr.getRsh() == 0;
                            Svar<PathExt> svar = lr.getVar();
                            assert svar.getDelay() == 0;
                            assert !svar.isNonblocking();
                            if (svar.getName() instanceof PathExt.PortInst)
                            {
                                continue;
                            }
                            out.print("  // alias " + lr + " <->");
                            for (Lhrange<PathExt> lr1 : r.ranges)
                            {
                                svar = lr1.getVar();
                                assert svar.getDelay() == 0;
                                assert !svar.isNonblocking();
                                out.print(" " + lr1);
                            }
                            out.println();
                        }
                        out.println("endmodule // " + nm);
                        out.println();
                    }
                    out.println("// totalUseCount=" + totalUseCount);
                    out.println("// design.top=" + design.getTop());
                    if (clockName != null)
                    {
                        out.println("// clock=" + clockName);
                    }
                }
            } catch (IOException e)
            {
                return false;
            }
            return true;
        }
    }

    private static String showCrude(Set<Object> dep0, Set<Object> dep1)
    {
        if (dep0.equals(dep1))
        {
            return dep0.toString();
        } else
        {
            return "0=>" + dep0 + " | 1=>" + dep1;
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

    public static void showSvexLibs(File saoFile)
    {
        new ShowSvexLibsJob<>(GenTutorial.class, saoFile).startJob();
    }

    public static class ShowSvexLibsJob<T extends GenFsmNew> extends Job
    {
        private final Class<T> cls;
        private final File saoFile;

        public ShowSvexLibsJob(Class<T> cls, File saoFile)
        {
            super("Show used Svex Libs", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.cls = cls;
            this.saoFile = saoFile;
        }

        @Override
        public boolean doIt() throws JobException
        {
            try
            {
                GenFsmNew gen = cls.newInstance();
                gen.scanLib(saoFile);
                gen.showLibs();
            } catch (InstantiationException | IllegalAccessException | IOException e)
            {
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        }
    }

    public static <T extends GenFsmNew> void compareSvexLibs(Class<T> cls, File[] saoFiles)
    {
        new CompareSvexLibsJob(cls, saoFiles).startJob();
    }

    private static class CompareSvexLibsJob<T extends GenFsmNew> extends Job
    {
        private final Class<T> cls;
        private final File[] saoFiles;

        public CompareSvexLibsJob(Class<T> cls, File[] saoFiles)
        {
            super("Compare Svex Libs", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.cls = cls;
            this.saoFiles = saoFiles;
        }

        @Override
        public boolean doIt() throws JobException
        {
            try
            {
                Map<ModName, Module<Address>> modMap = new HashMap<>();
                GenFsmNew gen = cls.newInstance();
                for (File saoFile : saoFiles)
                {
                    System.out.println(saoFile);
                    gen.scanLib(saoFile);
                    ACL2Reader sr = new ACL2Reader(saoFile);
                    Address.SvarBuilder builder = new Address.SvarBuilder();
                    Design<Address> design = new Design<>(builder, sr.root);
                    for (Map.Entry<ModName, Module<Address>> e : design.modalist.entrySet())
                    {
                        ModName modName = e.getKey();
                        Module<Address> newM = e.getValue();
                        Module<Address> oldM = modMap.get(modName);
                        if (oldM != null)
                        {
                            if (newM.equals(oldM))
                            {
                                assert newM.getACL2Object().equals(oldM.getACL2Object());
                            } else
                            {
                                System.out.println("Defferent module " + modName + " in " + saoFile);
                            }
                        } else
                        {
                            modMap.put(modName, newM);
                        }
                    }
                }
                gen.showLibs();
            } catch (InstantiationException | IllegalAccessException | IOException e)
            {
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        }
    }

    public static void dedup(File saoFile, String designName, String outFileName)
    {
        new DedupSvexJob(saoFile, designName, outFileName).startJob();
    }

    private static class DedupSvexJob extends Job
    {
        private final File saoFile;
        private final String designName;
        private final String outFileName;

        private DedupSvexJob(File saoFile, String designName, String outFileName)
        {
            super("Dedup SVEX in Design", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
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
                Map<Svex<PathExt>, String> svexLabels = new LinkedHashMap<>();
                Map<Svex<PathExt>, BigInteger> svexSizes = new HashMap<>();
                try (PrintStream out = new PrintStream(outFileName))
                {
                    out.println("(in-package \"SV\")");
                    out.println("(include-book \"std/util/defrule\" :dir :system)");
                    out.println("(include-book \"std/util/defconsts\" :dir :system)");
                    out.println("(include-book \"centaur/sv/mods/svmods\" :dir :system)");
                    out.println("(include-book \"centaur/sv/svex/svex\" :dir :system)");
                    out.println("(include-book \"centaur/sv/svex/rewrite\" :dir :system)");
                    out.println("(include-book \"centaur/sv/svex/xeval\" :dir :system)");
                    out.println();
                    out.println("(defconsts (*" + designName + "* state)");
                    out.println("  (serialize-read \"" + designName + ".sao\"))");
                    out.println();
                    out.println("(local (defn extract-labels (labels acc)");
                    out.println("  (if (atom labels)");
                    out.println("     ()");
                    out.println("    (cons (cdr (hons-get (car labels) acc))");
                    out.println("          (extract-labels (cdr labels) acc)))))");
                    out.println();
                    out.println("(local (defun from-dedup (x acc)");
                    out.println("  (if (atom x)");
                    out.println("       acc");
                    out.println("    (let* ((line (car x))");
                    out.println("           (label (car line))");
                    out.println("           (kind (cadr line))");
                    out.println("           (args (cddr line)))");
                    out.println("      (from-dedup");
                    out.println("        (cdr x)");
                    out.println("        (hons-acons");
                    out.println("          label");
                    out.println("          (case kind");
                    out.println("            (:quote (make-svex-quote :val (car args)))");
                    out.println("            (:var (make-svex-var :name (car args)))");
                    out.println("            (:call (make-svex-call :fn (car args)");
                    out.println("                                   :args (extract-labels (cdr args) acc))))");
                    out.println("          acc))))))");
                    out.println();
                    out.println("(defconsts (*" + designName + "-dedup*)");
                    out.println(" (from-dedup `(");
                    for (ModuleExt m : design.downTop.values())
                    {
                        for (DriverExt dr : m.assigns.values())
                        {
                            genDedup(out, dr.getSvex(), svexLabels, svexSizes);
                        }
                    }
                    out.println(" ) ()))");
                    out.println();
                    for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
                    {
                        ModName mn = e.getKey();
                        ModuleExt m = e.getValue();
                        out.println();
                        out.println("(local (defun |check-" + mn + "| ()");
                        out.println("  (let ((m (cdr (assoc-equal '" + mn + " (design->modalist *" + designName + "*)))))");
                        out.println("    (equal (hons-copy (strip-cars (strip-cdrs (module->assigns m))))");
                        out.print("           (extract-labels '(");
                        for (DriverExt dr : m.assigns.values())
                        {
                            out.print(" " + svexLabels.get(dr.getSvex()));
                        }
                        out.println(")");
                        out.println("                           *" + designName + "-dedup*)))))");
                    }
                    out.println();
                    out.println("(rule");
                    out.println("  (and");
                    for (ModName mn : design.downTop.keySet())
                    {
                        out.println("    (|check-" + mn + "|)");
                    }
                    out.println("))");
                    out.println();
                    out.println("(defconsts (*" + designName + "-xeval*) '(");
                    Map<Svex<PathExt>, Vec4> xevalMemoize = new HashMap<>();
                    for (Map.Entry<Svex<PathExt>, String> e : svexLabels.entrySet())
                    {
                        Svex<PathExt> svex = e.getKey();
                        String label = e.getValue();
                        Vec4 xeval = svex.xeval(xevalMemoize);
                        out.println("  (" + label + " . " + xeval.makeAcl2Object().rep() + ")");
                    }
                    out.println(" ))");
                    out.println();
                    out.println("(local (defun check-xeval (alist dedup)");
                    out.println("  (or (atom alist)");
                    out.println("      (and (equal (svex-xeval (cdr (hons-get (caar alist) dedup)))");
                    out.println("                  (cdar alist))");
                    out.println("           (check-xeval (cdr alist) dedup)))))");
                    out.println();
                    out.println("(rule");
                    out.println("  (check-xeval *" + designName + "-xeval* *" + designName + "-dedup*))");
                    out.println();
                    out.println("(defconsts (*" + designName + "-toposort*) '(");
                    for (Map.Entry<Svex<PathExt>, String> e : svexLabels.entrySet())
                    {
                        Svex<PathExt> svex = e.getKey();
                        String label = e.getValue();
                        Svex<PathExt>[] toposort = svex.toposort();
                        Util.check(toposort[0].equals(svex));
                        out.print("  (" + label);
                        for (int i = 1; i < toposort.length; i++)
                        {
                            out.print(" " + svexLabels.get(toposort[i]));
                        }
                        out.println(")");
                    }
                    out.println(" ))");
                    out.println();
                    out.println("(local (defun check-toposort (list dedup)");
                    out.println("  (or (atom list)");
                    out.println("      (b* ((toposort (extract-labels (car list) dedup))");
                    out.println("           ((mv sort ?contents) (svex-toposort (car toposort) () ()))");
                    out.println("           (sort (hons-copy sort)))");
                    out.println("        (and (equal sort toposort)");
                    out.println("             (check-toposort (cdr list) dedup))))))");
                    out.println();
                    out.println("(rule");
                    out.println("  (check-toposort *" + designName + "-toposort* *" + designName + "-dedup*))");
                    out.println();
                    out.println("(defconsts (*" + designName + "-masks*) '(");
                    for (Map.Entry<Svex<PathExt>, String> e : svexLabels.entrySet())
                    {
                        Svex<PathExt> svex = e.getKey();
                        String label = e.getValue();
                        Svex<PathExt>[] toposort = svex.toposort();
                        Util.check(toposort[0].equals(svex));
                        Map<Svex<PathExt>, BigInteger> masks = svex.maskAlist(BigIntegerUtil.MINUS_ONE);
                        out.print("  (" + label);
                        for (int i = 0; i < toposort.length; i++)
                        {
                            BigInteger mask = masks.get(toposort[i]);
                            if (mask == null)
                            {
                                mask = BigInteger.ZERO;
                            }
                            out.print(" #x" + mask.toString(16));
                        }
                        out.println(")");
                    }
                    out.println(" ))");
                    out.println();
                    out.println("(local (defun toposort-label (label dedup)");
                    out.println("  (b* ((svex (cdr (hons-get label dedup)))");
                    out.println("       ((mv toposort ?contents) (svex-toposort svex () ())))");
                    out.println("    toposort)))");
                    out.println();
                    out.println("(local (defun comp-masks (toposort mask-al)");
                    out.println("  (if (atom toposort)");
                    out.println("      ()");
                    out.println("    (cons (svex-mask-lookup (car toposort) mask-al)");
                    out.println("          (comp-masks (cdr toposort) mask-al)))))");
                    out.println();
                    out.println("(local (defun masks-label (label dedup)");
                    out.println("  (b* ((svex (cdr (hons-get label dedup)))");
                    out.println("       (toposort (toposort-label label dedup))");
                    out.println("       (mask-al (svexlist-mask-alist (list svex))))");
                    out.println("    (comp-masks toposort mask-al))))");
                    out.println();
                    out.println("(local (defun show-line (line dedup)");
                    out.println("  (list");
                    out.println("   :line line");
                    out.println("   :toposort (toposort-label (car line) dedup)");
                    out.println("   :masks (masks-label (car line) dedup)");
                    out.println("   :ok (equal (masks-label (car line) dedup) (cdr line)))))");
                    out.println();
                    out.println("(local (defun check-masks (masks-lines dedup)");
                    out.println("  (or (atom masks-lines)");
                    out.println("      (and (let ((line (car masks-lines)))");
                    out.println("             (equal (masks-label (car line) dedup) (cdr line)))");
                    out.println("           (check-masks (cdr masks-lines) dedup)))))");
                    out.println();
                    out.println("(rule");
                    out.println("  (check-masks *" + designName + "-masks* *" + designName + "-dedup*))");
                }
            } catch (IOException e)
            {
                return false;
            }
            return true;
        }

        private <N extends SvarName> BigInteger computeSize(Svex<N> svex, Map<Svex<N>, BigInteger> sizes)
        {
            BigInteger size = sizes.get(svex);
            if (size == null)
            {
                if (svex instanceof SvexCall)
                {
                    size = BigInteger.ONE;
                    for (Svex<N> arg : ((SvexCall<N>)svex).getArgs())
                    {
                        size = size.add(computeSize(arg, sizes));
                    }
                } else
                {
                    size = BigInteger.ONE;
                }
                sizes.put(svex, size);
            }
            return size;
        }

        private String genDedup(PrintStream out, Svex<PathExt> svex,
            Map<Svex<PathExt>, String> svexLabels, Map<Svex<PathExt>, BigInteger> svexSizes)
        {
            String label = svexLabels.get(svex);
            if (label == null)
            {
                if (svex instanceof SvexQuote)
                {
                    SvexQuote<PathExt> sq = (SvexQuote<PathExt>)svex;
                    label = "l" + svexLabels.size();
                    svexLabels.put(svex, label);
                    out.print(" (" + label + " :quote ");
                    Vec4 val = sq.val;
                    if (val instanceof Vec2)
                    {
                        out.print("#x" + ((Vec2)val).getVal().toString(16));
                    } else
                    {
                        out.print("(#x" + val.getUpper().toString(16) + " . #x" + val.getLower().toString(16) + ")");
                    }
                    out.println(")");
                } else if (svex instanceof SvexVar)
                {
                    SvexVar<PathExt> sv = (SvexVar<PathExt>)svex;
                    label = "l" + svexLabels.size();
                    svexLabels.put(svex, label);
                    out.print(" (" + label + " :var ,(make-svar :name ");
                    if (sv.svar.getName() instanceof PathExt.PortInst)
                    {
                        PathExt.PortInst pi = (PathExt.PortInst)sv.svar.getName();
                        out.print("'(" + pi.inst.getInstname().toLispString() + " . " + pi.wire.getName().toLispString() + ")");
                    } else
                    {
                        PathExt.LocalWire lw = (PathExt.LocalWire)sv.svar.getName();
                        out.print(lw.wire.getName().toLispString());
                        if (sv.svar.getDelay() != 0)
                        {
                            out.print(" :delay " + sv.svar.getDelay());
                        }
                    }
                    out.println("))");
                } else
                {
                    SvexCall<PathExt> sc = (SvexCall<PathExt>)svex;
                    Svex<PathExt>[] args = sc.getArgs();
                    String[] labels = new String[args.length];
                    for (int i = 0; i < labels.length; i++)
                    {
                        labels[i] = genDedup(out, args[i], svexLabels, svexSizes);
                    }
                    label = "l" + svexLabels.size();
                    svexLabels.put(svex, label);
                    out.print(" (" + label + " :call " + symbol_name(sc.fun.fn).stringValueExact());
                    for (String l : labels)
                    {
                        out.print(" " + l);
                    }
                    out.println(") ; " + computeSize(svex, svexSizes));
                }
            }
            return label;
        }
    }

    public static void showAssigns(File saoFile, String designName, String outFileName)
    {
        new ShowAssignsJob(saoFile, designName, outFileName).startJob();
    }

    private static class ShowAssignsJob extends Job
    {
        private final File saoFile;
        private final String designName;
        private final String outFileName;

        private ShowAssignsJob(File saoFile, String designName, String outFileName)
        {
            super("Show SVEX assigns", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
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
                try (PrintStream out = new PrintStream(outFileName))
                {
                    out.println("(in-package \"SV\")");
                    out.println("(include-book \"std/util/defconsts\" :dir :system)");
                    out.println("(include-book \"std/util/define\" :dir :system)");
                    out.println("(include-book \"std/util/defrule\" :dir :system)");
                    out.println("(include-book \"centaur/sv/mods/svmods\" :dir :system)");
                    out.println("(include-book \"centaur/sv/svex/svex\" :dir :system)");
                    out.println("(include-book \"centaur/sv/svex/rewrite\" :dir :system)");
//                    out.println("(include-book \"centaur/sv/svex/xeval\" :dir :system)");
                    out.println();
                    out.println("(defconsts (*" + designName + "* state)");
                    out.println("  (serialize-read \"" + designName + ".sao\"))");
                    out.println();
                    out.println("(local (define filter-vars");
                    out.println("  ((toposort svexlist-p)");
                    out.println("   (mask-al svex-mask-alist-p))");
                    out.println("  :returns (filtered svex-mask-alist-p)");
                    out.println("  (and (consp toposort)");
                    out.println("       (let ((svex (svex-fix (car toposort)))");
                    out.println("             (rest (cdr toposort)))");
                    out.println("         (svex-case svex");
                    out.println("           :quote (filter-vars rest mask-al)");
                    out.println("           :call (filter-vars rest mask-al)");
                    out.println("           :var (cons (cons svex (svex-mask-lookup svex mask-al))");
                    out.println("                      (filter-vars rest mask-al)))))))");
                    out.println();
                    out.println("(local (define compute-driver-masks");
                    out.println("  ((x svexlist-p))");
                    out.println("  (and (consp x)");
                    out.println("       (b* ((svex (car x))");
                    out.println("            ((mv toposort ?contents) (svex-toposort svex () ()))");
                    out.println("            (mask-al (svexlist-mask-alist (list svex))))");
                    out.println("         (cons (rev (filter-vars toposort mask-al))");
                    out.println("               (compute-driver-masks (cdr x)))))))");

                    for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
                    {
                        ModName mn = e.getKey();
                        ModuleExt m = e.getValue();
                        out.println();
                        out.println("(local (define |check-" + mn + "| ()");
                        out.println("  (let ((m (cdr (assoc-equal \"" + mn + "\" (design->modalist *" + designName + "*)))))");
                        out.println("    (equal (compute-driver-masks (hons-copy (strip-cars (strip-cdrs (module->assigns m))))) `(");
                        for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : m.assigns.entrySet())
                        {
                            Lhs<PathExt> l = e1.getKey();
                            DriverExt d = e1.getValue();

                            Set<Svar<PathExt>> vars = d.collectVars();
                            Map<Svex<PathExt>, BigInteger> masks = d.getSvex().maskAlist(BigIntegerUtil.MINUS_ONE);
                            out.print("      (;");
                            assert !l.ranges.isEmpty();
                            for (int i = 0; i < l.ranges.size(); i++)
                            {
                                Lhrange<PathExt> lr = l.ranges.get(i);
                                Svar<PathExt> svar = lr.getVar();
                                assert svar.getDelay() == 0;
                                assert !svar.isNonblocking();
                                out.print((i == 0 ? "  " : ",") + lr);
                            }
                            out.println();
                            for (Svar<PathExt> var : vars)
                            {
                                PathExt.LocalWire lw = (PathExt.LocalWire)var.getName();
                                Svex<PathExt> svex = new SvexVar<>(var);
                                BigInteger mask = masks.get(svex);
                                if (mask == null)
                                {
                                    mask = BigInteger.ZERO;
                                }
                                out.print("        (");
                                String rep = lw.name.getACL2Object().rep();
                                if (var.getDelay() == 0)
                                {
                                    out.print(rep);
                                } else
                                {
                                    out.print(",(make-svar :name " + rep + " :delay " + var.getDelay() + ")");
                                }
                                out.println(" . #x" + mask.toString(16) + ")");
                            }
                            out.print("      )");
                        }
                        out.println(" )))))");
                    }
                    out.println();
                    out.println("(rule");
                    out.println("  (and");
                    for (ModName mn : design.downTop.keySet())
                    {
                        out.println("    (|check-" + mn + "|)");
                    }
                    out.println("))");

                }
            } catch (IOException e)
            {
                return false;
            }
            return true;
        }
    }

    public static void namedToIndexed(File saoFile, String designName, String outFileName)
    {
        new NamedToIndexedJob(saoFile, designName, outFileName).startJob();
    }

    private static class NamedToIndexedJob extends Job
    {
        private final File saoFile;
        private final String designName;
        private final String outFileName;

        private NamedToIndexedJob(File saoFile, String designName, String outFileName)
        {
            super("Named->Indexed", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
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
                Address.SvarBuilder builder = new Address.SvarBuilder();
                Design<Address> design = new Design<>(builder, sr.root);
                ModDb db = new ModDb(design.top, design.modalist);
                Map<ModName, Module<Address>> indexedMods = db.modalistNamedToIndex(design.modalist);
                ACL2Object indexedAlist = NIL;
                for (Map.Entry<ModName, Module<Address>> e : indexedMods.entrySet())
                {
                    indexedAlist = cons(cons(e.getKey().getACL2Object(), e.getValue().getACL2Object()), indexedAlist);
                }
                indexedAlist = Util.revList(indexedAlist);

                File outFile = new File(outFileName);
                File outDir = outFile.getParentFile();
                File saoIndexedFile = new File(outDir, designName + "-indexed.sao");
                ACL2Writer.write(indexedAlist, saoIndexedFile);
                try (PrintStream out = new PrintStream(outFileName))
                {
                    out.println("(in-package \"SV\")");
                    out.println("(include-book \"std/util/defconsts\" :dir :system)");
                    out.println("(include-book \"std/util/defrule\" :dir :system)");
                    out.println("(include-book \"centaur/sv/mods/moddb\" :dir :system)");
                    out.println("(include-book \"centaur/misc/hons-extra\" :dir :system)");
                    out.println();
                    out.println("(defconsts (*" + designName + "* state)");
                    out.println("  (serialize-read \"" + designName + ".sao\"))");
                    out.println();
                    out.println("(defconsts (*" + designName + "-indexed* state)");
                    out.println("  (serialize-read \"" + designName + "-indexed.sao\"))");
                    out.println();
                    out.println("(define svex-moddb-indexed");
                    out.println("  ((x design-p))");
                    out.println("  :guard (modalist-addr-p (design->modalist x))");
                    out.println("  :guard-hints ((\"goal\" :do-not-induct t");
                    out.println("                 :in-theory (e/d () (create-moddb moddb->nmods))))");
                    out.println("  (b* (((acl2::local-stobjs moddb)");
                    out.println("        (mv indexed moddb))");
                    out.println("       (moddb (moddb-clear moddb))");
                    out.println("       ((design x) x)");
                    out.println("       (modalist x.modalist)");
                    out.println("       (topmod x.top)");
                    out.println("       ((with-fast modalist))");
                    out.println("       ((unless (modhier-loopfree-p topmod modalist))");
                    out.println("        (mv");
                    out.println("         (msg \"Module ~s0 has an instance loop!~%\" topmod)");
                    out.println("         moddb))");
                    out.println("       (moddb (module->db topmod modalist moddb))");
                    out.println("       ((mv err indexed) (modalist-named->indexed modalist moddb :quiet t))");
                    out.println("       ((when err)");
                    out.println("        (mv (msg \"Error indexing wire names: ~@0~%\" err)");
                    out.println("            moddb)))");
                    out.println("    (mv indexed moddb)))");
                    out.println();
                    out.println("(rule (modalist-p *" + designName + "-indexed*))");
                    out.println();
                    out.println("(rule");
                    out.println(" (equal");
                    out.println("  (svex-moddb-indexed *" + designName + "*)");
                    out.println("  *" + designName + "-indexed*))");
                }
            } catch (IOException e)
            {
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

        public Alu()
        {
        }

        @Override
        protected boolean ignore_wire(WireExt w)
        {
            String s = w.getName().impl.stringValueExact();
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
    }

}
