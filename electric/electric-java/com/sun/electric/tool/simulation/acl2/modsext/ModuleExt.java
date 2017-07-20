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
package com.sun.electric.tool.simulation.acl2.modsext;

import com.sun.electric.tool.simulation.acl2.mods.Driver;
import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModInst;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.mods.Wire;
import com.sun.electric.tool.simulation.acl2.mods.Util;
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
    public final ModName modName;
    public final Module<? extends SvarName> b;
    public final List<WireExt> wires = new ArrayList<>();
    public final List<ModInstExt> insts = new ArrayList<>();
    public final Map<Lhs<PathExt>, DriverExt> assigns = new LinkedHashMap<>();
    public final Map<Lhs<PathExt>, Lhs<PathExt>> aliaspairs = new LinkedHashMap<>();

    final Map<Name, WireExt> wiresIndex = new HashMap<>();
    final Map<Name, ModInstExt> instsIndex = new HashMap<>();
    int useCount;
    boolean isTop;

    final boolean hasState;
    final Set<WireExt> stateWires = new LinkedHashSet<>();
    Set<WireExt> crudeCombinationalInputs0;
    Set<WireExt> crudeCombinationalInputs1;
    Set<WireExt> fineCombinationalInputs0;
    Set<WireExt> fineCombinationalInputs1;

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

        boolean hasState = false;
        for (ModInst modInst : b.insts)
        {
            ModInstExt mi = new ModInstExt(this, modInst, downTop);
            insts.add(mi);
            ModInstExt old = instsIndex.put(mi.getInstname(), mi);
            Util.check(old == null);
            if (mi.proto.hasState)
            {
                hasState = true;
            }
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

            drv.setSource(lhs);
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
//                    drv.setSource(lhr);
                    ((PathExt.PortInst)svar.getName()).setDriver(drv);
                }
                lsh += lhr.getWidth();
            }
            markAssigned(lhs, BigIntegerUtil.MINUS_ONE);
            drv.markUsed();
            for (Svar<PathExt> svar : drv.collectVars())
            {
                if (svar.getDelay() != 0)
                {
                    Util.check(svar.getDelay() == 1);
                    PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                    stateWires.add(lw.wire);
                }
            }
        }
        this.hasState = hasState || !stateWires.isEmpty();

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
                    pi.setSource(rhs);
                    BigInteger assignedBits = pi.wire.getAssignedBits();
                    Util.check(assignedBits.equals(BigIntegerUtil.logheadMask(pi.wire.getWidth())));
                    markAssigned(rhs, assignedBits);
                } else
                {
                    pi.setDriver(rhs);
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
                    rhsPi.setDriver(lhs);
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
                    Util.check(lhs.ranges.size() == 1);
                    Util.check(lr.getRsh() == 0);
                    PathExt.PortInst pi = (PathExt.PortInst)name.getName();
                    Util.check(assignedBitsRange.signum() >= 0 && assignedBitsRange.bitLength() <= pi.wire.getWidth());
                    Util.check(!pi.wire.isAssigned());
//                    Util.check(assignedBitsRange.and(pi.wire.getAssignedBits()).signum() == 0);
                }
            }
            assignedBits = assignedBits.shiftRight(lr.getWidth());
        }
    }

    void markTop()
    {
        isTop = true;
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

    void checkExports()
    {
        for (WireExt wire : wires)
        {
            if (wire.exported && wire.isAssigned())
            {
                Util.check(wire.getAssignedBits().equals(BigIntegerUtil.logheadMask(wire.getWidth())));
            }
        }
        for (ModInstExt inst : insts)
        {
            inst.checkExports();
        }
    }

    Set<WireExt> getCrudeCombinationalInputs(boolean clockVal)
    {
        return clockVal ? crudeCombinationalInputs1 : crudeCombinationalInputs0;
    }

    Set<WireExt> getFineCombinationalInputs(boolean clockVal)
    {
        return clockVal ? fineCombinationalInputs1 : fineCombinationalInputs0;
    }

    void computeCombinationalInputs(String global)
    {
        checkExports();
        computeDriverDeps(global, false);
        computeDriverDeps(global, true);

        fineCombinationalInputs0 = new LinkedHashSet<>();
        fineCombinationalInputs1 = new LinkedHashSet<>();
        Map<Object, Set<Object>> fineGraph0 = computeFineDepsGraph(false);
        Map<Object, Set<Object>> fineGraph1 = computeFineDepsGraph(true);
        Map<Object, Set<Object>> fineClosure0 = closure(fineGraph0);
        Map<Object, Set<Object>> fineClosure1 = closure(fineGraph1);
//        System.out.println("=== " + modName + " fineDeps0");
//        showGraph(fineGraph0);
//        System.out.println("=== " + modName + " fineClosure0");
//        showGraph(fineClosure0);
//        System.out.println("=== " + modName + " fineDeps1");
//        showGraph(fineGraph1);
//        System.out.println("=== " + modName + " fineClosure1");
//        showGraph(fineClosure1);
        for (WireExt out : wires)
        {
            if (out.exported && out.isAssigned())
            {
                out.setFineDeps(false, fineClosure0);
                out.setFineDeps(true, fineClosure1);
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
                            fineCombinationalInputs0.add(lw.wire);
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
                            fineCombinationalInputs1.add(lw.wire);
                        }
                    }
                }
            }
        }
        Map<Object, Set<Object>> fineTransdep0 = transdep(fineGraph0);
        Map<Object, Set<Object>> fineTransdep1 = transdep(fineGraph1);
        markInstancesToSplit(fineTransdep0, false);
        markInstancesToSplit(fineTransdep1, true);

        crudeCombinationalInputs0 = new LinkedHashSet<>();
        crudeCombinationalInputs1 = new LinkedHashSet<>();
        Map<Object, Set<Object>> crudeGraph0 = computeDepsGraph(false);
        Map<Object, Set<Object>> crudeGraph1 = computeDepsGraph(true);
        Map<Object, Set<Object>> crudeClosure0 = closure(crudeGraph0);
        Map<Object, Set<Object>> crudeClosure1 = closure(crudeGraph1);
