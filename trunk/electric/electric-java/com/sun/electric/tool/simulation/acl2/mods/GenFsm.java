/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenFsm.java
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

import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.SvexVar;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generate next-state function of control block as ACL2 text
 */
public abstract class GenFsm
{

    private static final Vec4 X16 = Vec4.valueOf(BigInteger.valueOf(0xffff), BigInteger.valueOf(0));

    private final String projName;
    private final String topName;
    private final Set<Wire> knownWires = new HashSet<>();
    private final List<Wire> sortedWires = new ArrayList<>();
    private final Map<Wire, Map<Lhs, Driver>> wireDrivers = new HashMap<>();
    private final Map<Wire, Set<Wire>> wireDependencies = new HashMap<>();

    protected abstract boolean ignore_wire(Wire w);

    private static Wire searchWire(Module m, String name)
    {
        for (Wire w : m.wires)
        {
            if (w.name.impl.stringValueExact().equals(name))
            {
                return w;
            }
        }
        throw new RuntimeException();
    }

    private void topSort(Wire w)
    {
        if (knownWires.contains(w))
        {
            return;
        }
        Set<Wire> deps = wireDependencies.get(w);
        if (deps == null)
        {
            System.out.println("Unknown " + w);
        } else
        {
            for (Wire wd : deps)
            {
                topSort(wd);
            }
        }
        knownWires.add(w);
        sortedWires.add(w);
    }

    private void genCurrentState(Name instname, int ffWidth, Lhrange lr, int rsh)
    {
        Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
        SVarExt svar = atomVar.name;
        Wire w = svar.wire;
        if (lr.w != w.width)
        {
            throw new UnsupportedOperationException();
        }
        String s = w.name.impl.stringValueExact();
        assert svar.delay == 0;
        assert !svar.nonblocking;
        s();
        s("(define " + s + "-ext");
        sb(" ((st " + projName + "-st-p)");
        sb("  (in " + projName + "-in-p))");
        e();
        s(" :returns (" + s + " unsigned-" + w.width + "-p)");
        s(" (declare (ignore in))");
        String ff = "(" + projName + "-st->" + instname.toLispString() + " st)";
        if (w.width == ffWidth)
        {
            // ff
        } else if (w.width == 1)
        {
            ff = "(logbit " + rsh + " " + ff + ")";
        } else if (rsh == 0)
        {
            ff = "(loghead " + w.width + " " + ff + ")";
        } else
        {
            ff = "(loghead " + w.width + " (logtail " + rsh + " " + ff + "))";
        }
        s(" " + ff + ")");
        e();
    }

