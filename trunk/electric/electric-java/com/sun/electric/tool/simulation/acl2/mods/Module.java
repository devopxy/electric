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
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            pair = o;
            Lhs lhs = new Lhs(this, car(pair));
            for (Lhrange lhr : lhs.ranges)
            {
                Util.check(lhr.atom instanceof Lhatom.Var);
            }
            Driver drv = new Driver(this, cdr(pair));
            Driver old = assigns.put(lhs, drv);
            Util.check(old == null);
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

    @Override
    public SVarExt newVar(ACL2Object name, int delay, boolean nonblocking)
    {
        Util.check(!nonblocking);
        if (consp(name).bool())
        {
            Util.check(delay == 0);
            return new SVarExt.PortInst(this, name);
        } else
        {
            return new SVarExt.LocalWire(this, name, delay);
        }
    }
}