//            System.out.println("Module " + modName + " crude graph 0");
//            showGraph(graph0);
//            System.out.println("Module " + modName + " crude graph 1");
//            showGraph(graph1);
        for (WireExt out : wires)
        {
            if (out.exported && out.isAssigned())
            {
                for (Object dep : crudeClosure0.get(out))
                {
                    if (dep instanceof WireExt)
                    {
                        WireExt in = (WireExt)dep;
                        if (in.exported && !in.isAssigned())
                        {
                            crudeCombinationalInputs0.add(in);
                        }
                    }
                }
                for (Object dep : crudeClosure1.get(out))
                {
                    if (dep instanceof WireExt)
                    {
                        WireExt in = (WireExt)dep;
                        if (in.exported && !in.isAssigned())
                        {
                            crudeCombinationalInputs1.add(in);
                        }
                    }
                }
            }
        }
        if (!crudeCombinationalInputs0.equals(crudeCombinationalInputs1))
        {
            System.out.print("Crude combinational inputs differs in " + modName);
            if (modName.toString().equals("l1_l1ahdrpgendp"))
            {
                System.out.print(" FIXING");
                Util.check(crudeCombinationalInputs0.containsAll(crudeCombinationalInputs1));
                crudeCombinationalInputs1 = crudeCombinationalInputs0;
            }
            System.out.println();
        }
    }

    private void computeDriverDeps(String global, boolean clkOne)
    {
        Map<Svar<PathExt>, Vec4> patchEnv = makePatchEnv(global, clkOne ? Vec2.ONE : Vec2.ZERO);
        Map<SvexCall<PathExt>, SvexCall<PathExt>> patchMemoize = new HashMap<>();
        for (Map.Entry<Lhs<PathExt>, DriverExt> e : assigns.entrySet())
        {
            Lhs<PathExt> l = e.getKey();
            DriverExt d = e.getValue();
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
            if (w.isAssigned() || !(w.exported || isTop))
            {
                BigInteger mask = BigIntegerUtil.logheadMask(w.getWidth());
                Set<Object> outputDeps = new LinkedHashSet<>();
                addWireDeps(w.getVar(0), mask, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        for (ModInstExt mi : insts)
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
                            Set<Object> deps = new LinkedHashSet<>();
                            Map<Svar<PathExt>, BigInteger> fineDeps = piOut.wire.getFineDeps(clockHigh).get(bit);
                            for (Map.Entry<Svar<PathExt>, BigInteger> e : fineDeps.entrySet())
                            {
                                Svar<PathExt> svar = e.getKey();
                                BigInteger mask = e.getValue();
                                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                                PathExt.PortInst piIn = mi.portInsts.get(lw.wire.getName());
                                Util.check(piIn != null);
                                if (piIn.driver instanceof DriverExt)
                                {
                                    addDriverDeps(piIn.getDriverExt(), mask, deps);
//                                    deps.add(piIn.getDriverExt().name);
                                } else
                                {
                                    Lhs<PathExt> lhs = piIn.getDriverLhs();
                                    for (Lhrange<PathExt> lr : lhs.ranges)
                                    {
                                        BigInteger mask1 = BigIntegerUtil.loghead(lr.getWidth(), mask).shiftLeft(lr.getRsh());
                                        addWireDeps(lr.getVar(), mask1, deps);
                                        mask = mask.shiftRight(lr.getWidth());
                                    }
                                }
                            }
                            graph.put(wb, deps);
                        }
                    }
                }
            } else
            {
                Set<WireExt> combinationalInputs = mi.proto.getCrudeCombinationalInputs(clockHigh);
                Set<Object> instDeps = new LinkedHashSet<>();
                for (PathExt.PortInst pi : mi.portInsts.values())
                {
                    if (combinationalInputs.contains(pi.wire))
                    {
                        if (pi.driver instanceof DriverExt)
                        {
                            BigInteger mask = BigIntegerUtil.logheadMask(pi.wire.getWidth());
                            addDriverDeps(pi.getDriverExt(), mask, instDeps);
//                            instDeps.add(pi.getDriverExt().name);
                        } else
                        {
                            Lhs<PathExt> lhs = pi.getDriverLhs();
                            for (Lhrange<PathExt> lr : lhs.ranges)
                            {
                                BigInteger mask = BigIntegerUtil.logheadMask(lr.getWidth()).shiftLeft(lr.getRsh());
                                addWireDeps(lr.getVar(), mask, instDeps);
                            }
                        }
                    }
                }
                graph.put(mi, instDeps);
                for (PathExt.PortInst pi : mi.portInsts.values())
                {
                    if (pi.wire.specialOutput)
                    {
                        assert pi.wire.isAssigned();
                        Lhs<PathExt> lhs = pi.source;
                        assert lhs.ranges.size() == 1;
                        Lhrange<PathExt> lrange = lhs.ranges.get(0);
                        assert lrange.getRsh() == 0;
                        Svar<PathExt> svar = lrange.getVar();
                        assert svar.getDelay() == 0;
                        Set<Object> deps = new LinkedHashSet<>();
                        for (Map<Svar<PathExt>, BigInteger> fineDeps : pi.wire.getFineDeps(clockHigh))
                        {
                            for (Map.Entry<Svar<PathExt>, BigInteger> e : fineDeps.entrySet())
                            {
                                Svar<PathExt> svar1 = e.getKey();
                                BigInteger mask = e.getValue();
                                PathExt.LocalWire lw = (PathExt.LocalWire)svar1.getName();
                                PathExt.PortInst piIn = mi.portInsts.get(lw.wire.getName());
                                Util.check(piIn != null);
                                if (piIn.driver instanceof DriverExt)
                                {
                                    addDriverDeps(piIn.getDriverExt(), mask, deps);
//                                    deps.add(piIn.getDriverExt().name);
                                } else
                                {
                                    Lhs<PathExt> lhs1 = piIn.getDriverLhs();
                                    for (Lhrange<PathExt> lr : lhs1.ranges)
                                    {
                                        BigInteger mask1 = BigIntegerUtil.loghead(lr.getWidth(), mask).shiftLeft(lr.getRsh());
                                        addWireDeps(lr.getVar(), mask1, deps);
                                        mask = mask.shiftRight(lr.getWidth());
                                    }
                                }
                            }
                        }
                        graph.put(pi, deps);
                    }
                }
            }
        }
        for (Map.Entry<Lhs<PathExt>, DriverExt> e1 : assigns.entrySet())
        {
            Lhs<PathExt> l = e1.getKey();
            DriverExt d = e1.getValue();
            if (d.splitIt)
            {
                assert l.ranges.size() == 1;
                Lhrange<PathExt> lrange = l.ranges.get(0);
                assert lrange.getRsh() == 0;
                Svar<PathExt> svar = lrange.getVar();
                assert svar.getDelay() == 0;
                List<Map<Svar<PathExt>, BigInteger>> fineDeps = d.getFineDeps(clockHigh);
                assert fineDeps.size() == lrange.getWidth();
                for (int bit = 0; bit < lrange.getWidth(); bit++)
                {
                    Set<Object> driverDeps = new LinkedHashSet<>();
                    Map<Svar<PathExt>, BigInteger> fineDep = fineDeps.get(bit);
                    for (Map.Entry<Svar<PathExt>, BigInteger> e2 : fineDep.entrySet())
                    {
                        Svar<PathExt> svar2 = e2.getKey();
                        BigInteger mask = e2.getValue();
                        addWireDeps(svar2, mask, driverDeps);
                    }
                    graph.put(d.wireBits[bit], driverDeps);
                }
            } else
            {
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
        }
        return graph;
    }

    private void addWireDeps(Svar<PathExt> svar, BigInteger mask, Set<Object> deps)
    {
        if (svar.getDelay() != 0 || mask.signum() == 0)
        {
            return;
        }
        PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
        if (!lw.wire.isAssigned() && (lw.wire.exported || isTop))
        {
            deps.add(lw.wire);
            return;
        }
        for (Map.Entry<Lhrange<PathExt>, WireExt.WireDriver> e3 : lw.wire.drivers.entrySet())
        {
            Lhrange<PathExt> lhr = e3.getKey();
            WireExt.WireDriver wd = e3.getValue();
            BigInteger mask1 = BigIntegerUtil.loghead(lhr.getWidth(), mask.shiftRight(lhr.getRsh()));
            if (lhr.getVar().getDelay() == 0 && mask1.signum() > 0)
            {
                if (wd.driver != null)
                {
                    addDriverDeps(wd.driver, mask1.shiftLeft(wd.lsh), deps);
                }
                if (wd.pi != null)
                {
                    if (wd.pi.inst.splitIt)
                    {
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            if (mask1.testBit(i))
                            {
                                deps.add(wd.pi.getParentBit(wd.lsh + i));
                            }
                        }
                    } else if (wd.pi.wire.specialOutput)
                    {
                        deps.add(wd.pi);
                    } else
                    {
                        deps.add(wd.pi.inst);
                    }
                }
            }
        }
    }

    private void addDriverDeps(DriverExt driver, BigInteger mask, Set<Object> deps)
    {
        if (driver.splitIt)
        {
            for (int i = 0; i < driver.wireBits.length; i++)
            {
                if (mask.testBit(i))
                {
                    deps.add(driver.wireBits[i]);
                }
            }
        } else if (mask.signum() > 0)
        {
            deps.add(driver.name);
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
                        assert wd.pi == null;
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            WireExt.Bit wb = w.getBit(rsh + i);
                            assert wb == wd.driver.wireBits[wd.lsh + i];
                            Set<Object> dep = new LinkedHashSet<>();
                            addDriverFineDeps(wd.driver, clockHigh, wd.lsh + i, dep);
                            graph.put(wb, dep);
                        }
                    } else
                    {
                        PathExt.PortInst piOut = wd.pi;
                        assert piOut != null;
                        ModInstExt inst = piOut.inst;
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            WireExt.Bit wb = w.getBit(rsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            Map<Svar<PathExt>, BigInteger> masks = piOut.wire.getFineDeps(clockHigh).get(wd.lsh + i);
                            for (Map.Entry<Svar<PathExt>, BigInteger> e1 : masks.entrySet())
                            {
                                PathExt.LocalWire lw = (PathExt.LocalWire)e1.getKey().getName();
                                BigInteger mask = e1.getValue();
                                PathExt.PortInst piIn = inst.portInsts.get(lw.wire.getName());
                                Util.check(piIn != null);
                                for (int bit = 0; bit < piIn.getWidth(); bit++)
                                {
                                    if (mask.testBit(bit))
                                    {
                                        assert piIn.getParentBit(bit) != null;
                                        dep.add(piIn.getParentBit(bit));
                                    }
                                }
                            }
                            graph.put(wb, dep);
                        }
                    }
                }
