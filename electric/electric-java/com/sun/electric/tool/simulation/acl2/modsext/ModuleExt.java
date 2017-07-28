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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * SV module.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODULE>.
 */
public class ModuleExt extends SvarImpl.Builder<PathExt> implements Comparator<Svar<PathExt>>
{
    // State marker in dependency graphs
    static final String STATE = "STATE";

    public final ModName modName;
    public final Module<? extends SvarName> b;
    public final List<WireExt> wires = new ArrayList<>();
    public final List<ModInstExt> insts = new ArrayList<>();
    public final Map<Lhs<PathExt>, DriverExt> assigns = new LinkedHashMap<>();
    public final Map<Lhs<PathExt>, Lhs<PathExt>> aliaspairs = new LinkedHashMap<>();

    final Map<Name, WireExt> wiresIndex = new HashMap<>();
    final Map<Name, ModInstExt> instsIndex = new HashMap<>();
    long bitCount;
    int useCount;
    boolean isTop;

    final boolean hasSvtvState;
    boolean hasPhaseState, hasCycleState;
    final Set<WireExt> stateWires = new LinkedHashSet<>();
    final Map<Svar<PathExt>, BigInteger> stateVars0 = new LinkedHashMap<>();
    final Map<Svar<PathExt>, BigInteger> stateVars1 = new LinkedHashMap<>();

    <N extends SvarName> ModuleExt(ModName modName, Module<N> b, Map<ModName, ModuleExt> downTop)
    {
        this.modName = modName;
        this.b = b;

        for (Wire wire : b.wires)
        {
            WireExt w = new WireExt(this, wire, wires.size());
            wires.add(w);
            WireExt old = wiresIndex.put(w.getName(), w);
            Util.check(old == null);
            bitCount += w.getWidth();
        }

        boolean hasSvtvState = false;
        for (ModInst modInst : b.insts)
        {
            ModInstExt mi = new ModInstExt(this, modInst, downTop);
            insts.add(mi);
            ModInstExt old = instsIndex.put(mi.getInstname(), mi);
            Util.check(old == null);
            if (mi.proto.hasSvtvState)
            {
                hasSvtvState = true;
            }
            bitCount += mi.proto.bitCount;
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
//                    drv.setSource(lhr);
                    ((PathExt.PortInst)svar.getName()).setDriver(drv);
                }
                lsh += lhr.getWidth();
            }
            markAssigned(lhs, BigIntegerUtil.MINUS_ONE);
            drv.setSource(lhs);
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
        this.hasSvtvState = hasSvtvState || !stateWires.isEmpty();

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

    void markTop(String[] exports)
    {
        isTop = true;
        useCount = 1;
        List<String> exportList = null;
        if (exports != null)
        {
            exportList = Arrays.asList(exports);
        }
        int numExported = 0;
        for (WireExt w : wires)
        {
            if (exportList == null || exportList.indexOf(w.getName().toString()) >= 0)
            {
                w.exported = true;
                numExported++;
                if (w.getWidth() == 1 && w.getLowIdx() == 0)
                {
                    w.global = w.getName().toString();
                }
            }
        }
        Util.check(numExported == (exports != null ? exports.length : wires.size()));
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
        boolean prevIsExport = true;
        for (WireExt wire : wires)
        {
            if (wire.isExport())
            {
                if (!prevIsExport)
                {
                    System.out.println("Module " + modName + " export " + wire + " is not at the beginning");
                    prevIsExport = true;
                }
            } else
            {
                prevIsExport = false;
            }

        }
        for (ModInstExt inst : insts)
        {
            inst.checkExports();
        }
    }

