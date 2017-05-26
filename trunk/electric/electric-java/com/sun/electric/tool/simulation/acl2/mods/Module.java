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
    private static final boolean CRUDE_COMBINATIONAL_INPUTS = false;

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
            Util.check(lhs.width() == rhs.width());
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
                if (stringp(lw.name.getACL2Object()).bool())
                {
                    Util.check(rhs.ranges.size() == 1);
                    SVarExt.PortInst rhsPi = (SVarExt.PortInst)((Lhatom.Var)rhs.ranges.get(0).atom).name;
                    Util.check(rhsPi.inst.modname.isCoretype);
//                    System.out.println("lw string " + lw + " in " + modName);
                } else if (integerp(lw.name.getACL2Object()).bool())
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

    Set<Wire> getCombinationalInputs(boolean clockVal)
    {
        return clockVal ? combinationalInputs1 : combinationalInputs0;
    }

    void computeCombinationalInputs(String global)
    {
        computeDriverDeps(global, false);
        computeDriverDeps(global, true);

            combinationalInputs0 = new LinkedHashSet<>();
            combinationalInputs1 = new LinkedHashSet<>();
        if (CRUDE_COMBINATIONAL_INPUTS)
        {
            Map<Object, Set<Object>> graph0 = computeDepsGraph(false);
            Map<Object, Set<Object>> closure0 = closure(graph0);
            Map<Object, Set<Object>> graph1 = computeDepsGraph(true);
            Map<Object, Set<Object>> closure1 = closure(graph1);
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

        Map<Object, Set<Object>> fineGraph0 = computeFineDepsGraph(false);
        Map<Object, Set<Object>> fineClosure0 = closure(fineGraph0);
        Map<Object, Set<Object>> fineGraph1 = computeFineDepsGraph(true);
        Map<Object, Set<Object>> fineClosure1 = closure(fineGraph1);
        for (Wire out : wires)
        {
            if (out.exported && out.isAssigned())
            {
                out.setFineDeps(false, fineClosure0);
                out.setFineDeps(true, fineClosure1);
                if (!CRUDE_COMBINATIONAL_INPUTS) {
                    for (Map<SVarExt.LocalWire,BigInteger> mapForBit: out.fineDeps0) {
                        for (Map.Entry<SVarExt.LocalWire,BigInteger> e: mapForBit.entrySet()) {
                            SVarExt.LocalWire lw = e.getKey();
                            BigInteger mask = e.getValue();
                            if (mask.signum() != 0) {
                                combinationalInputs0.add(lw.wire);
                            }
                        }
                    }
                    for (Map<SVarExt.LocalWire,BigInteger> mapForBit: out.fineDeps1) {
                        for (Map.Entry<SVarExt.LocalWire,BigInteger> e: mapForBit.entrySet()) {
                            SVarExt.LocalWire lw = e.getKey();
                            BigInteger mask = e.getValue();
                            if (mask.signum() != 0) {
                                combinationalInputs1.add(lw.wire);
                            }
                        }
                    }
                }
            }
        }
    }

    private void computeDriverDeps(String global, boolean clkOne)
    {
        Map<Svar, Vec4> patchEnv = makePatchEnv(global, clkOne ? Vec2.ONE : Vec2.ZERO);
        Map<SvexCall, SvexCall> patchMemoize = new HashMap<>();
        for (Map.Entry<Lhs, Driver> e1 : assigns.entrySet())
        {
            Lhs l = e1.getKey();
            Driver d = e1.getValue();
            d.computeDeps(l.width(), clkOne, patchEnv, patchMemoize);
        }
    }

    private Map<Svar, Vec4> makePatchEnv(String global, Vec4 globalVal)
    {
        Map<Svar, Vec4> env = new HashMap<>();
        for (Wire w : wires)
        {
            if (w.isGlobal() && w.global.equals(global))
            {
                env.put(w.curVar, globalVal);
            }
        }
        return env;
    }

    Map<Object, Set<Object>> computeDepsGraph(boolean clockHigh)
    {
        Map<Object, Set<Object>> graph = new LinkedHashMap<>();
        for (Wire w : wires)
        {
            if (w.exported && w.isAssigned())
            {
                Set<Object> outputDeps = new LinkedHashSet<>();
                addWireDeps(w.curVar, BigIntegerUtil.MINUS_ONE, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        for (ModInst mi : insts)
        {
            Set<Object> instDeps = new LinkedHashSet<>();
            for (SVarExt.PortInst pi : mi.portInsts.values())
            {
                Set<Wire> combinationalInputs = mi.proto.getCombinationalInputs(clockHigh);
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
        for (Map.Entry<Lhs, Driver> e1 : assigns.entrySet())
        {
            Lhs l = e1.getKey();
            Driver d = e1.getValue();
            assert !l.ranges.isEmpty();
            Set<Object> driverDeps = new LinkedHashSet<>();
            for (Map.Entry<SVarExt.LocalWire, BigInteger> e2 : d.getCrudeDeps(clockHigh).entrySet())
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

    Map<Object, Set<Object>> computeFineDepsGraph(boolean clockHigh)
    {
        Map<Object, Set<Object>> graph = new LinkedHashMap<>();
        for (Wire w : wires)
        {
            if (w.isAssigned())
            {
                for (Map.Entry<Lhrange, Object> e : w.drivers.entrySet())
                {
                    Lhrange lhr = e.getKey();
                    Object driver = e.getValue();
                    int rsh = ((Lhatom.Var)lhr.atom).rsh;

                    if (driver instanceof Driver)
                    {
                        for (int i = 0; i < lhr.w; i++)
                        {
                            WireBit wb = new WireBit(w, rsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            addDriverFineDeps((Driver)driver, clockHigh, lhr.lsh + i, dep);
                            graph.put(wb, dep);
                        }
                    } else
                    {
                        SVarExt.PortInst piOut = (SVarExt.PortInst)driver;
                        ModInst inst = piOut.inst;
                        for (int i = 0; i < lhr.w; i++)
                        {
                            WireBit wb = new WireBit(w, rsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            Map<SVarExt.LocalWire, BigInteger> masks = piOut.wire.getFineDeps(clockHigh).get(lhr.lsh + i);
                            for (Map.Entry<SVarExt.LocalWire, BigInteger> e1 : masks.entrySet())
                            {
                                SVarExt.LocalWire lw = e1.getKey();
                                BigInteger mask = e1.getValue();
                                SVarExt.PortInst piIn = inst.portInsts.get(lw.wire.name);
                                if (piIn == null)
                                {
                                    System.out.println(lw.wire.name + " was not found in " + inst.instname + " of " + modName);
                                    continue;
                                }
                                if (piIn.driver instanceof Driver)
                                {
                                    addDriverFineDeps((Driver)piIn.driver, clockHigh, mask, dep);
                                } else
                                {
                                    int lsh = 0;
                                    for (Lhrange lhrIn : ((Lhs)piIn.driver).ranges)
                                    {
                                        Lhatom.Var atomVar = (Lhatom.Var)lhrIn.atom;
                                        SVarExt.LocalWire lwIn = (SVarExt.LocalWire)atomVar.name;
                                        for (int j = 0; j < lhrIn.w; j++)
                                        {
                                            if (mask.testBit(lsh + j))
                                            {
                                                dep.add(new WireBit(lwIn.wire, atomVar.rsh + j));
                                            }
                                        }
                                        lsh += lhrIn.w;
                                    }
                                }
                            }
                            graph.put(wb, dep);
                        }
                    }
                }
                Set<Object> outputDeps = new LinkedHashSet<>();
                addWireDeps(w.curVar, BigIntegerUtil.MINUS_ONE, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        return graph;
    }

    void addDriverFineDeps(Driver dr, boolean clockHigh, BigInteger maskOut, Set<Object> dep)
    {
        assert maskOut.signum() >= 0;
        for (int bit = 0; bit < maskOut.bitLength(); bit++)
        {
            if (maskOut.testBit(bit))
            {
                addDriverFineDeps(dr, clockHigh, bit, dep);
            }
        }
    }

    void addDriverFineDeps(Driver dr, boolean clockHigh, int bit, Set<Object> dep)
    {
        Map<SVarExt.LocalWire, BigInteger> masks = dr.getFineDeps(clockHigh).get(bit);
        for (Map.Entry<SVarExt.LocalWire, BigInteger> e1 : masks.entrySet())
        {
            SVarExt.LocalWire lw = e1.getKey();
            BigInteger maskIn = e1.getValue();
            if (maskIn != null && lw.delay == 0)
            {
                assert maskIn.signum() >= 0;
                for (int j = 0; j < maskIn.bitLength(); j++)
                {
                    if (maskIn.testBit(j))
                    {
                        dep.add(new WireBit(lw.wire, j));
                    }
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
            Wire wire = wiresIndex.get(new Name(name));
            return wire.getVar(delay);
        }
    }

    static class WireBit
    {
        final Wire wire;
        final int bit;

        WireBit(Wire wire, int bit)
        {
            this.wire = wire;
            this.bit = bit;
        }

        @Override
        public String toString()
        {
            return wire.toString(BigInteger.ONE.shiftLeft(bit));
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof WireBit)
            {
                WireBit that = (WireBit)o;
                return this.wire.equals(that.wire) && this.bit == that.bit;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 29 * hash + wire.hashCode();
            hash = 29 * hash + this.bit;
            return hash;
        }
    }
}