//                Set<Object> outputDeps = new LinkedHashSet<>();
//                addWireDeps(w.getVar(0), BigIntegerUtil.MINUS_ONE, outputDeps);
//                graph.put(w, outputDeps);
            }
            if (w.isAssigned() || !isTop && !w.exported)
            {
                BigInteger assignedBits = w.getAssignedBits();
                for (int bit = 0; bit < w.getWidth(); bit++)
                {
                    if (!assignedBits.testBit(bit))
                    {
                        WireExt.Bit wb = w.getBit(bit);
                        graph.put(wb, Collections.emptySet());
                    }
                }
            }
        }
        for (Map.Entry<Lhs<PathExt>, DriverExt> e : assigns.entrySet())
        {
            Lhs<PathExt> lhs = e.getKey();
            DriverExt drv = e.getValue();
            if (lhs.ranges.size() != 1)
            {
                continue;
            }
            Lhrange<PathExt> lhr = lhs.ranges.get(0);
            Svar<PathExt> svar = lhr.getVar();
            if (!(svar.getName() instanceof PathExt.PortInst))
            {
                continue;
            }
            assert lhr.getRsh() == 0;
            PathExt.PortInst pi = (PathExt.PortInst)svar.getName();
            for (int bit = 0; bit < lhr.getWidth(); bit++)
            {
                WireExt.Bit wb = drv.wireBits[bit];
                assert wb == pi.getParentBit(bit);
                Set<Object> dep = new LinkedHashSet<>();
                addDriverFineDeps(drv, clockHigh, bit, dep);
                graph.put(wb, dep);
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
                        assert lw.wire.getBit(j) != null;
                        dep.add(lw.wire.getBit(j));
                    }
                }
            }
        }
    }

    private void markInstancesToSplit(Map<Object, Set<Object>> transdep, boolean clockHigh)
    {
        for (ModInstExt inst : insts)
        {
            Set<WireExt> combinationalInputs = inst.proto.getFineCombinationalInputs(clockHigh);
            Set<Object> inputDeps = new HashSet<>();
            for (PathExt.PortInst pi : inst.portInsts.values())
            {
                WireExt export = pi.wire;
                if (!combinationalInputs.contains(export))
                {
                    continue;
                }
                assert !export.isAssigned();
                for (int bit = 0; bit < export.getWidth(); bit++)
                {
                    Set<Object> deps = transdep.get(pi.getParentBit(bit));
                    if (deps != null)
                    {
                        inputDeps.addAll(deps);
                    }
                }
            }
            Set<WireExt.Bit> outDeps = new LinkedHashSet<>();
            for (PathExt.PortInst pi : inst.portInsts.values())
            {
                WireExt export = pi.wire;
                if (export.isAssigned())
                {
                    for (int bit = 0; bit < export.getWidth(); bit++)
                    {
                        if (inputDeps.contains(pi.getParentBit(bit)))
                        {
                            outDeps.add(pi.getProtoBit(bit));
                        }
                    }
                }
            }
            if (!outDeps.isEmpty())
            {
                inst.splitIt = true;
                System.out.println(modName + " " + inst + " clock=" + (clockHigh ? "1" : "0") + " some combinational inputs depend on these outputs:");
                System.out.print("   ");
                for (WireExt.Bit wireBit : outDeps)
                {
                    System.out.print(" " + wireBit);
                }
                System.out.println();
            }
        }
        for (Map.Entry<Lhs<PathExt>, DriverExt> e : assigns.entrySet())
        {
            Lhs<PathExt> lhs = e.getKey();
            DriverExt drv = e.getValue();
            Set<Object> inputDeps = new HashSet<>();
            Map<Svar<PathExt>, BigInteger> crudeDeps = drv.getCrudeDeps(clockHigh);
            for (Map.Entry<Svar<PathExt>, BigInteger> e1 : crudeDeps.entrySet())
            {
                Svar<PathExt> svar = e1.getKey();
                BigInteger mask = e1.getValue();
                if (svar.getDelay() != 0)
                {
                    continue;
                }
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                for (int bit = 0; bit < lw.wire.getWidth(); bit++)
                {
                    if (mask.testBit(bit))
                    {
                        Set<Object> deps = transdep.get(lw.wire.getBit(bit));
                        if (deps != null)
                        {
                            inputDeps.addAll(deps);
                        }
                    }
                }
            }
            Set<WireExt.Bit> outDeps = new LinkedHashSet<>();
            for (int bit = 0; bit < lhs.width(); bit++)
            {
                WireExt.Bit wb = drv.wireBits[bit];
                assert wb != null;
                if (inputDeps.contains(wb))
                {
                    outDeps.add(wb);
                }
            }
            if (!outDeps.isEmpty())
            {
                drv.splitIt = true;
                System.out.println(modName + " " + drv.name + " clock=" + (clockHigh ? "1" : "0") + " some combinational inputs depend on these outputs:");
                System.out.print("   ");
                for (WireExt.Bit wireBit : outDeps)
                {
                    System.out.print(" " + wireBit);
                }
                System.out.println();
            }
        }
    }

    void showGraph(Map<Object, Set<Object>> graph)
    {
        for (Map.Entry<Object, Set<Object>> e : graph.entrySet())
        {
            System.out.print(e.getKey() + " <=");
            for (Object o : e.getValue())
            {
                System.out.print(" " + o);
            }
            System.out.println();
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

    Map<Object, Set<Object>> transdep(Map<Object, Set<Object>> rel)
    {
        Map<Object, Set<Object>> transdep = new HashMap<>();
        Set<Object> visited = new HashSet<>();
        for (Object key : rel.keySet())
        {
            transdep(key, rel, transdep, visited);
        }
        Util.check(transdep.size() == visited.size());
        return transdep;
    }

    private Set<Object> transdep(Object top,
        Map<Object, Set<Object>> rel,
        Map<Object, Set<Object>> transdep,
        Set<Object> visited)
    {
        Set<Object> ret = transdep.get(top);
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
                ret.add(top);
                for (Object svar : dep)
                {
                    ret.addAll(transdep(svar, rel, transdep, visited));
                }
            }
            transdep.put(top, ret);
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
}
