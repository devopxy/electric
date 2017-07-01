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

    private final Driver<PathExt> b;
    final String name;

    Map<Svar<PathExt>, BigInteger> crudeDeps0;
    Map<Svar<PathExt>, BigInteger> crudeDeps1;
    final List<Map<Svar<PathExt>, BigInteger>> fineDeps0 = new ArrayList<>();
    final List<Map<Svar<PathExt>, BigInteger>> fineDeps1 = new ArrayList<>();

    DriverExt(ModuleExt parent, Driver<PathExt> b, String name)
    {
        this.parent = parent;
        this.b = b;
        this.name = name;
        Util.check(b.strength == 6);
        for (Svar<PathExt> svar : b.svex.collectVars())
        {
            Util.check(svar.getName() instanceof PathExt.LocalWire);
        }
    }

    public Svex<PathExt> getSvex()
    {
        return b.svex;
    }

    public int getStrength()
    {
        return b.strength;
    }

    @Override
    public String toString()
    {
        assert getStrength() == 6;
        return getSvex().toString();
    }

    public final Set<Svar<PathExt>> collectVars()
    {
        return getSvex().collectVars();
    }

    void markUsed()
    {
        for (Svar<PathExt> svar : collectVars())
        {
            ((PathExt.LocalWire)svar.getName()).markUsed();
        }
    }

    Map<Svar<PathExt>, BigInteger> getCrudeDeps(boolean clockHigh)
    {
        return clockHigh ? crudeDeps1 : crudeDeps0;
    }

    List<Map<Svar<PathExt>, BigInteger>> getFineDeps(boolean clockHigh)
    {
        return clockHigh ? fineDeps1 : fineDeps0;
    }

    /**
     * Compute crude and fine dependencies of this driver.
     * Driver SV expression is patched by assumption about clock value
     *
     * @param width width of this driver used by left-hand side
     * @param clkVal which clock value is assumed in the patch
     * @param env environment for the patch
     * @param patchMemoize memoization cache for patch
     */
    void computeDeps(int width, boolean clkVal, Map<Svar<PathExt>, Vec4> env,
        Map<SvexCall<PathExt>, SvexCall<PathExt>> patchMemoize)
    {
        Svex<PathExt> patched = getSvex().patch(env, patchMemoize);
        BigInteger mask = BigIntegerUtil.MINUS_ONE;
        Map<Svar<PathExt>, BigInteger> varsWithMasks = patched.collectVarsWithMasks(mask);
        if (clkVal)
        {
            crudeDeps1 = varsWithMasks;
        } else
        {
            crudeDeps0 = varsWithMasks;
        }
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = getFineDeps(clkVal);
        fineDeps.clear();
        for (int bit = 0; bit < width; bit++)
        {
            mask = BigInteger.ONE.shiftLeft(bit);
            varsWithMasks = patched.collectVarsWithMasks(mask);
            fineDeps.add(varsWithMasks);
        }
    }
}
