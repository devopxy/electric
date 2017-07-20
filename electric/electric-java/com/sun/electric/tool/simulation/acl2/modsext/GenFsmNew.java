/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenFsmNew.java
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
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.mods.Wire;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.SvexVar;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Concat;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 */
public class GenFsmNew extends GenBase
{
    public static <H extends DesignHints> void genFsm(Class<H> cls, File saoFile, String designName)
    {
        new GenFsmJob(cls, saoFile, designName).startJob();
    }

    private final String[] clockNames =
    {
        "l2clk"
    };
    final Map<ModName, ParameterizedModule> modToParMod = new HashMap<>();
    final Map<ParameterizedModule, Map<String, ModName>> parModuleInstances = new HashMap<>();
    private final Set<Integer> vec4sizes = new TreeSet<>();
    private String designName;

    private final DesignHints designHints;
    private final List<ParameterizedModule> parameterizedModules;

    protected GenFsmNew(DesignHints designHints)
    {
        this.designHints = designHints;
        parameterizedModules = designHints.getParameterizedModules();
    }

    ParameterizedModule matchParameterized(ModName modName)
    {
        for (ParameterizedModule parMod : parameterizedModules)
        {
            if (parMod.setCurBuilder(modName))
            {
                return parMod;
            }
        }
        return null;
    }

    public void scanLib(File saoFile) throws IOException
    {
        ACL2Reader sr = new ACL2Reader(saoFile);
        DesignExt design = new DesignExt(sr.root, designHints);
        scanDesign(design);
        for (ModName modName : design.downTop.keySet())
        {
            if (!modToParMod.containsKey(modName))
            {
                System.out.println(modName);
            }
        }
    }

    public void showLibs()
    {
        System.out.println("========= Instances of libs ============");
        for (ParameterizedModule parModule : parameterizedModules)
        {
            Map<String, ModName> parInsts = parModuleInstances.get(parModule);
            if (parInsts != null)
            {
                System.out.println(parModule);
                for (ModName modName : parInsts.values())
                {
                    System.out.println("   " + parModule.matchModName(modName));
                }
            }
        }
        System.out.println("vec4 sizes");
        for (Integer width : vec4sizes)
        {
            System.out.println("(def-4vec-p " + width + ")");
        }
    }

    void scanDesign(DesignExt design)
    {
        List<ParameterizedModule> parModules = parameterizedModules;
        for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
        {
            ModName modName = e.getKey();
            ModuleExt m = e.getValue();
            boolean found = false;
            for (ParameterizedModule parModule : parModules)
            {
                if (parModule.setCurBuilder(modName))
                {
                    assert !found;
                    found = true;
                    Map<String, ModName> parInsts = parModuleInstances.get(parModule);
                    if (parInsts == null)
                    {
                        parInsts = new TreeMap<>(TextUtils.STRING_NUMBER_ORDER);
                        parModuleInstances.put(parModule, parInsts);
                    }
                    parInsts.put(modName.toString(), modName);
                    modToParMod.put(modName, parModule);
                    Module<Path> genM = parModule.genModule();
                    if (genM == null)
                    {
                        System.out.println("Module specalizition is unfamiliar " + modName);
                    } else if (!genM.equals(m.b))
                    {
                        System.out.println("Module mismatch " + modName);
                    }
                }
            }
            for (Wire wire : m.b.wires)
            {
                vec4sizes.add(wire.width);
            }
            for (Lhs<?> lhs : m.b.assigns.keySet())
            {
                vec4sizes.add(lhs.width());
            }
        }
    }

