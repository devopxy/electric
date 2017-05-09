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

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexFunction;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
            Lhs l = new Lhs(this, car(pair));
            Driver old = assigns.put(l, new Driver(this, cdr(pair)));
            Util.check(old == null);
        }

        pair = fields.get(3);
        Util.check(car(pair).equals(Util.SV_ALIASPAIRS));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            pair = o;
            Lhs l = new Lhs(this, car(pair));
            Util.check(l.ranges.size() == 1);
            Lhrange l0 = l.ranges.get(0);
            Util.check(l0.atom instanceof Lhatom.Var);
            Lhatom.Var atomVar = (Lhatom.Var)l0.atom;
            Util.check(atomVar.rsh == 0);
            SVarExt var = atomVar.name;
            Util.check(var.getDelay() == 0 && !var.isNonblocking());
            if (stringp(var.name).bool())
            {
                var = var;
            } else if (integerp(var.name).bool())
            {
            } else
            {
                Util.checkNotNil(consp(var.name));
            }
            Lhs r = new Lhs(this, cdr(pair));
            Lhs old = aliaspairs.put(l, r);
            Util.check(old == null);
        }
    }

    void check(Map<ModName, Module> downTop)
    {
        CheckRhsVisitor checkVisitor = new CheckRhsVisitor();
        for (Map.Entry<Lhs, Driver> e : assigns.entrySet())
        {
            e.getKey().markAssigned(Vec2.BI_MINUS_ONE);
            e.getKey().check(downTop, true);
            e.getValue().check(checkVisitor, downTop);
        }
        for (Map.Entry<Lhs, Lhs> e : aliaspairs.entrySet())
        {
            Lhs l = e.getKey();
            Lhs r = e.getValue();
            Util.check(l.size() == r.size());
            Util.check(l.ranges.size() == 1);
            Lhrange lr = l.ranges.get(0);
            Lhatom.Var atomVar = (Lhatom.Var)lr.atom;
            Util.check(atomVar.rsh == 0);
            SVarExt svar = atomVar.name;
            Util.check(svar.getDelay() == 0);
            assert !svar.isNonblocking();
//            svar.check(modalist);
            if (svar instanceof SVarExt.PortInst)
            {
                SVarExt.PortInst pi = (SVarExt.PortInst)svar;
                Util.check(lr.w == pi.wire.width);
                BigInteger assignedBits = pi.wire.getAssignedBits();
                r.markAssigned(assignedBits);
                r.check(downTop, svar.wire.isAssigned());
            } else
            {
                SVarExt.LocalWire lw = (SVarExt.LocalWire)svar;
                Util.check(lr.w == lw.wire.width);
                // Is it memory ?
                assert r.ranges.size() == 1;
                Lhrange rr = r.ranges.get(0);
                Lhatom.Var atomVar2 = (Lhatom.Var)rr.atom;
                SVarExt svar2 = atomVar2.name;
                assert svar2.getDelay() == 0;
                assert !svar2.isNonblocking();
                if (symbolp(svar2.name).bool())
                {
                    Util.check(svar2.name.equals(Util.KEYWORD_SELF));
                } else
                {
                    Util.check(cdr(svar2.name).equals(Util.KEYWORD_SELF));
                }
//                svar2.check(modalist);
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

    static class CheckRhsVisitor implements Svex.Visitor<Void, Map<ModName, Module>>
    {
        private final Set<Svex> visited = new HashSet<>();

        @Override
        public Void visitConst(Vec4 val, Map<ModName, Module> p)
        {
            return null;
        }

        @Override
        public Void visitVar(Svar name, Map<ModName, Module> p)
        {
            if (name instanceof SVarExt.LocalWire)
            {
                SVarExt.LocalWire lw = (SVarExt.LocalWire)name;
                lw.check(p, false);
            }
            return null;
        }

        @Override
        public Void visitCall(SvexFunction fun, Svex[] args, Map<ModName, Module> p)
        {
            for (Svex a : args)
            {
                if (!visited.contains(a))
                {
                    a.accept(this, p);
                    visited.add(a);
                }
            }
            return null;
        }
    }
}