    void computeCombinationalInputs(String global)
    {
        checkExports();
        computeDriverDeps(global, false);
        computeDriverDeps(global, true);
        for (Map.Entry<Svar<PathExt>, BigInteger> e : stateVars0.entrySet())
        {
            Svar<PathExt> svar = e.getKey();
            BigInteger mask0 = e.getValue();
            Util.check(svar.getDelay() == 1);
            PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
            Util.check(stateWires.contains(lw.wire));
            Util.check(mask0.equals(BigIntegerUtil.logheadMask(lw.getWidth())));
        }
        for (Map.Entry<Svar<PathExt>, BigInteger> e : stateVars1.entrySet())
        {
            Svar<PathExt> svar = e.getKey();
            BigInteger mask1 = e.getValue();
            Util.check(svar.getDelay() == 1);
            PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
            Util.check(stateWires.contains(lw.wire));
            Util.check(mask1.equals(BigIntegerUtil.logheadMask(lw.getWidth())));
        }
        hasPhaseState = !stateVars0.isEmpty() || !stateVars1.isEmpty();
        hasCycleState = !stateVars0.isEmpty();
        for (ModInstExt inst : insts)
        {
            if (inst.proto.hasPhaseState)
            {
                hasPhaseState = true;
            }
            if (inst.proto.hasCycleState)
            {
                hasCycleState = true;
            }
        }

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
            if (!out.isInput())
            {
                out.setFineDeps(false, fineClosure0);
                out.setFineDeps(true, fineClosure1);
            }
        }
        Map<Object, Set<Object>> fineTransdep0 = transdep(fineGraph0);
        Map<Object, Set<Object>> fineTransdep1 = transdep(fineGraph1);
        markInstancesToSplit(fineTransdep0, false);
        markInstancesToSplit(fineTransdep1, true);

