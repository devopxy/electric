/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ModuleExt.java
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
public class ModuleExt implements Svar.Builder<SVarExt>
{
    private static final boolean CRUDE_COMBINATIONAL_INPUTS = false;

    public final ModName modName;
    public final List<WireExt> wires = new ArrayList<>();
    public final List<ModInstExt> insts = new ArrayList<>();
    public final Map<Lhs<SVarExt>, DriverExt> assigns = new LinkedHashMap<>();
    public final Map<Lhs<SVarExt>, Lhs<SVarExt>> aliaspairs = new LinkedHashMap<>();

    final Map<Name, WireExt> wiresIndex = new HashMap<>();
    final Map<Name, ModInstExt> instsIndex = new HashMap<>();
    final Map<ACL2Object, Svex<SVarExt>> svexCache = new HashMap<>();
    int useCount;

    Set<WireExt> combinationalInputs0;
    Set<WireExt> combinationalInputs1;

    ModuleExt(ModName modName, ACL2Object impl, Map<ModName, ModuleExt> downTop)
    {
        this.modName = modName;
        List<ACL2Object> fields = Util.getList(impl, true);
        Util.check(fields.size() == 4);
        ACL2Object pair;

        pair = fields.get(0);
        Util.check(car(pair).equals(Util.SV_WIRES));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            Wire wire = new Wire(o);
            WireExt w = new WireExt(this, wire);
            wires.add(w);
            WireExt old = wiresIndex.put(w.getName(), w);
            Util.check(old == null);
        }