    void gen(String designName, DesignExt design, File outDir) throws FileNotFoundException
    {
        scanDesign(design);
        this.designName = designName;

        File readSaoFile = new File(outDir, designName + "-sao.lisp");
        try (PrintStream out = new PrintStream(readSaoFile))
        {
            this.out = out;
            printReadSao();
        } finally
        {
            this.out = null;
        }

        for (Map.Entry<ParameterizedModule, Map<String, ModName>> e : parModuleInstances.entrySet())
        {
            ParameterizedModule parMod = e.getKey();
            Map<String, ModName> specializations = e.getValue();
            if (!parMod.hasState() || specializations.isEmpty())
            {
                continue;
            }

            File statesFile = new File(outDir, parMod.modName + "-st.lisp");
            try (PrintStream out = new PrintStream(statesFile))
            {
                this.out = out;
                printPhaseStates(design, parMod, specializations.values());
            } finally
            {
                this.out = null;
            }
        }
        for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
        {
            ModName modName = e.getKey();
            ModuleExt m = e.getValue();
            if (modToParMod.containsKey(modName) || !m.hasState)
            {
                continue;
            }

            File statesFile = new File(outDir, modName + "-st.lisp");
            try (PrintStream out = new PrintStream(statesFile))
            {
                this.out = out;
                printPhaseStates(design, null, Collections.singleton(modName));
            } finally
            {
                this.out = null;
            }
        }

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

        for (Map.Entry<ParameterizedModule, Map<String, ModName>> e : parModuleInstances.entrySet())
        {
            ParameterizedModule parMod = e.getKey();
            Map<String, ModName> specializations = e.getValue();
            if (specializations.isEmpty())
            {
                continue;
            }

            File locFile = new File(outDir, parMod.modName + "-loc.lisp");
            try (PrintStream out = new PrintStream(locFile))
            {
                this.out = out;
                printLocs(design, parMod, specializations.values());
            } finally
            {
                this.out = null;
            }
        }
        for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
        {
            ModName modName = e.getKey();
            ModuleExt m = e.getValue();
            if (modToParMod.containsKey(modName))
            {
                continue;
            }

            File statesFile = new File(outDir, modName + "-loc.lisp");
            try (PrintStream out = new PrintStream(statesFile))
            {
                this.out = out;
                printLocs(design, null, Collections.singleton(modName));
            } finally
            {
                this.out = null;
            }
        }

//        File phase2File = new File(outDir, designName + "-phase2.lisp");
//        try (PrintStream out = new PrintStream(phase2File))
//        {
//            this.out = out;
//            printPhase2(design);
//        }
    }

    private void printReadSao()
    {
        s("(in-package \"SV\")");
        s("(include-book \"std/util/defconsts\" :dir :system)");
        s("(include-book \"std/util/define\" :dir :system)");
        s("(include-book \"../4vec-nnn\")");
        s("(include-book \"../design-fetch-svex\")");
        s();
        s("(defconsts (*" + designName + "-sao* state)");
        sb("(serialize-read \"" + designName + ".sao\"))");
        e();
        s();
        s("(define " + designName + "-sao ()");
        sb(":returns (design design-p)");
        s("*" + designName + "-sao*)");
        e();
        s();
        s("(in-theory (disable (:executable-counterpart " + designName + "-sao)))");
        s();
        s("(define " + designName + "-sao-fetch-svex-guard (modname assign-idx)");
        sb(":returns (ok booleanp)");
        s("(design-fetch-svex-guard (" + designName + "-sao) modname assign-idx))");
        e();
        s();
        s("(define " + designName + "-sao-fetch-svex");
        sb("(modname");
        sb("assign-idx)");
        e();
        s(":guard (" + designName + "-sao-fetch-svex-guard modname assign-idx)");
        s(":returns (svex svex-p)");
        s("(design-fetch-svex (" + designName + "-sao) modname assign-idx)");
        s(":guard-hints ((\"goal\" :in-theory (enable " + designName + "-sao-fetch-svex-guard))))");
        e();
        s();
        s("(define " + designName + "-sao-svex-eval");
        sb("(modname");
        sb("assign-idx");
        s("(width posp)");
        s("(env svex-env-p))");
        e();
        s(":guard (" + designName + "-sao-fetch-svex-guard modname assign-idx)");
        s(":returns (result (4vec-n-p width result) :hyp (posp width))");
        s(":guard-hints ((\"goal\" :in-theory (enable " + designName + "-sao-fetch-svex-guard)))");
        s("(let*");
        sb("((svex (" + designName + "-sao-fetch-svex modname assign-idx))");
        sb("(width (pos-fix width))");
        s("(svex (list 'concat width svex 0)))");
        e();
        s("(with-fast-alist env (svex-eval svex env)))");
        e();
        s("///");
        s("(deffixequiv " + designName + "-sao-svex-eval))");
        e();
        assert indent == 0;
    }

    private void printPhaseStates(DesignExt design, ParameterizedModule parMod, Collection<ModName> modNames)
    {
        s("(in-package \"SV\")");
        s("(include-book \"centaur/fty/top\" :dir :system)");
        s("(include-book \"../4vec-nnn\")");
        s();
        s("(set-rewrite-stack-limit 2000)");
        Set<String> imports = new HashSet<>();
        for (ModName modName : modNames)
        {
            ModuleExt m = design.downTop.get(modName);
            if (!m.hasState)
            {
                continue;
            }
            for (ModInstExt inst : m.insts)
            {
                ModuleExt proto = inst.proto;
                if (proto.hasState)
                {
                    ParameterizedModule protoParMod = modToParMod.get(proto.modName);
                    String importStr = protoParMod != null ? protoParMod.modName : proto.modName.toString();
                    if (!imports.contains(importStr))
                    {
                        s("(include-book \"" + importStr + "-st\")");
                        imports.add(importStr);
                    }
                }
            }
        }

        for (ModName modName : modNames)
        {
            ModuleExt m = design.downTop.get(modName);
            if (!m.hasState)
            {
                continue;
            }
            printPhaseState(modName, m);
        }
    }