        Map<Object, Set<Object>> crudeGraph0 = computeDepsGraph(false);
        Map<Object, Set<Object>> crudeGraph1 = computeDepsGraph(true);
        Map<Object, Set<Object>> crudeClosure0 = closure(crudeGraph0);
        Map<Object, Set<Object>> crudeClosure1 = closure(crudeGraph1);
        for (WireExt out : wires)
        {
            if (!out.isInput())
            {
                out.crudePortStateDep0 = gatherDep(out.crudePortDeps0, out, crudeClosure0);
                out.crudePortStateDep1 = gatherDep(out.crudePortDeps1, out, crudeClosure1);
            }
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
            if (!w.isInput())
            {
                BigInteger mask = BigIntegerUtil.logheadMask(w.getWidth());
                Set<Object> outputDeps = new LinkedHashSet<>();
                addWireDeps(w.getVar(0), mask, outputDeps);
                graph.put(w, outputDeps);
            }
        }
        for (ModInstExt mi : insts)
        {
            for (PathExt.PortInst piOut : mi.portInsts)
            {
                if (piOut.wire.isAssigned())
                {
                    if (piOut.splitIt)
                    {
                        BitSet fineBitStateDeps = piOut.wire.getFineBitStateDeps(clockHigh);
                        List<Map<Svar<PathExt>, BigInteger>> fineBitDeps = piOut.wire.getFineBitDeps(clockHigh);
                        for (int bit = 0; bit < piOut.wire.getWidth(); bit++)
                        {
                            PathExt.Bit pb = piOut.getBit(bit);
                            boolean fineBitStateDep = fineBitStateDeps.get(bit);
                            Map<Svar<PathExt>, BigInteger> fineBitDep = fineBitDeps.get(bit);
                            putPortInsDeps(pb, mi, fineBitStateDep, fineBitDep, graph);
                        }
                    } else
                    {
//                        Set<WireExt> crudeCombinationalInputs = piOut.wire.getCrudeCombinationalInputs(clockHigh);
//                        Set<Object> deps = new LinkedHashSet<>();
//                        if (piOut.wire.getCrudeStateArg(clockHigh))
//                        {
//                            deps.add(STATE);
//                        }
//                        for (WireExt wire : crudeCombinationalInputs)
//                        {
//                            BigInteger mask = BigIntegerUtil.logheadMask(wire.getWidth());
//                            PathExt.PortInst piIn = mi.portInsts.get(wire.getName());
//                            addPortInDeps(piIn, mask, deps);
//                        }
//                        graph.put(piOut, deps);
                        boolean finePortStateDeps = piOut.wire.getCrudePortStateDeps(clockHigh);
                        Map<Svar<PathExt>, BigInteger> finePortDeps = piOut.wire.getCrudePortDeps(clockHigh);
                        putPortInsDeps(piOut, mi, finePortStateDeps, finePortDeps, graph);
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
//                assert l.ranges.size() == 1;
//                Lhrange<PathExt> lrange = l.ranges.get(0);
//                assert lrange.getRsh() == 0;
//                Svar<PathExt> svar = lrange.getVar();
//                assert svar.getDelay() == 0;
                List<Map<Svar<PathExt>, BigInteger>> fineDeps = d.getFineBitLocDeps(clockHigh);
                assert fineDeps.size() == l.width();
                for (int bit = 0; bit < l.width(); bit++)
                {
                    Map<Svar<PathExt>, BigInteger> fineDep = fineDeps.get(bit);
                    putVarMasksDeps(d.getBit(bit), fineDep, graph);
                }
            } else
            {
                assert !l.ranges.isEmpty();
                putVarMasksDeps(d, d.getCrudeDeps(clockHigh), graph);
            }
        }
        return graph;
    }

    private void putPortInsDeps(Object node, ModInstExt mi, boolean state, Map<Svar<PathExt>, BigInteger> portMasks, Map<Object, Set<Object>> graph)
    {
        Set<Object> deps = new LinkedHashSet<>();
        if (state)
        {
            deps.add(STATE);
        }
        for (Map.Entry<Svar<PathExt>, BigInteger> e : portMasks.entrySet())
        {
            Svar<PathExt> svar = e.getKey();
            BigInteger mask = e.getValue();
            PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
            PathExt.PortInst piIn = mi.portInstsIndex.get(lw.wire.getName());
            addPortInDeps(piIn, mask, deps);
        }
        graph.put(node, deps);
    }

    private void putVarMasksDeps(Object node, Map<Svar<PathExt>, BigInteger> varMasks, Map<Object, Set<Object>> graph)
    {
        Set<Object> deps = new LinkedHashSet<>();
        for (Map.Entry<Svar<PathExt>, BigInteger> e : varMasks.entrySet())
        {
            Svar<PathExt> svar = e.getKey();
            BigInteger mask = e.getValue();
            addWireDeps(svar, mask, deps);
        }
        graph.put(node, deps);
    }

    private void addPortInDeps(PathExt.PortInst piIn, BigInteger mask, Set<Object> deps)
    {
        if (piIn.driver instanceof DriverExt)
        {
            addDriverDeps(piIn.getDriverExt(), mask, deps);
        } else
        {
            addLhsDeps(piIn.getDriverLhs(), mask, deps);
        }
    }

    private void addLhsDeps(Lhs<PathExt> lhs, BigInteger mask, Set<Object> deps)
    {
        for (Lhrange<PathExt> lr : lhs.ranges)
        {
            BigInteger mask1 = BigIntegerUtil.loghead(lr.getWidth(), mask).shiftLeft(lr.getRsh());
            addWireDeps(lr.getVar(), mask1, deps);
            mask = mask.shiftRight(lr.getWidth());
        }
    }

    private void addWireDeps(Svar<PathExt> svar, BigInteger mask, Set<Object> deps)
    {
        if (mask.signum() == 0)
        {
            return;
        }
        if (svar.getDelay() != 0)
        {
            deps.add(STATE);
            return;
        }
        PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
        if (lw.wire.isInput())
        {
            for (int bit = 0; bit < lw.getWidth(); bit++)
            {
                if (mask.testBit(bit))
                {
                    deps.add(lw.getBit(bit));
                }
            }
            return;
        }
        for (Map.Entry<Lhrange<PathExt>, WireExt.WireDriver> e : lw.wire.drivers.entrySet())
        {
            Lhrange<PathExt> lhr = e.getKey();
            WireExt.WireDriver wd = e.getValue();
            BigInteger mask1 = BigIntegerUtil.loghead(lhr.getWidth(), mask.shiftRight(lhr.getRsh()));
            if (lhr.getVar().getDelay() == 0 && mask1.signum() > 0)
            {
                if (wd.driver != null)
                {
                    addDriverDeps(wd.driver, mask1.shiftLeft(wd.lsh), deps);
                }
                if (wd.pi != null)
                {
                    assert wd.pi.wire.isOutput();
                    if (wd.pi.splitIt)
                    {
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            if (mask1.testBit(i))
                            {
                                deps.add(wd.pi.getBit(wd.lsh + i));
                            }
                        }
                    } else
                    {
                        deps.add(wd.pi);
                    }
                }
            }
        }
    }