        pair = fields.get(1);
        Util.check(car(pair).equals(Util.SV_INSTS));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            ModInst modInst = new ModInst(o);
            ModInstExt mi = new ModInstExt(this, modInst, downTop);
            insts.add(mi);
            ModInstExt old = instsIndex.put(mi.getInstname(), mi);
            Util.check(old == null);
        }
        pair = fields.get(2);
        Util.check(car(pair).equals(Util.SV_ASSIGNS));
        int driverCount = 0;
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            pair = o;
            Lhs<SVarExt> lhs = new Lhs<>(this, car(pair));
            DriverExt drv = new DriverExt(this, cdr(pair), "dr" + driverCount++);
            DriverExt old = assigns.put(lhs, drv);
            Util.check(old == null);
            int lsh = 0;
            for (Lhrange<SVarExt> lhr : lhs.ranges)
            {
                SVarExt svar = lhr.getVar();
                if (svar instanceof SVarExt.LocalWire)
                {
                    ((SVarExt.LocalWire)svar).wire.addDriver(lhr, lsh, drv);
                } else
                {
                    ((SVarExt.PortInst)svar).addDriver(drv);
                }
                lsh += lhr.getWidth();
            }
            markAssigned(lhs, BigIntegerUtil.MINUS_ONE);
            drv.markUsed();
        }

        pair = fields.get(3);
        Util.check(car(pair).equals(Util.SV_ALIASPAIRS));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            pair = o;
            Lhs<SVarExt> lhs = new Lhs<>(this, car(pair));
            Util.check(lhs.ranges.size() == 1);
            Lhrange<SVarExt> lhsRange = lhs.ranges.get(0);
            Util.check(lhsRange.getRsh() == 0);
            SVarExt lhsVar = lhsRange.getVar();
            Util.check(lhsVar.getDelay() == 0 && !lhsVar.isNonblocking());
            Lhs<SVarExt> rhs = new Lhs<>(this, cdr(pair));
            Lhs old = aliaspairs.put(lhs, rhs);
            Util.check(old == null);
            Util.check(lhs.width() == rhs.width());
            if (lhsVar instanceof SVarExt.PortInst)
            {
                SVarExt.PortInst pi = (SVarExt.PortInst)lhsVar;
                Util.check(lhsRange.getWidth() == pi.wire.getWidth());
                int lsh = 0;
                for (Lhrange rhsRange : rhs.ranges)
                {
                    Util.check(rhsRange.getVar() instanceof SVarExt.LocalWire);
                    SVarExt.LocalWire lw = (SVarExt.LocalWire)(rhsRange.getVar());
                    if (pi.wire.isAssigned())
                    {
                        lw.wire.addDriver(rhsRange, lsh, pi);
                    }
                    lsh += rhsRange.getWidth();
                }
                if (pi.wire.isAssigned())
                {
                    pi.addSource(rhs);
                    BigInteger assignedBits = pi.wire.getAssignedBits();
                    markAssigned(rhs, assignedBits);
                } else
                {
                    pi.addDriver(rhs);
                }
                if (pi.wire.used)
                {
                    for (Lhrange<SVarExt> lr : rhs.ranges)
                    {
                        SVarExt name = lr.getVar();
                        if (name != null)
                        {
                            Util.check(name instanceof SVarExt.LocalWire);
                            ((SVarExt.LocalWire)name).markUsed();
                        }
                    }
                }
            } else
            {
                SVarExt.LocalWire lw = (SVarExt.LocalWire)lhsVar;
                if (stringp(lw.name.getACL2Object()).bool())
                {
                    Util.check(rhs.ranges.size() == 1);
                    SVarExt.PortInst rhsPi = (SVarExt.PortInst)rhs.ranges.get(0).getVar();
                    Util.check(rhsPi.inst.getModname().isCoretype);
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

    public static void markAssigned(Lhs<SVarExt> lhs, BigInteger assignedBits)
    {
        for (Lhrange<SVarExt> lr : lhs.ranges)
        {
            SVarExt name = lr.getVar();
            if (name != null)
            {
                BigInteger assignedBitsRange = BigIntegerUtil.loghead(lr.getWidth(), assignedBits);
                int rsh = lr.getRsh();
                assignedBitsRange = assignedBitsRange.shiftLeft(rsh);
                if (name instanceof SVarExt.LocalWire)
                {
                    SVarExt.LocalWire lw = (SVarExt.LocalWire)name;
                    lw.wire.markAssigned(assignedBitsRange);
                } else
                {
                    SVarExt.PortInst pi = (SVarExt.PortInst)name;
                    Util.check(assignedBitsRange.signum() >= 0 && assignedBitsRange.bitLength() <= pi.wire.getWidth());
                    Util.check(assignedBitsRange.and(pi.wire.getAssignedBits()).signum() == 0);
                }
            }
            assignedBits = assignedBits.shiftRight(lr.getWidth());
        }
    }

    void markTop()
    {
        useCount = 1;
        for (WireExt w : wires)
        {
            if (w.getWidth() == 1 && w.getLowIdx() == 0)
            {
                w.global = w.getName().toString();
            }
        }
    }

    void markDown(Map<String, Integer> globalCounts)
    {
        for (ModInstExt mi : insts)
        {
            mi.proto.useCount += useCount;
        }
        for (Map.Entry<Lhs<SVarExt>, Lhs<SVarExt>> e1 : aliaspairs.entrySet())
        {
            Lhs<SVarExt> lhs = e1.getKey();
            Lhs<SVarExt> rhs = e1.getValue();
            if (rhs.ranges.size() == 1
                && rhs.ranges.get(0).getWidth() == 1
                && rhs.ranges.get(0).getVar() != null
                && rhs.ranges.get(0).getRsh() == 0)
            {
                SVarExt svar = rhs.ranges.get(0).getVar();
                if (svar instanceof SVarExt.LocalWire)
                {
                    WireExt w = ((SVarExt.LocalWire)svar).wire;
                    if (w.isGlobal()
                        && lhs.ranges.size() == 1
                        && lhs.ranges.get(0).getWidth() == 1
                        && lhs.ranges.get(0).getVar() != null
                        && rhs.ranges.get(0).getRsh() == 0)
                    {
                        SVarExt svar1 = lhs.ranges.get(0).getVar();
                        if (svar1 instanceof SVarExt.PortInst)
                        {
                            ((SVarExt.PortInst)svar1).wire.markGlobal(w.global);
                        }
                    }
                }
            }
        }
        for (WireExt w : wires)
        {
            if (w.isGlobal())
            {
                Integer count = globalCounts.get(w.global);
                globalCounts.put(w.global, count == null ? 1 : count + 1);
            }
        }
    }

    Set<WireExt> getCombinationalInputs(boolean clockVal)
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
            for (WireExt out : wires)
            {
                if (out.exported && out.isAssigned())
                {
                    for (Object dep : closure0.get(out))
                    {
                        if (dep instanceof WireExt)
                        {
                            WireExt in = (WireExt)dep;
                            if (in.exported && !in.isAssigned())
                            {
                                combinationalInputs0.add(in);
                            }
                        }
                    }
                    for (Object dep : closure1.get(out))
                    {
                        if (dep instanceof WireExt)
                        {
                            WireExt in = (WireExt)dep;
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
        for (WireExt out : wires)
        {
            if (out.exported && out.isAssigned())
            {
                out.setFineDeps(false, fineClosure0);
                out.setFineDeps(true, fineClosure1);
                if (!CRUDE_COMBINATIONAL_INPUTS)
                {
                    for (Map<SVarExt.LocalWire, BigInteger> mapForBit : out.fineDeps0)
                    {
                        for (Map.Entry<SVarExt.LocalWire, BigInteger> e : mapForBit.entrySet())
                        {
                            SVarExt.LocalWire lw = e.getKey();
                            BigInteger mask = e.getValue();
                            if (mask.signum() != 0)
                            {
                                combinationalInputs0.add(lw.wire);
                            }
                        }
                    }
                    for (Map<SVarExt.LocalWire, BigInteger> mapForBit : out.fineDeps1)
                    {
                        for (Map.Entry<SVarExt.LocalWire, BigInteger> e : mapForBit.entrySet())
                        {
                            SVarExt.LocalWire lw = e.getKey();
                            BigInteger mask = e.getValue();
                            if (mask.signum() != 0)
                            {
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
        Map<SVarExt, Vec4> patchEnv = makePatchEnv(global, clkOne ? Vec2.ONE : Vec2.ZERO);
        Map<SvexCall<SVarExt>, SvexCall<SVarExt>> patchMemoize = new HashMap<>();
        for (Map.Entry<Lhs<SVarExt>, DriverExt> e1 : assigns.entrySet())
        {
            Lhs l = e1.getKey();
            DriverExt d = e1.getValue();
            d.computeDeps(l.width(), clkOne, patchEnv, patchMemoize);
        }
    }

    private Map<SVarExt, Vec4> makePatchEnv(String global, Vec4 globalVal)
    {
        Map<SVarExt, Vec4> env = new HashMap<>();
        for (WireExt w : wires)
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
        for (WireExt w : wires)
        {
            if (w.exported && w.isAssigned())
            {
                Set<Object> outputDeps = new LinkedHashSet<>();
                addWireDeps(w.curVar, BigIntegerUtil.MINUS_ONE, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        for (ModInstExt mi : insts)
        {
            Set<Object> instDeps = new LinkedHashSet<>();
            for (SVarExt.PortInst pi : mi.portInsts.values())
            {
                Set<WireExt> combinationalInputs = mi.proto.getCombinationalInputs(clockHigh);
                if (combinationalInputs.contains(pi.wire))
                {
                    if (pi.driver instanceof DriverExt)
                    {
                        instDeps.add(pi.getDriverExt().name);
                    } else
                    {
                        Lhs<SVarExt> lhs = pi.getDriverLhs();
                        for (Lhrange<SVarExt> lr : lhs.ranges)
                        {
                            SVarExt.LocalWire lw = (SVarExt.LocalWire)lr.getVar();
                            addWireDeps(lw, BigIntegerUtil.MINUS_ONE, instDeps);
                        }
                    }
                }
            }
            graph.put(mi, instDeps);
        }
        for (Map.Entry<Lhs<SVarExt>, DriverExt> e1 : assigns.entrySet())
        {
            Lhs l = e1.getKey();
            DriverExt d = e1.getValue();
            assert !l.ranges.isEmpty();
            Set<Object> driverDeps = new LinkedHashSet<>();
            for (Map.Entry<SVarExt, BigInteger> e2 : d.getCrudeDeps(clockHigh).entrySet())
            {
                SVarExt.LocalWire lw = (SVarExt.LocalWire)e2.getKey();
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
        for (Map.Entry<Lhrange, WireExt.WireDriver> e3 : lw.wire.drivers.entrySet())
        {
            Lhrange lhr = e3.getKey();
            WireExt.WireDriver wd = e3.getValue();
            if (lhr.getVar().getDelay() == 0
                && BigIntegerUtil.logheadMask(lhr.getWidth()).shiftLeft(lhr.getRsh()).and(mask).signum() != 0)
            {
                if (wd.driver != null)
                {
                    deps.add(wd.driver.name);
                }
                if (wd.pi != null)
                {
                    deps.add(wd.pi.inst);
                }
            }
        }
    }

    Map<Object, Set<Object>> computeFineDepsGraph(boolean clockHigh)
    {
        Map<Object, Set<Object>> graph = new LinkedHashMap<>();
        for (WireExt w : wires)
        {
            if (w.isAssigned())
            {
                for (Map.Entry<Lhrange, WireExt.WireDriver> e : w.drivers.entrySet())
                {
                    Lhrange lhr = e.getKey();
                    WireExt.WireDriver wd = e.getValue();
                    int rsh = lhr.getRsh();

                    if (wd.driver != null)
                    {
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            WireBit wb = new WireBit(w, rsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            addDriverFineDeps(wd.driver, clockHigh, wd.lsh + i, dep);
                            graph.put(wb, dep);
                        }
                    }
                    if (wd.pi != null)
                    {
                        SVarExt.PortInst piOut = wd.pi;
                        ModInstExt inst = piOut.inst;
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            WireBit wb = new WireBit(w, rsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            Map<SVarExt.LocalWire, BigInteger> masks = piOut.wire.getFineDeps(clockHigh).get(wd.lsh + i);
                            for (Map.Entry<SVarExt.LocalWire, BigInteger> e1 : masks.entrySet())
                            {
                                SVarExt.LocalWire lw = e1.getKey();
                                BigInteger mask = e1.getValue();
                                SVarExt.PortInst piIn = inst.portInsts.get(lw.wire.getName());
                                if (piIn == null)
                                {
                                    System.out.println(lw.wire.getName() + " was not found in " + inst.getInstname() + " of " + modName);
                                    continue;
                                }
                                if (piIn.driver instanceof DriverExt)
                                {
                                    addDriverFineDeps(piIn.getDriverExt(), clockHigh, mask, dep);
                                } else
                                {
                                    Lhs<SVarExt> lhs = piIn.getDriverLhs();
                                    int lsh = 0;
                                    for (Lhrange<SVarExt> lhrIn : lhs.ranges)
                                    {
                                        SVarExt.LocalWire lwIn = (SVarExt.LocalWire)lhrIn.getVar();
                                        for (int j = 0; j < lhrIn.getWidth(); j++)
                                        {
                                            if (mask.testBit(lsh + j))
                                            {
                                                dep.add(new WireBit(lwIn.wire, lhrIn.getRsh() + j));
                                            }
                                        }
                                        lsh += lhrIn.getWidth();
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

    void addDriverFineDeps(DriverExt dr, boolean clockHigh, BigInteger maskOut, Set<Object> dep)
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

    void addDriverFineDeps(DriverExt dr, boolean clockHigh, int bit, Set<Object> dep)
    {
        Map<SVarExt, BigInteger> masks = dr.getFineDeps(clockHigh).get(bit);
        for (Map.Entry<SVarExt, BigInteger> e1 : masks.entrySet())
        {
            SVarExt.LocalWire lw = (SVarExt.LocalWire)e1.getKey();
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
            ModInstExt inst = instsIndex.get(new Name(car(name)));
            return inst.newPortInst(name);
//            Module sm = inst.proto;
//            Wire wire = sm.wiresIndex.get(new Name(cdr(name)));
//            return new SVarExt.PortInst(this, name);
        } else
        {
            WireExt wire = wiresIndex.get(new Name(name));
            return wire.getVar(delay);
        }
    }

    static class WireBit
    {
        final WireExt wire;
        final int bit;

        WireBit(WireExt wire, int bit)
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
