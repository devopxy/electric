/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Module.java
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

import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SV module.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODULE>.
 */
public class Module implements Svar.Builder<SVarExt>
{
    public final ModName modName;
    public final List<Wire> wires = new ArrayList<>();
    public final List<ModInst> insts = new ArrayList<>();
    public final Map<Lhs, Driver> assigns = new LinkedHashMap<>();
    public final Map<Lhs, Lhs> aliaspairs = new LinkedHashMap<>();

    final Map<Name, Wire> wiresIndex = new HashMap<>();
    final Map<Name, ModInst> instsIndex = new HashMap<>();
    final Map<ACL2Object, Svex> svexCache = new HashMap<>();
    int useCount;

    Set<Wire> combinationalInputs0;
    Set<Wire> combinationalInputs1;

    Module(ModName modName, ACL2Object impl, Map<ModName, Module> downTop)
    {
        this.modName = modName;
        List<ACL2Object> fields = Util.getList(impl, true);
        Util.check(fields.size() == 4);
        ACL2Object pair;

        pair = fields.get(0);
        Util.check(car(pair).equals(Util.SV_WIRES));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            Wire w = new Wire(this, o);
            wires.add(w);
            Wire old = wiresIndex.put(w.name, w);
            Util.check(old == null);
        }

        pair = fields.get(1);
        Util.check(car(pair).equals(Util.SV_INSTS));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            ModInst mi = new ModInst(this, o, downTop);
            insts.add(mi);
            ModInst old = instsIndex.put(mi.instname, mi);
            Util.check(old == null);
        }
        pair = fields.get(2);
        Util.check(car(pair).equals(Util.SV_ASSIGNS));
        int driverCount = 0;
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            pair = o;
            Lhs lhs = new Lhs(this, car(pair));
            Driver drv = new Driver(this, cdr(pair), "dr" + driverCount++);
            Driver old = assigns.put(lhs, drv);
            Util.check(old == null);
            for (Lhrange lhr : lhs.ranges)
            {
                Util.check(lhr.atom instanceof Lhatom.Var);
                SVarExt svar = ((Lhatom.Var)lhr.atom).name;
                if (svar instanceof SVarExt.LocalWire)
                {
                    ((SVarExt.LocalWire)svar).wire.addDriver(lhr, drv);
                } else
                {
                    ((SVarExt.PortInst)svar).addDriver(drv);
                }
            }
            lhs.markAssigned(BigIntegerUtil.MINUS_ONE);
            drv.markUsed();
        }

        pair = fields.get(3);
        Util.check(car(pair).equals(Util.SV_ALIASPAIRS));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            pair = o;
            Lhs lhs = new Lhs(this, car(pair));
            Util.check(lhs.ranges.size() == 1);
            Lhrange lhsRange = lhs.ranges.get(0);
            Util.check(lhsRange.atom instanceof Lhatom.Var);
            Lhatom.Var lhsAtom = (Lhatom.Var)lhsRange.atom;
            Util.check(lhsAtom.rsh == 0);
            SVarExt lhsVar = lhsAtom.name;
            Util.check(lhsVar.getDelay() == 0 && !lhsVar.isNonblocking());
            Lhs rhs = new Lhs(this, cdr(pair));
            Lhs old = aliaspairs.put(lhs, rhs);
            Util.check(old == null);
            Util.check(lhs.size() == rhs.size());
            if (lhsVar instanceof SVarExt.PortInst)
            {
                SVarExt.PortInst pi = (SVarExt.PortInst)lhsVar;
                Util.check(lhsRange.w == pi.wire.width);
                for (Lhrange rhsRange : rhs.ranges)
                {
                    Util.check(((Lhatom.Var)rhsRange.atom).name instanceof SVarExt.LocalWire);
                    SVarExt.LocalWire lw = (SVarExt.LocalWire)((Lhatom.Var)rhsRange.atom).name;
                    if (pi.wire.isAssigned())
                    {
                        lw.wire.addDriver(rhsRange, pi);
                    }
                }
                if (pi.wire.isAssigned())
                {
                    pi.addSource(rhs);
                } else
                {
                    pi.addDriver(rhs);
                }
                BigInteger assignedBits = pi.wire.getAssignedBits();
                rhs.markAssigned(assignedBits);
                if (pi.wire.used)
                {
                    rhs.markUsed();
                }
            } else
            {
                SVarExt.LocalWire lw = (SVarExt.LocalWire)lhsVar;
                if (stringp(lw.name).bool())
                {
                    Util.check(rhs.ranges.size() == 1);
                    SVarExt.PortInst rhsPi = (SVarExt.PortInst)((Lhatom.Var)rhs.ranges.get(0).atom).name;
                    Util.check(rhsPi.inst.modname.isCoretype);
//                    System.out.println("lw string " + lw + " in " + modName);
                } else if (integerp(lw.name).bool())
                {
                    Util.check(modName.isCoretype);
                } else
                {
                    Util.check(false);
                }
            }
        }
    }

    void markTop()
    {
        useCount = 1;
        for (Wire w : wires)
        {
            if (w.width == 1 && w.low_idx == 0)
            {
                w.global = w.name.toString();
            }
        }
    }

    void markDown(Map<String, Integer> globalCounts)
    {
        for (ModInst mi : insts)
        {
            mi.proto.useCount += useCount;
        }
        for (Map.Entry<Lhs, Lhs> e1 : aliaspairs.entrySet())
        {
            Lhs lhs = e1.getKey();
            Lhs rhs = e1.getValue();
            if (rhs.ranges.size() == 1
                && rhs.ranges.get(0).w == 1
                && rhs.ranges.get(0).atom instanceof Lhatom.Var
                && ((Lhatom.Var)rhs.ranges.get(0).atom).rsh == 0)
            {
                SVarExt svar = ((Lhatom.Var)rhs.ranges.get(0).atom).name;
                if (svar instanceof SVarExt.LocalWire)
                {
                    Wire w = ((SVarExt.LocalWire)svar).wire;
                    if (w.isGlobal()
                        && lhs.ranges.size() == 1
                        && lhs.ranges.get(0).w == 1
                        && lhs.ranges.get(0).atom instanceof Lhatom.Var
                        && ((Lhatom.Var)rhs.ranges.get(0).atom).rsh == 0)
                    {
                        SVarExt svar1 = ((Lhatom.Var)lhs.ranges.get(0).atom).name;
                        if (svar1 instanceof SVarExt.PortInst)
                        {
                            ((SVarExt.PortInst)svar1).wire.markGlobal(w.global);
                        }
                    }
                }
            }
        }
        for (Wire w : wires)
        {
            if (w.isGlobal())
            {
                Integer count = globalCounts.get(w.global);
                globalCounts.put(w.global, count == null ? 1 : count + 1);
            }
        }
    }

    void computeCombinationalInputs(String global)
    {
        Map<Object, Set<Object>> graph0 = computeDepsGraph(global, false);
        Map<Object, Set<Object>> closure0 = closure(graph0);
        combinationalInputs0 = new LinkedHashSet<>();
        for (Wire out : wires)
        {
            if (out.exported && out.isAssigned())
            {
                for (Object dep : closure0.get(out))
                {
                    if (dep instanceof Wire)
                    {
                        Wire in = (Wire)dep;
                        if (in.exported && !in.isAssigned())
                        {
                            combinationalInputs0.add(in);
                        }
                    }
                }
            }
        }

        Map<Object, Set<Object>> graph1 = computeDepsGraph(global, false);
        Map<Object, Set<Object>> closure1 = closure(graph1);
        combinationalInputs1 = new LinkedHashSet<>();
        for (Wire out : wires)
        {
            if (out.exported && out.isAssigned())
            {
                for (Object dep : closure1.get(out))
                {
                    if (dep instanceof Wire)
                    {
                        Wire in = (Wire)dep;
                        if (in.exported && !in.isAssigned())
                        {
                            combinationalInputs1.add(in);
                        }
                    }
                }
            }
        }
    }

    Map<Object, Set<Object>> computeDepsGraph(String global, boolean clkOne)
    {
        Map<Object, Set<Object>> graph = new LinkedHashMap<>();
        for (Wire w : wires)
        {
            if (w.exported && w.isAssigned())
            {
                Set<Object> outputDeps = new LinkedHashSet<>();
                addWireDeps(new SVarExt.LocalWire(this, w.name.impl, 0), BigIntegerUtil.MINUS_ONE, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        for (ModInst mi : insts)
        {
            Set<Object> instDeps = new LinkedHashSet<>();
            for (SVarExt.PortInst pi : mi.portInsts.values())
            {
                Set<Wire> combinationalInputs = clkOne ? mi.proto.combinationalInputs1 : mi.proto.combinationalInputs0;
                if (combinationalInputs.contains(pi.wire))
                {
                    if (pi.driver instanceof Driver)
                    {
                        instDeps.add(((Driver)pi.driver).name);
                    } else
                    {
                        for (Lhrange lr : ((Lhs)pi.driver).ranges)
                        {
                            SVarExt.LocalWire lw = (SVarExt.LocalWire)((Lhatom.Var)lr.atom).name;
                            addWireDeps(lw, BigIntegerUtil.MINUS_ONE, instDeps);
                        }
                    }
                }
            }
            graph.put(mi, instDeps);
        }
        Map<Svar, Vec4> patchEnv = makePatchEnv(global, clkOne ? Vec2.ONE : Vec2.ZERO);
        Map<SvexCall, SvexCall> patchMemoize = new HashMap<>();
        for (Map.Entry<Lhs, Driver> e1 : assigns.entrySet())
        {
            Lhs l = e1.getKey();
            Driver d = e1.getValue();
            assert !l.ranges.isEmpty();
            Svex patched = d.svex.patch(patchEnv, patchMemoize);
            Map<SVarExt.LocalWire, BigInteger> varsWithMasks = patched.collectVarsWithMasks(BigIntegerUtil.MINUS_ONE, SVarExt.LocalWire.class);
            Set<Object> driverDeps = new LinkedHashSet<>();
            for (Map.Entry<SVarExt.LocalWire, BigInteger> e2 : varsWithMasks.entrySet())
            {
                SVarExt.LocalWire lw = e2.getKey();
                BigInteger mask = e2.getValue();
                if (mask == null)
                {
                    mask = BigInteger.ZERO;
                }
                addWireDeps(lw, mask, driverDeps);
            }
            graph.put(d.name, driverDeps);
        }
        return graph;
    }

    private Map<Svar, Vec4> makePatchEnv(String global, Vec4 globalVal)
    {
        Map<Svar, Vec4> env = new HashMap<>();
        for (Wire w : wires)
        {
            if (w.isGlobal() && w.global.equals(global))
            {
                env.put(new SVarExt.LocalWire(this, w.name.impl, 0), globalVal);
            }
        }
        return env;
    }

    private void addWireDeps(SVarExt.LocalWire lw, BigInteger mask, Set<Object> deps)
    {
        if (lw.delay != 0)
        {
            return;
        }
        if (!lw.wire.isAssigned())
        {
            deps.add(lw.wire);
            return;
        }
        for (Map.Entry<Lhrange, Object> e3 : lw.wire.drivers.entrySet())
        {
            Lhrange lhr = e3.getKey();
            Object driver = e3.getValue();
            Lhatom.Var atomVar = (Lhatom.Var)lhr.atom;
            if (atomVar.name.getDelay() == 0
                && BigIntegerUtil.logheadMask(lhr.w).shiftLeft(atomVar.rsh).and(mask).signum() != 0)
            {
                if (driver instanceof Driver)
                {
                    deps.add(((Driver)driver).name);
                } else
                {
                    deps.add(((SVarExt.PortInst)driver).inst);
                }
            }
        }
    }

    Map<Object, Set<Object>> closure(Map<Object, Set<Object>> rel)
    {
        Map<Object, Set<Object>> closure = new HashMap<>();
        Set<Object> visited = new HashSet<>();
        for (Object key : rel.keySet())
        {
            closure(key, rel, closure, visited);
        }
        Util.check(closure.size() == visited.size());
        return closure;
    }

    private Set<Object> closure(Object top,
        Map<Object, Set<Object>> rel,
        Map<Object, Set<Object>> closure,
        Set<Object> visited)
    {
        Set<Object> ret = closure.get(top);
        if (ret == null)
        {
            boolean ok = visited.add(top);
            if (!ok)
            {
                System.out.println("CombinationalLoop!!! in " + top + " of " + modName);
                return Collections.singleton(top);
            }
            Set<Object> dep = rel.get(top);
            if (dep == null)
            {
                ret = Collections.singleton(top);
            } else
            {
                ret = new LinkedHashSet<>();
                for (Object svar : dep)
                {
                    ret.addAll(closure(svar, rel, closure, visited));
                }
            }
            closure.put(top, ret);
        }
        return ret;
    }

    @Override
    public SVarExt newVar(ACL2Object name, int delay, boolean nonblocking)
    {
        Util.check(!nonblocking);
        if (consp(name).bool())
        {
            Util.check(delay == 0);
            ModInst inst = instsIndex.get(new Name(car(name)));
            return inst.newPortInst(name);
//            Module sm = inst.proto;
//            Wire wire = sm.wiresIndex.get(new Name(cdr(name)));
//            return new SVarExt.PortInst(this, name);
        } else
        {
            return new SVarExt.LocalWire(this, name, delay);
        }
    }
}