    private void addDriverDeps(DriverExt driver, BigInteger mask, Set<Object> deps)
    {
        if (driver.splitIt)
        {
            for (int bit = 0; bit < driver.getWidth(); bit++)
            {
                if (mask.testBit(bit))
                {
                    deps.add(driver.getBit(bit));
                }
            }
        } else if (mask.signum() > 0)
        {
            deps.add(driver);
        }
    }

    Map<Object, Set<Object>> computeFineDepsGraph(boolean clockHigh)
    {
        Map<Object, Set<Object>> graph = new LinkedHashMap<>();
        for (WireExt w : wires)
        {
            /*
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
                            PathExt.Bit pb = w.getBit(rsh + i);
                            assert pb == wd.driver.getBit(wd.lsh + i);
                            Set<Object> dep = new LinkedHashSet<>();
                            dep.add(wd.driver.getBit(wd.lsh + i));
//                            addDriverFineDeps(wd.driver, wd.lsh + i, dep);
                            graph.put(pb, dep);
                        }
                    } else
                    {
                        PathExt.PortInst piOut = wd.pi;
                        assert piOut != null;
//                        ModInstExt inst = piOut.inst;
//                        BitSet fineBitStateTransDeps = piOut.wire.getFineBitStateDeps(clockHigh);
//                        List<Map<Svar<PathExt>, BigInteger>> fineBitTransDeps = piOut.wire.getFineBitDeps(clockHigh);
                        for (int i = 0; i < lhr.getWidth(); i++)
                        {
                            PathExt.Bit wb = w.getBit(rsh + i);
                            assert wb == piOut.getParentBit(wd.lsh + i);
                            graph.put(wb, Collections.singleton(piOut.getBit(wd.lsh + i)));
//                            Set<Object> dep = new LinkedHashSet<>();
//                            if (fineBitStateTransDeps.get(i))
//                            {
//                                dep.add(STATE);
//                            }
//                            Map<Svar<PathExt>, BigInteger> fineBitDep = fineBitTransDeps.get(wd.lsh + i);
//                            for (Map.Entry<Svar<PathExt>, BigInteger> e1 : fineBitDep.entrySet())
//                            {
//                                PathExt.LocalWire lw = (PathExt.LocalWire)e1.getKey().getName();
//                                BigInteger mask = e1.getValue();
//                                PathExt.PortInst piIn = inst.portInsts.get(lw.wire.getName());
//                                Util.check(piIn != null);
//                                for (int bit = 0; bit < piIn.getWidth(); bit++)
//                                {
//                                    if (mask.testBit(bit))
//                                    {
//                                        assert piIn.getParentBit(bit) != null;
//                                        dep.add(piIn.getParentBit(bit));
//                                    }
//                                }
//                            }
//                            graph.put(wb, dep);
                        }
                    }
                }
            }
             */
            if (!w.isInput())
            {
                BigInteger assignedBits = w.getAssignedBits();
                for (int bit = 0; bit < w.getWidth(); bit++)
                {
                    if (!assignedBits.testBit(bit))
                    {
                        PathExt.Bit pb = w.getBit(bit);
                        graph.put(pb, Collections.emptySet());
                    }
                }
            }
        }
        for (ModInstExt inst : insts)
        {
            for (PathExt.PortInst pi : inst.portInsts)
            {
                if (pi.wire.isOutput())
                {
                    assert pi.source.width() == pi.getWidth();
                    assert pi.driver == null;
                    BitSet fineBitStateTransDeps = pi.wire.getFineBitStateDeps(clockHigh);
                    List<Map<Svar<PathExt>, BigInteger>> fineBitTransDeps = pi.wire.getFineBitDeps(clockHigh);
                    for (int bitOut = 0; bitOut < pi.getWidth(); bitOut++)
                    {
                        Set<Object> dep = new LinkedHashSet<>();
                        if (fineBitStateTransDeps.get(bitOut))
                        {
                            dep.add(STATE);
                        }
                        Map<Svar<PathExt>, BigInteger> fineBitDep = fineBitTransDeps.get(bitOut);
                        for (Map.Entry<Svar<PathExt>, BigInteger> e1 : fineBitDep.entrySet())
                        {
                            PathExt.LocalWire lw = (PathExt.LocalWire)e1.getKey().getName();
                            BigInteger mask = e1.getValue();
                            PathExt.PortInst piIn = inst.portInstsIndex.get(lw.wire.getName());
                            Util.check(piIn != null);
                            for (int bitIn = 0; bitIn < piIn.getWidth(); bitIn++)
                            {
                                assert piIn.getBit(bitIn) != null;
                                if (mask.testBit(bitIn))
                                {
                                    dep.add(piIn.getBit(bitIn));
                                }
                            }
                        }
                        graph.put(pi.getBit(bitOut), dep);
                        graph.put(pi.getParentBit(bitOut), Collections.singleton(pi.getBit(bitOut)));
                    }

                } else
                {
                    assert pi.wire.isInput();
                    assert pi.source == null;
                    if (pi.driver instanceof DriverExt)
                    {
                        for (int bit = 0; bit < pi.getWidth(); bit++)
                        {
                            Util.check(pi.getBit(bit) == pi.getParentBit(bit));
                        }
                    } else
                    {
                        assert pi.driver instanceof Lhs;
                        for (int bit = 0; bit < pi.getWidth(); bit++)
                        {
                            graph.put(pi.getBit(bit), Collections.singleton(pi.getParentBit(bit)));
                        }
                    }
                }
            }
        }
        for (DriverExt drv : assigns.values())
        {
            for (int bitDrv = 0; bitDrv < drv.getWidth(); bitDrv++)
            {
                Set<Object> dep = new LinkedHashSet<>();
                Map<Svar<PathExt>, BigInteger> varMasks = drv.getFineBitLocDeps(clockHigh).get(bitDrv);
                for (Map.Entry<Svar<PathExt>, BigInteger> e : varMasks.entrySet())
                {
                    Svar<PathExt> svar = e.getKey();
                    BigInteger maskIn = e.getValue();
                    if (svar.getDelay() == 0)
                    {
                        PathExt pathExt = svar.getName();
                        assert maskIn.signum() >= 0;
                        for (int bitIn = 0; bitIn < maskIn.bitLength(); bitIn++)
                        {
                            if (maskIn.testBit(bitIn))
                            {
                                PathExt.Bit pb = pathExt.getBit(bitIn);
                                assert pb != null;
                                dep.add(pb);
                            }
                        }
                    } else
                    {
                        dep.add(STATE);
                    }
                }
                graph.put(drv.getBit(bitDrv), dep);
            }
        }
        /*
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
                PathExt.Bit wb = drv.getBit(bit);
                assert wb == pi.getParentBit(bit);
                Set<Object> dep = new LinkedHashSet<>();
                addDriverFineDeps(drv, clockHigh, bit, dep);
                graph.put(wb, dep);
            }
        }
         */
        return graph;
    }

