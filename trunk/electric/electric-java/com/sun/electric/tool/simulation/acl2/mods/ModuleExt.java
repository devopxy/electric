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
import com.sun.electric.tool.simulation.acl2.svex.SvarImpl;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
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
public class ModuleExt extends SvarImpl.Builder<PathExt>
{
    private static final boolean CRUDE_COMBINATIONAL_INPUTS = false;

    public final ModName modName;
    public final Module<? extends SvarName> b;
    public final List<WireExt> wires = new ArrayList<>();
    public final List<ModInstExt> insts = new ArrayList<>();
    public final Map<Lhs<PathExt>, DriverExt> assigns = new LinkedHashMap<>();
    public final Map<Lhs<PathExt>, Lhs<PathExt>> aliaspairs = new LinkedHashMap<>();

    final Map<Name, WireExt> wiresIndex = new HashMap<>();
    final Map<Name, ModInstExt> instsIndex = new HashMap<>();
    int useCount;

    Set<WireExt> combinationalInputs0;
    Set<WireExt> combinationalInputs1;

    <N extends SvarName> ModuleExt(ModName modName, Module<N> b, Map<ModName, ModuleExt> downTop)
    {
        this.modName = modName;
        this.b = b;

        for (Wire wire : b.wires)
        {
            WireExt w = new WireExt(this, wire);
            wires.add(w);
            WireExt old = wiresIndex.put(w.getName(), w);
            Util.check(old == null);
        }

        for (ModInst modInst : b.insts)
        {
            ModInstExt mi = new ModInstExt(this, modInst, downTop);
            insts.add(mi);
            ModInstExt old = instsIndex.put(mi.getInstname(), mi);
            Util.check(old == null);
        }

        Map<Svex<N>, Svex<PathExt>> svexCache = new HashMap<>();
        int driverCount = 0;
        for (Map.Entry<Lhs<N>, Driver<N>> e : b.assigns.entrySet())
        {
            Lhs<PathExt> lhs = e.getKey().convertVars(this);
            Driver<PathExt> driver = e.getValue().convertVars(this, svexCache);
            DriverExt drv = new DriverExt(this, driver, "dr" + driverCount++);
            DriverExt old = assigns.put(lhs, drv);
            Util.check(old == null);

            int lsh = 0;
            for (Lhrange<PathExt> lhr : lhs.ranges)
            {
                Svar<PathExt> svar = lhr.getVar();
                if (svar.getName() instanceof PathExt.LocalWire)
                {
                    ((PathExt.LocalWire)svar.getName()).wire.addDriver(lhr, lsh, drv);
                } else
                {
                    assert lhs.ranges.size() == 1 && lhr.getRsh() == 0 && lsh == 0;
                    ((PathExt.PortInst)svar.getName()).addDriver(drv);
                }
                lsh += lhr.getWidth();
            }
            markAssigned(lhs, BigIntegerUtil.MINUS_ONE);
            drv.markUsed();
        }

        for (Map.Entry<Lhs<N>, Lhs<N>> e : b.aliaspairs.entrySet())
        {
            Lhs<PathExt> lhs = e.getKey().convertVars(this);
            Lhs<PathExt> rhs = e.getValue().convertVars(this);
            Util.check(lhs.ranges.size() == 1);
            Lhrange<PathExt> lhsRange = lhs.ranges.get(0);
            Util.check(lhsRange.getRsh() == 0);
            Svar<PathExt> lhsVar = lhsRange.getVar();
            Util.check(lhsVar.getDelay() == 0 && !lhsVar.isNonblocking());
            Lhs old = aliaspairs.put(lhs, rhs);
            Util.check(old == null);
            Util.check(lhs.width() == rhs.width());
            if (lhsVar.getName() instanceof PathExt.PortInst)
            {
                PathExt.PortInst pi = (PathExt.PortInst)lhsVar.getName();
                Util.check(lhsRange.getWidth() == pi.wire.getWidth());
                int lsh = 0;
                for (Lhrange<PathExt> rhsRange : rhs.ranges)
                {
                    Svar<PathExt> rhsVar = rhsRange.getVar();
                    Util.check(rhsVar.getName() instanceof PathExt.LocalWire);
                    PathExt.LocalWire lw = (PathExt.LocalWire)rhsVar.getName();
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
                    for (Lhrange<PathExt> lr : rhs.ranges)
                    {
                        Svar<PathExt> name = lr.getVar();
                        if (name != null)
                        {
                            Util.check(name.getName() instanceof PathExt.LocalWire);
                            ((PathExt.LocalWire)name.getName()).markUsed();
                        }
                    }
                }
            } else
            {
                PathExt.LocalWire lw = (PathExt.LocalWire)lhsVar.getName();
                if (stringp(lw.name.getACL2Object()).bool())
                {
                    Util.check(rhs.ranges.size() == 1);
                    PathExt.PortInst rhsPi = (PathExt.PortInst)rhs.ranges.get(0).getVar().getName();
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

    public static void markAssigned(Lhs<PathExt> lhs, BigInteger assignedBits)
    {
        for (Lhrange<PathExt> lr : lhs.ranges)
        {
            Svar<PathExt> name = lr.getVar();
            if (name != null)
            {
                BigInteger assignedBitsRange = BigIntegerUtil.loghead(lr.getWidth(), assignedBits);
                int rsh = lr.getRsh();
                assignedBitsRange = assignedBitsRange.shiftLeft(rsh);
                if (name.getName() instanceof PathExt.LocalWire)
                {
                    PathExt.LocalWire lw = (PathExt.LocalWire)name.getName();
                    lw.wire.markAssigned(assignedBitsRange);
                } else
                {
                    PathExt.PortInst pi = (PathExt.PortInst)name.getName();
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
        for (Map.Entry<Lhs<PathExt>, Lhs<PathExt>> e1 : aliaspairs.entrySet())
        {
            Lhs<PathExt> lhs = e1.getKey();
            Lhs<PathExt> rhs = e1.getValue();
            if (rhs.ranges.size() == 1
                && rhs.ranges.get(0).getWidth() == 1
                && rhs.ranges.get(0).getVar() != null
                && rhs.ranges.get(0).getRsh() == 0)
            {
                Svar<PathExt> svar = rhs.ranges.get(0).getVar();
                if (svar.getName() instanceof PathExt.LocalWire)
                {
                    WireExt w = ((PathExt.LocalWire)svar.getName()).wire;
                    if (w.isGlobal()
                        && lhs.ranges.size() == 1
                        && lhs.ranges.get(0).getWidth() == 1
                        && lhs.ranges.get(0).getVar() != null
                        && rhs.ranges.get(0).getRsh() == 0)
                    {
                        Svar<PathExt> svar1 = lhs.ranges.get(0).getVar();
                        if (svar1.getName() instanceof PathExt.PortInst)
                        {
                            ((PathExt.PortInst)svar1.getName()).wire.markGlobal(w.global);
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
                    for (Map<Svar<PathExt>, BigInteger> mapForBit : out.fineDeps0)
                    {
                        for (Map.Entry<Svar<PathExt>, BigInteger> e : mapForBit.entrySet())
                        {
                            Svar<PathExt> svar = e.getKey();
                            BigInteger mask = e.getValue();
                            if (mask.signum() != 0)
                            {
                                assert svar.getDelay() == 0;
                                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                                combinationalInputs0.add(lw.wire);
                            }
                        }
                    }
                    for (Map<Svar<PathExt>, BigInteger> mapForBit : out.fineDeps1)
                    {
                        for (Map.Entry<Svar<PathExt>, BigInteger> e : mapForBit.entrySet())
                        {
                            Svar<PathExt> svar = e.getKey();
                            BigInteger mask = e.getValue();
                            if (mask.signum() != 0)
                            {
                                assert svar.getDelay() == 0;
                                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
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
        Map<Svar<PathExt>, Vec4> patchEnv = makePatchEnv(global, clkOne ? Vec2.ONE : Vec2.ZERO);
        Map<SvexCall<PathExt>, SvexCall<PathExt>> patchMemoize = new HashMap<>();
        for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : assigns.entrySet())
        {
            Lhs<PathExt> l = e1.getKey();
            DriverExt d = e1.getValue();
            d.computeDeps(l.width(), clkOne, patchEnv, patchMemoize);
        }
    }

    private Map<Svar<PathExt>, Vec4> makePatchEnv(String global, Vec4 globalVal)
    {
        Map<Svar<PathExt>, Vec4> env = new HashMap<>();
        for (WireExt w : wires)
        {
            if (w.isGlobal() && w.global.equals(global))
            {
                env.put(w.getVar(0), globalVal);
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
                addWireDeps(w.getVar(0), BigIntegerUtil.MINUS_ONE, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        for (ModInstExt mi : insts)
        {
            Set<Object> instDeps = new LinkedHashSet<>();
            for (PathExt.PortInst pi : mi.portInsts.values())
            {
                Set<WireExt> combinationalInputs = mi.proto.getCombinationalInputs(clockHigh);
                if (combinationalInputs.contains(pi.wire))
                {
                    if (pi.driver instanceof DriverExt)
                    {
                        instDeps.add(pi.getDriverExt().name);
                    } else
                    {
                        Lhs<PathExt> lhs = pi.getDriverLhs();
                        for (Lhrange<PathExt> lr : lhs.ranges)
                        {
                            addWireDeps(lr.getVar(), BigIntegerUtil.MINUS_ONE, instDeps);
                        }
                    }
                }
            }
            graph.put(mi, instDeps);
        }
        for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : assigns.entrySet())
        {
            Lhs<PathExt> l = e1.getKey();
            DriverExt d = e1.getValue();
            assert !l.ranges.isEmpty();
            Set<Object> driverDeps = new LinkedHashSet<>();
            for (Map.Entry<Svar<PathExt>, BigInteger> e2 : d.getCrudeDeps(clockHigh).entrySet())
            {
                Svar<PathExt> svar = e2.getKey();
                BigInteger mask = e2.getValue();
                if (mask == null)
                {
                    mask = BigInteger.ZERO;
                }
                addWireDeps(svar, mask, driverDeps);
            }
            graph.put(d.name, driverDeps);
        }
        return graph;
    }

    private void addWireDeps(Svar<PathExt> svar, BigInteger mask, Set<Object> deps)
    {
        if (svar.getDelay() != 0)
        {
            return;
        }
        PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
        if (!lw.wire.isAssigned())
        {
            deps.add(lw.wire);
            return;
        }
        for (Map.Entry<Lhrange<PathExt>, WireExt.WireDriver> e3 : lw.wire.drivers.entrySet())
        {
            Lhrange<PathExt> lhr = e3.getKey();
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
                for (Map.Entry<Lhrange<PathExt>, WireExt.WireDriver> e : w.drivers.entrySet())
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
                        PathExt.PortInst piOut = wd.pi;
                        ModInstExt inst = piOut.inst;
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            WireBit wb = new WireBit(w, rsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            Map<Svar<PathExt>, BigInteger> masks = piOut.wire.getFineDeps(clockHigh).get(wd.lsh + i);
                            for (Map.Entry<Svar<PathExt>, BigInteger> e1 : masks.entrySet())
                            {
                                PathExt.LocalWire lw = (PathExt.LocalWire)e1.getKey().getName();
                                BigInteger mask = e1.getValue();
                                PathExt.PortInst piIn = inst.portInsts.get(lw.wire.getName());
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
                                    Lhs<PathExt> lhs = piIn.getDriverLhs();
                                    int lsh = 0;
                                    for (Lhrange<PathExt> lhrIn : lhs.ranges)
                                    {
                                        PathExt.LocalWire lwIn = (PathExt.LocalWire)lhrIn.getVar().getName();
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
                addWireDeps(w.getVar(0), BigIntegerUtil.MINUS_ONE, outputDeps);
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
        Map<Svar<PathExt>, BigInteger> masks = dr.getFineDeps(clockHigh).get(bit);
        for (Map.Entry<Svar<PathExt>, BigInteger> e : masks.entrySet())
        {
            Svar<PathExt> svar = e.getKey();
            BigInteger maskIn = e.getValue();
            if (maskIn != null && svar.getDelay() == 0)
            {
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
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
    public PathExt newName(ACL2Object nameImpl)
    {
        Path path = Path.fromACL2(nameImpl);
        if (path instanceof Path.Wire)
        {
            Path.Wire pathWire = (Path.Wire)path;
            WireExt wire = wiresIndex.get(pathWire.name);
            return wire.path;
        } else
        {
            Path.Scope pathScope = (Path.Scope)path;
//            Path.Wire subPathWire = (Path.Wire) pathScope.subpath;
            ModInstExt inst = instsIndex.get(pathScope.namespace);
            return inst.newPortInst(pathScope);
        }
    }

    /*
    @Override
    public SvarImpl<PathExt> newVar(ACL2Object name, int delay, boolean nonblocking)
    {
        Util.check(!nonblocking);
        if (consp(name).bool())
        {
            Util.check(delay == 0);
            ModInstExt inst = instsIndex.get(new Name(car(name)));
            return inst.newPortInst(name).svar;
//            Module sm = inst.proto;
//            Wire wire = sm.wiresIndex.get(new Name(cdr(name)));
//            return new SVarExt.PortInst(this, name);
        } else
        {
            WireExt wire = wiresIndex.get(new Name(name));
            return wire.getVar(delay);
        }
    }
     */
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