    private void printLocs(DesignExt design, ParameterizedModule parMod, Collection<ModName> modNames)
    {
        s("(in-package \"SV\")");
        s("(include-book \"centaur/fty/top\" :dir :system)");
        s("(include-book \"../4vec-nnn\")");
        s("(include-book \"" + designName + "-sao\")");
        for (ModName modName : modNames)
        {
            ModuleExt m = design.downTop.get(modName);
            printPhase2(modName, m);
        }
    }

    private void printPhase2(ModName modName, ModuleExt m)
    {
        s();
        s("; " + modName);
        int assignIndex = 0;
        for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : m.assigns.entrySet())
        {
            Lhs<PathExt> lhs = e1.getKey();
            DriverExt drv = e1.getValue();
            s();
            s("(define |" + modName + "-" + lhs + "-loc| (");
            b();
            b();
            Set<Svar<PathExt>> svars = drv.collectVars();
            for (Svar<PathExt> svar : svars)
            {
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                s("(|" + svar + "| 4vec-" + lw.wire.getWidth() + "-p)");
            }
            out.print(")");
            e();
            s(":returns (|" + lhs + "| 4vec-" + lhs.width() + "-p)");
            s("(let ((env (list");
            b();
            for (Svar<PathExt> svar : svars)
            {
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                String s = "(cons ";
                if (svar.getDelay() == 0)
                {
                    s += "\"" + lw.name + "\"";
                } else
                {
                    s += "'(:var " + "\"" + lw.name + "\" . " + svar.getDelay() + ")";
                }
                s += " (4vec-" + lw.wire.getWidth() + "-fix |" + svar + "|))";
                s(s);
            }
            out.print(")))");
            e();
            s("(" + designName + "-sao-svex-eval " + modName.toLispString() + " "
                + assignIndex + " " + lhs.width() + " " + "env))");
            s("///");
            s("(deffixequiv |" + modName + "-" + lhs + "-loc|))");
//                        out.print("  (declare (ignore");
//                        for (Svar<PathExt> svar : svars)
//                        {
//                            out.print(" |" + svar + "|");
//                        }
//                        out.print("))");
//                        printSvex(out, drv.getSvex(), lhs.width());
            e();
            if (svars.isEmpty())
            {
                s("(in-theory (disable (|" + modName + "-" + lhs + "-loc|)))");
            }
            assignIndex++;
        }
    }

    private void printPhaseState(ModName modName, ModuleExt m)
    {
        s();
        s("; " + modName);
        s();
        s("(defprod |" + modName + "-phase-st| (");
        b();
        b();
        for (WireExt wire : m.stateWires)
        {
            s("(|" + wire + "| 4vec-" + wire.getWidth() + ")");
        }
        for (ModInstExt inst : m.insts)
        {
            if (inst.proto.hasState)
            {
                s("(|" + inst.getInstname() + "| |" + inst.proto.modName + "-phase-st|)");
            }
        }
        out.print(")");
        e();
        s(":layout :fulltree)");
        e();
        assert indent == 0;
    }

    private void printSvex(Svex<PathExt> top, int width)
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
                    printSvex(svex, multirefNames, 4);
                    out.print(')');
                    multirefNames.put(svex, name);
                }
            }
            out.print(')');
            indent = 4;
        }
        printSvex(top, multirefNames, indent);
        out.println(multirefs.isEmpty() ? ")" : "))");
    }

    private void printSvex(Svex<PathExt> top, Map<Svex<PathExt>, String> multirefsNames, int indent)
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
                    printSvex(arg, multirefsNames, indent + 1);
                }
                out.print(')');
            }
        }
    }

    static class GenFsmJob<H extends DesignHints> extends Job
    {
        private final Class<H> cls;
        private final File saoFile;
        private final String designName;

        private GenFsmJob(Class<H> cls, File saoFile, String designName)
        {
            super("Dump SV Design", User.getUserTool(), Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.cls = cls;
            this.saoFile = saoFile;
            this.designName = designName;
        }

        @Override
        public boolean doIt() throws JobException
        {
            try
            {
                DesignHints designHints = cls.newInstance();
                ACL2Reader sr = new ACL2Reader(saoFile);
                DesignExt design = new DesignExt(sr.root, designHints);
                GenFsmNew gen = new GenFsmNew(designHints);
                File outDir = saoFile.getParentFile();
                gen.gen(designName, design, outDir);
            } catch (InstantiationException | IllegalAccessException | IOException e)
            {
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        }
    }

}