    protected void showSvex(Svex sv)
    {
        if (sv instanceof SvexQuote)
        {
            Vec4 val = ((SvexQuote)sv).val;
            if (val.isVec2())
            {
                s("" + ((Vec2)val).getVal());
            } else
            {
                Util.check(val.equals(Vec4.X) || val.equals(X16));
                s("0");
            }
        } else if (sv instanceof SvexVar)
        {
            Wire w = ((SVarExt)((SvexVar)sv).svar).wire;
            String s = w.name.impl.stringValueExact();
            s("(" + s + "-ext st in)");
        } else if (sv instanceof SvexCall)
        {
            String nm = symbol_name(((SvexCall)sv).fun.fn).stringValueExact();
            String lnm = "<" + nm + ">";
            boolean boolBit = false;
            boolean neBit = false;
            switch (nm)
            {
                case "ID":
                    break;
                case "BITSEL":
                    break;
                case "UNFLOAT":
                    break;
                case "BITNOT":
                    lnm = "lognot";
                    break;
                case "BITAND":
                    lnm = "logand";
                    break;
                case "BITOR":
                    lnm = "logior";
                    break;
                case "BITXOR":
                    lnm = "logxor";
                    break;
                case "RES":
                    break;
                case "RESAND":
                    break;
                case "RESOR":
                    break;
                case "OVERRIDE":
                    break;
                case "ONP":
                    break;
                case "OFFP":
                    break;
                case "UAND":
                    break;
                case "UOR":
                    break;
                case "UXOR":
                    break;
                case "ZEROX":
                    lnm = "loghead";
                    break;
                case "SIGNX":
                    lnm = "loghead";
                    break;
                case "CONCAT":
                    lnm = "logapp";
                    break;
                case "BLKREV":
                    break;
                case "RSH":
                    break;
                case "LSH":
                    break;
                case "+":
                    lnm = "+";
                    break;
                case "B-":
                    lnm = "-";
                    break;
                case "U-":
                    break;
                case "*":
                    lnm = "*";
                    break;
                case "/":
                    break;
                case "%":
                    break;
                case "XDET":
                    break;
                case "<":
                    lnm = "<";
                    boolBit = true;
                    break;
                case "==":
                    lnm = "=";
                    boolBit = true;
                    break;
                case "===":
                    break;
                case "==?":
                    break;
                case "SAFER-==?":
                    break;
                case "==??":
                    break;
                case "CLOG2":
                    break;
                case "POW":
                    break;
                case "?":
                    lnm = "if";
                    neBit = true;
                    break;
                case "?*":
                    break;
                case "BIT?":
                    break;
                case "PARTSEL":
                    lnm = "partsel";
                    break;
                case "PARTINST":
                    break;
                default:
                    Util.check(false);
            }
            if (boolBit)
            {
                s("(bool->bit (" + lnm);
            } else
            {
                s("(" + lnm);
            }
            b();
            for (Svex arg : ((SvexCall)sv).getArgs())
            {
                if (neBit)
                {
                    s("(not (= ");
                    showSvex(arg);
                    out.print(" 0))");
                    neBit = false;
                } else
                {
                    showSvex(arg);
                }
            }
            out.print(')');
            if (boolBit)
            {
                out.print(')');
            }
            e();
        } else
        {
            assert false;
        }
    }

    protected void genDummyWireBody(String s, Map<Lhs, Driver> drv)
    {
        showSvex(drv.values().iterator().next().svex);
    }

    private void genDummyWire(Wire w)
    {
        String s = w.name.impl.stringValueExact();
        s();
        s("(define " + s + "-ext");
        sb("((st " + projName + "-st-p)");
        sb("(in " + projName + "-in-p))");
        e();
        s(" :returns (" + s + " unsigned-" + w.width + "-p)");
//        s(" (declare (ignore st in))");
        s(";");
        Set<Wire> deps = wireDependencies.get(w);
        for (Wire wd : deps)
        {
            out.print(" " + wd);
        }
        Map<Lhs, Driver> drv = wireDrivers.get(w);
        if (drv != null && !drv.isEmpty())
        {
            genDummyWireBody(s, drv);
        } else
        {
            s("0");
        }
        out.print(')');
        e();
    }

    protected String getGatedClock(String s)
    {
        return null;
    }

    protected String[] getGatedClocks()
    {
        return new String[]
        {
        };
    }

    private void genNextFlipFlop(Name instname, int ffWidth, Lhs r)
    {
        String s = instname.impl.stringValueExact();
        s();
        s("(define " + s + "-next");
        sb("((st " + projName + "-st-p)");
        sb("(in " + projName + "-in-p))");
        e();
        s(":returns (" + s + " unsigned-" + ffWidth + "-p)");
        String clk = getGatedClock(s);
        if (clk != null)
        {
            s("(if (= (" + clk + "-ext st in) 1)");
            sb("(" + projName + "-st->" + s + " st)");
            e();
        }
        int rsh = ffWidth;
        for (int i = 0; i < r.ranges.size() - 1; i++)
        {
            Lhrange lr = r.ranges.get(r.ranges.size() - 1 - i);
            rsh -= lr.w;
            sb("(logapp " + rsh);
        }
        assert rsh == r.ranges.get(0).w;
        for (int i = r.ranges.size() - 1; i >= 0; i--)
        {
            Lhrange lr = r.ranges.get(r.ranges.size() - 1 - i);
            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
            String atomStr = "(" + atomVar.name.wire.name.toLispString() + "-ext st in)";
            if (atomVar.rsh != 0)
            {
                atomStr = "(logtail " + atomVar.rsh + " " + atomStr + ")";
            }
            if (i == 0 && lr.w != atomVar.name.wire.width - atomVar.rsh)
            {
                atomStr = "(loghead " + lr.w + " " + atomStr + ")";
            }
            s(atomStr);
            if (i != r.ranges.size() - 1)
            {
                out.print(")");
            }
            if (i == 0)
            {
                out.print(")");
                if (clk != null)
                {
                    out.print(")");
                }
            }
            out.print(" ; " + lr);
            e();
        }
    }