    private void markInstancesToSplit(Map<Object, Set<Object>> transdep, boolean clockHigh)
    {
        for (ModInstExt inst : insts)
        {
            for (PathExt.PortInst piOut : inst.portInsts)
            {
                if (piOut.wire.isOutput())
                {
                    Set<Object> inputDeps = new HashSet<>();
                    Map<Svar<PathExt>, BigInteger> crudePortDeps = piOut.wire.getCrudePortDeps(clockHigh);
                    for (Map.Entry<Svar<PathExt>, BigInteger> e : crudePortDeps.entrySet())
                    {
                        Svar<PathExt> svar = e.getKey();
                        BigInteger mask = e.getValue();
                        PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                        PathExt.PortInst piIn = inst.portInstsIndex.get(lw.wire.getName());
                        for (int bit = 0; bit < piIn.getWidth(); bit++)
                        {
                            Set<Object> deps = transdep.get(piIn.getBit(bit));
                            if (deps != null)
                            {
                                inputDeps.addAll(deps);
                            }
                        }
                    }
                    /*
                    Set<WireExt> crudeInputs = piOut.wire.getCrudeCombinationalInputs(clockHigh);
                    Set<Object> inputDeps = new HashSet<>();
                    for (WireExt crudeInput : crudeInputs)
                    {
                        PathExt.PortInst piIn = inst.portInsts.get(crudeInput.getName());
                        for (int bit = 0; bit < piIn.getWidth(); bit++)
                        {
                            Set<Object> deps = transdep.get(piIn.getBit(bit));
                            if (deps != null)
                            {
                                inputDeps.addAll(deps);
                            }
                        }
                    }
                     */
                    Set<PathExt.Bit> outDeps = new LinkedHashSet<>();
                    for (int bit = 0; bit < piOut.getWidth(); bit++)
                    {
                        if (inputDeps.contains(piOut.getBit(bit)))
                        {
                            outDeps.add(piOut.getProtoBit(bit));
                        }
                    }
                    if (!outDeps.isEmpty())
                    {
                        piOut.splitIt = true;
//                        System.out.println(modName + " " + piOut + " clock=" + (clockHigh ? "1" : "0") + " some combinational inputs depend on this output:");
//                        System.out.print("   ");
//                        for (WireExt.Bit wireBit : outDeps)
//                        {
//                            System.out.print(" " + wireBit);
//                        }
//                        System.out.println();
                    }
                }
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
                PathExt pathExt = svar.getName();
                for (int bit = 0; bit < pathExt.getWidth(); bit++)
                {
                    if (mask.testBit(bit))
                    {
                        Set<Object> deps = transdep.get(pathExt.getBit(bit));
                        if (deps != null)
                        {
                            inputDeps.addAll(deps);
                        }
                    }
                }
            }
            Set<PathExt.Bit> outDeps = new LinkedHashSet<>();
            for (int bit = 0; bit < lhs.width(); bit++)
            {
                PathExt.Bit pb = drv.getBit(bit);
                assert pb != null;
                if (inputDeps.contains(pb))
                {
                    outDeps.add(pb);
                }
            }
            if (!outDeps.isEmpty())
            {
                drv.splitIt = true;
//                System.out.println(modName + " " + drv.name + " clock=" + (clockHigh ? "1" : "0") + " some combinational inputs depend on these outputs:");
//                System.out.print("   ");
//                for (WireExt.Bit wireBit : outDeps)
//                {
//                    System.out.print(" " + wireBit);
//                }
//                System.out.println();
            }
        }
    }

