/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DriverExt.java
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
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.Set;

/**
 * Driver - SVEX expression with strength.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____DRIVER>.
 */
public class DriverExt
{
    final ModuleExt parent;

    public final Svex svex;
    final int strength;
    final String name;

    Map<SVarExt.LocalWire, BigInteger> crudeDeps0;
    Map<SVarExt.LocalWire, BigInteger> crudeDeps1;
    final List<Map<SVarExt.LocalWire, BigInteger>> fineDeps0 = new ArrayList<>();
    final List<Map<SVarExt.LocalWire, BigInteger>> fineDeps1 = new ArrayList<>();

    DriverExt(ModuleExt parent, ACL2Object impl, String name)
    {
        this.parent = parent;
        this.name = name;
        svex = Svex.valueOf(parent, car(impl), parent.svexCache);
        strength = cdr(impl).intValueExact();
        Util.check(strength == 6);
        for (Svar svar : svex.collectVars())
        {
            Util.check(svar instanceof SVarExt.LocalWire);
        }
        Util.check(strength >= 0);
    }

    @Override
    public String toString()
    {
        assert strength == 6;
        return svex.toString();
    }

    public final Set<SVarExt.LocalWire> collectVars()
    {
        return svex.collectVars(SVarExt.LocalWire.class);
    }

    void markUsed()
    {
        for (SVarExt.LocalWire svar : collectVars())
        {
            svar.markUsed();
        }
    }

    Map<SVarExt.LocalWire, BigInteger> getCrudeDeps(boolean clockHigh)
    {
        return clockHigh ? crudeDeps1 : crudeDeps0;
    }

    List<Map<SVarExt.LocalWire, BigInteger>> getFineDeps(boolean clockHigh)
    {
        return clockHigh ? fineDeps1 : fineDeps0;
    }

    void computeDeps(int width, boolean clkVal, Map<Svar, Vec4> env, Map<SvexCall, SvexCall> patchMemoize)
    {
        Svex patched = svex.patch(env, patchMemoize);
        BigInteger mask = BigIntegerUtil.MINUS_ONE;
        Map<SVarExt.LocalWire, BigInteger> varsWithMasks = patched.collectVarsWithMasks(mask, SVarExt.LocalWire.class);
        if (clkVal)
        {
            crudeDeps1 = varsWithMasks;
        } else
        {
            crudeDeps0 = varsWithMasks;
        }
        List<Map<SVarExt.LocalWire, BigInteger>> fineDeps = getFineDeps(clkVal);
        fineDeps.clear();
        for (int bit = 0; bit < width; bit++)
        {
            mask = BigInteger.ONE.shiftLeft(bit);
            varsWithMasks = patched.collectVarsWithMasks(mask, SVarExt.LocalWire.class);
            fineDeps.add(varsWithMasks);
        }
    }
}