    protected abstract boolean isFlipFlopIn(String modname, String wireName);

    protected abstract boolean isFlipFlopOut(String modname, String wireName);

    private void genNextState(Module m)
    {
        s("(define next-state");
        sb("((st " + projName + "-st-p)");
        sb("(in " + projName + "-in-p))");
        e();
        s(":returns (nst " + projName + "-st-p)");
        s("(make-" + projName + "-st");
        b();
        for (Map.Entry<Lhs, Lhs> e1 : m.aliaspairs.entrySet())
        {
            Lhs l = e1.getKey();
            Lhs r = e1.getValue();
            assert l.ranges.size() == 1;
            Lhrange lr = l.ranges.get(0);
            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
            assert atomVar.rsh == 0;
            SVarExt svar = atomVar.name;
            assert svar.delay == 0;
            assert !svar.nonblocking;
            ModInst inst = svar.inst;
            if (isFlipFlopOut(inst.modname.impl.stringValueExact(),
                svar.wire.name.impl.stringValueExact()))
            {
                String s = inst.instname.impl.stringValueExact();
                s(":" + s + " (" + s + "-next st in)");
            }
        }
        out.print("))");
        e();
        e();
    }

    public void gen(Module m)
    {
        genAux();
        s();

        s("; Primary inputs");
        for (Wire w : m.wires)
        {
            if (!w.assigned && w.used && !ignore_wire(w))
            {
                knownWires.add(w);
                genInput(w);
            }
        }
        s();

        s("; Current state");
        for (Map.Entry<Lhs, Lhs> e1 : m.aliaspairs.entrySet())
        {
            Lhs l = e1.getKey();
            Lhs r = e1.getValue();
            assert l.ranges.size() == 1;
            Lhrange lr = l.ranges.get(0);
            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
            assert atomVar.rsh == 0;
            SVarExt svar = atomVar.name;
            assert svar.delay == 0;
            assert !svar.nonblocking;
            ModInst inst = svar.inst;
            int ffWidth = svar.wire.width;
            if (isFlipFlopOut(inst.modname.impl.stringValueExact(),
                svar.wire.name.impl.stringValueExact()))
            {
                int rsh = 0;
                for (Lhrange lr1 : r.ranges)
                {
                    atomVar = (Lhatom.Var)lr1.atom;
                    svar = atomVar.name;
                    assert svar.delay == 0;
                    assert !svar.nonblocking;
                    knownWires.add(svar.wire);
                    genCurrentState(inst.instname, ffWidth, lr1, rsh);
                    rsh += lr1.w;
                }
            }
        }
        out.println();

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
                if (svar.inst != null)
                {
                    System.out.println("Inst " + svar);
                    continue;
                }
                Map<Lhs, Driver> drv = wireDrivers.get(svar.wire);
                if (drv == null)
                {
                    drv = new LinkedHashMap<>();
                    wireDrivers.put(svar.wire, drv);
                }
                drv.put(l, d);
                Set<Wire> dep = wireDependencies.get(svar.wire);
                if (dep == null)
                {
                    dep = new LinkedHashSet<>();
                    wireDependencies.put(svar.wire, dep);
                } else
                {
                    System.out.println("Twice " + svar.wire);
                }
                Set<SVarExt> deps = d.svex.collectVars(SVarExt.class);
                for (SVarExt sv : deps)
                {
                    dep.add(sv.wire);
                }
            }
        }

        for (Map.Entry<Lhs, Lhs> e1 : m.aliaspairs.entrySet())
        {
            Lhs l = e1.getKey();
            Lhs r = e1.getValue();
            assert l.ranges.size() == 1;
            Lhrange lr = l.ranges.get(0);
            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
            assert atomVar.rsh == 0;
            SVarExt svar = atomVar.name;
            assert svar.delay == 0;
            assert !svar.nonblocking;
            ModInst inst = svar.inst;
            if (isFlipFlopIn(inst.modname.impl.stringValueExact(),
                svar.wire.name.impl.stringValueExact()))
            {
                for (Lhrange lr1 : r.ranges)
                {
                    atomVar = (Lhatom.Var)lr1.atom;
                    svar = atomVar.name;
                    assert svar.delay == 0;
                    assert !svar.nonblocking;
                    topSort(svar.wire);
                }
            }
        }
        for (String clock : getGatedClocks())
        {
            topSort(searchWire(m, clock));
        }

        s("; Wires");
        for (Wire w : sortedWires)
        {
            genDummyWire(w);
        }
        s();

        s("; New state");
        for (Map.Entry<Lhs, Lhs> e1 : m.aliaspairs.entrySet())
        {
            Lhs l = e1.getKey();
            Lhs r = e1.getValue();
            assert l.ranges.size() == 1;
            Lhrange lr = l.ranges.get(0);
            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
            assert atomVar.rsh == 0;
            SVarExt svar = atomVar.name;
            assert svar.delay == 0;
            assert !svar.nonblocking;
            ModInst inst = svar.inst;
            int ffWidth = svar.wire.width;
            if (isFlipFlopIn(inst.modname.impl.stringValueExact(),
                svar.wire.name.impl.stringValueExact()))
            {
                genNextFlipFlop(inst.instname, ffWidth, r);
            }
        }
        s();

        genNextState(m);
        s();
    }

    public void gen(String saoFileName, String outFileName) throws IOException
    {
        ACL2Reader sr = new ACL2Reader(saoFileName);
        Design design = new Design(sr.root);
        ModName nm = null;
        Module m = null;
        for (Map.Entry<ModName, Module> e : design.downTop.entrySet())
        {
            ACL2Object mn = e.getKey().impl;
            if (stringp(mn).bool() && mn.stringValueExact().equals(topName))
            {
                assert nm == null && m == null;
                nm = e.getKey();
                m = e.getValue();
            }
        }
        try (PrintStream out = new PrintStream(outFileName))
        {
            this.out = out;
            gen(m);
        } finally {
            this.out = null;
        }
    }

    protected GenFsm(String projName, String topName)
    {
        this.projName = projName;
        this.topName = topName;
    }

    protected PrintStream out;
    int indent = 0;

    protected void s()
    {
        out.println();
        for (int i = 0; i < indent; i++)
        {
            out.print(' ');
        }
    }

    protected void b()
    {
        indent++;
    }

    protected void e()
    {
        indent--;
    }

    protected void s(String s)
    {
        s();
        out.print(s);
    }

    protected void sb(String s)
    {
        b();
        s(s);
    }

    protected void genAux()
    {
        s("(in-package \"ACL2\")");
        s();
        s("(include-book \"" + projName + "-state\")");
        s("(local (include-book \"ihs/logops-lemmas\" :dir :system))");

        s();
        s("(define partsel");
        sb("((lsb natp)");
        sb("(width natp)");
        s("(in integerp))");
        e();
        s("(loghead width (logtail lsb in)))");
        e();
        s();
        s("(local (in-theory (disable unsigned-byte-p loghead logtail logapp lognot logior (tau-system))))");
        s();
    }

    protected void genInput(Wire w)
    {
        String s = w.name.impl.stringValueExact();
        s();
        s("(define " + s + "-ext");
        sb("((st " + projName + "-st-p)");
        sb("(in " + projName + "-in-p))");
        e();
        s(":returns (" + s + " unsigned-" + w.width + "-p)");
        s("(declare (ignore st))");
        s("(" + projName + "-in->" + s + " in))");
        e();
    }
}