    void markPortInstancesToSplit(String[] portInstancesToSplit)
    {
        for (String portInstanceToSplit : portInstancesToSplit)
        {
            int indexOfDot = portInstanceToSplit.indexOf('.');
            String instStr = portInstanceToSplit.substring(0, indexOfDot);
            String portStr = portInstanceToSplit.substring(indexOfDot + 1);
            Name instName = new Name(ACL2Object.valueOf(instStr));
            Name portName = new Name(ACL2Object.valueOf(portStr));
            ModInstExt inst = instsIndex.get(instName);
            PathExt.PortInst pi = inst.portInstsIndex.get(portName);
            pi.splitIt = true;
        }
    }

    void markDriversToSplit(int[] driversToSplit)
    {
        for (int driverToSplit : driversToSplit)
        {
            Iterator<DriverExt> it = assigns.values().iterator();
            for (int i = 0; i < driverToSplit; i++)
            {
                it.next();
            }
            it.next().splitIt = true;
        }
    }

    static Map<Svar<PathExt>, BigInteger> combineDeps(List<Map<Svar<PathExt>, BigInteger>> deps)
    {
        Map<Svar<PathExt>, BigInteger> result = new LinkedHashMap<>();
        for (Map<Svar<PathExt>, BigInteger> dep : deps)
        {
            for (Map.Entry<Svar<PathExt>, BigInteger> e : dep.entrySet())
            {
                Svar<PathExt> svar = e.getKey();
                BigInteger mask = e.getValue();
                if (mask.signum() > 0)
                {
                    BigInteger oldMask = result.get(svar);
                    if (oldMask == null)
                    {
                        oldMask = BigInteger.ZERO;
                    }
                    result.put(svar, oldMask.or(mask));
                }
            }
        }
        return result;
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

    public String showFinePortDeps(PathExt.Bit[] pathBits, Map<Object, Set<Object>> graph0, Map<Object, Set<Object>> graph1)
    {
        BitSet fineBitState0 = new BitSet();
        BitSet fineBitState1 = new BitSet();
        List<Map<Svar<PathExt>, BigInteger>> fineBitDeps0 = gatherFineBitDeps(fineBitState0, pathBits, graph0);
        List<Map<Svar<PathExt>, BigInteger>> fineBitDeps1 = gatherFineBitDeps(fineBitState1, pathBits, graph1);

        boolean fineState0 = !fineBitState0.isEmpty();
        Map<Svar<PathExt>, BigInteger> fineExportDeps0 = sortDeps(combineDeps(fineBitDeps0));
        boolean fineState1 = !fineBitState1.isEmpty();
        Map<Svar<PathExt>, BigInteger> fineExportDeps1 = sortDeps(combineDeps(fineBitDeps1));

        return showFineDeps(fineState0, fineExportDeps0, fineState1, fineExportDeps1);
    }

    public String showCrudePortDeps(Object node, Map<Object, Set<Object>> graph0, Map<Object, Set<Object>> graph1)
    {
        Map<Svar<PathExt>, BigInteger> dep0 = new LinkedHashMap<>();
        Map<Svar<PathExt>, BigInteger> dep1 = new LinkedHashMap<>();
        boolean stateDep0 = gatherDep(dep0, node, graph0);
        boolean stateDep1 = gatherDep(dep1, node, graph1);
        return showFineDeps(stateDep0, sortDeps(dep0), stateDep1, sortDeps(dep1));
    }

    public static String showFineDeps(
        BitSet stateDeps0,
        List<Map<Svar<PathExt>, BigInteger>> deps0,
        BitSet stateDeps1,
        List<Map<Svar<PathExt>, BigInteger>> deps1,
        int bit)
    {
        return showFineDeps(
            stateDeps0.get(bit), deps0.get(bit),
            stateDeps1.get(bit), deps1.get(bit));
    }

    public static String showFineDeps(
        boolean stateDep0,
        Map<Svar<PathExt>, BigInteger> dep0,
        boolean stateDep1,
        Map<Svar<PathExt>, BigInteger> dep1)
    {
        if (dep0.equals(dep1) && stateDep0 == stateDep1)
        {
            return showFineDeps(stateDep0, dep0);
        } else
        {
            return "0=>" + showFineDeps(stateDep0, dep0)
                + " | 1=>" + showFineDeps(stateDep1, dep1);
        }
    }

    private static String showFineDeps(boolean stateDep, Map<Svar<PathExt>, BigInteger> dep)
    {
        String s = stateDep ? "STATE" : "";
        for (Map.Entry<Svar<PathExt>, BigInteger> e : dep.entrySet())
        {
            Svar<PathExt> svar = e.getKey();
            BigInteger mask = e.getValue();
            if (!s.isEmpty())
            {
                s += ",";
            }
            s += svar.toString(mask);
        }
        return s;
    }

    List<Map<Svar<PathExt>, BigInteger>> gatherFineBitDeps(BitSet stateDeps, PathExt.Bit[] pathBits, Map<Object, Set<Object>> graph)
    {
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = new ArrayList<>();
        stateDeps.clear();
        for (int bit = 0; bit < pathBits.length; bit++)
        {
            Map<Svar<PathExt>, BigInteger> fineDep = new LinkedHashMap<>();
            if (gatherDep(fineDep, pathBits[bit], graph))
            {
                stateDeps.set(bit);
            }
//            for (Object o : graph.get(pathBits[bit]))
//            {
//                if (o.equals(ModuleExt.STATE))
//                {
//                    stateDeps.set(bit);
//                    continue;
//                }
//                PathExt.Bit pbIn = (PathExt.Bit)o;
//                BigInteger mask = fineDep.get(pbIn.getPath().getVar(0));
//                if (mask == null)
//                {
//                    mask = BigInteger.ZERO;
//                }
//                fineDep.put(pbIn.getPath().getVar(0), mask.setBit(pbIn.bit));
//            }
            fineDeps.add(fineDep);
        }
        return fineDeps;
    }

    boolean gatherDep(Map<Svar<PathExt>, BigInteger> dep, Object node, Map<Object, Set<Object>> graph)
    {
        boolean state = false;
        dep.clear();
        for (Object o : graph.get(node))
        {
            if (o.equals(ModuleExt.STATE))
            {
                state = true;
            } else if (o instanceof PathExt)
            {
                PathExt pathExt = (PathExt)o;
                dep.put(pathExt.getVar(0), BigIntegerUtil.logheadMask(pathExt.getWidth()));
            } else if (o instanceof DriverExt)
            {
                DriverExt drv = (DriverExt)o;
                for (int bit = 0; bit < drv.getWidth(); bit++)
                {
                    gatherBitDep(dep, drv.getBit(bit));
                }
            } else
            {
                PathExt.Bit pb = (PathExt.Bit)o;
                gatherBitDep(dep, pb);
            }
        }
        return state;
    }

    void gatherBitDep(Map<Svar<PathExt>, BigInteger> dep, PathExt.Bit pb)
    {
        BigInteger mask = dep.get(pb.getPath().getVar(0));
        if (mask == null)
        {
            mask = BigInteger.ZERO;
        }
        dep.put(pb.getPath().getVar(0), mask.setBit(pb.bit));
    }

    Map<Svar<PathExt>, BigInteger> sortDeps(Map<Svar<PathExt>, BigInteger> deps)
    {
        Map<Svar<PathExt>, BigInteger> sortedDeps = new TreeMap<>(this);
        sortedDeps.putAll(deps);
        assert sortedDeps.size() == deps.size();
        return sortedDeps;
//        Map<Svar<PathExt>, BigInteger> sortedDeps = new LinkedHashMap<>();
//        for (WireExt wire : wires)
//        {
//            Svar<PathExt> svar = wire.getVar(1);
//            BigInteger mask = deps.get(svar);
//            if (mask != null && mask.signum() > 0)
//            {
//                sortedDeps.put(svar, mask);
//            }
//        }
//        for (WireExt wire : wires)
//        {
//            Svar<PathExt> svar = wire.getVar(0);
//            BigInteger mask = deps.get(svar);
//            if (mask != null && mask.signum() > 0)
//            {
//                sortedDeps.put(svar, mask);
//            }
//        }
//        for (Map.Entry<Svar<PathExt>, BigInteger> e : deps.entrySet())
//        {
//            Svar<PathExt> svar = e.getKey();
//            BigInteger mask = e.getValue();
//            assert mask.signum() > 0;
//            sortedDeps.put(svar, mask);
//        }
//        assert sortedDeps.equals(deps);
//        return sortedDeps;
    }

    Set<WireExt> sortWires(Set<WireExt> wires)
    {
        Set<WireExt> sortedWires = new LinkedHashSet<>();
        for (WireExt wire : this.wires)
        {
            if (wires.contains(wire))
            {
                sortedWires.add(wire);
            }
        }
        for (WireExt wire : wires)
        {
            sortedWires.add(wire);
        }
        assert sortedWires.equals(wires);
        return sortedWires;
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
    @Override
    public int compare(Svar<PathExt> o1, Svar<PathExt> o2)
    {
        if (o1.getDelay() > o2.getDelay())
            return -1;
        if (o1.getDelay() < o2.getDelay())
            return 1;
        PathExt p1 = o1.getName();
        PathExt p2 = o2.getName();
        if (p1 instanceof PathExt.LocalWire)
        {
            if (p2 instanceof PathExt.LocalWire)
            {
                PathExt.LocalWire lw1 = (PathExt.LocalWire)p1;
                PathExt.LocalWire lw2 = (PathExt.LocalWire)p2;
                return Integer.compare(lw1.wire.index, lw2.wire.index);
            } else
            {
                return -1;
            }
        } else
        {
            if (p2 instanceof PathExt.LocalWire)
            {
                return 1;
            } else
            {
                PathExt.PortInst pi1 = (PathExt.PortInst)p1;
                PathExt.PortInst pi2 = (PathExt.PortInst)p2;
                String s1 = pi1.inst.getInstname().toString();
                String s2 = pi2.inst.getInstname().toString();
                int res = s1.compareTo(s2);
                if (res != 0)
                {
                    return res;
                }
                return Integer.compare(pi1.wire.index, pi2.wire.index);
            }
        }
    }
}
