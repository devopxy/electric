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
package com.sun.electric.tool.simulation.acl2.modsext;

import com.sun.electric.tool.simulation.acl2.mods.Driver;
import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.Util;
import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
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
    final List<Map<Svar<PathExt>, BigInteger>> fineBitLocDeps0 = new ArrayList<>();
    final List<Map<Svar<PathExt>, BigInteger>> fineBitLocDeps1 = new ArrayList<>();

    PathExt.Bit[] pathBits;
    boolean splitIt;

    DriverExt(ModuleExt parent, Driver<PathExt> b, String name)
    {
        this.parent = parent;
        this.b = b;
        this.name = name;
        Util.check(b.strength == 6);
        for (Svar<PathExt> svar : b.vars)
        {
            Util.check(svar.getName() instanceof WireExt);
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

    public int getWidth()
    {
        return pathBits.length;
    }

    public PathExt.Bit getBit(int bit)
    {
        return pathBits[bit];
    }

    @Override
    public String toString()
    {
        assert getStrength() == 6;
        return name != null ? name : getSvex().toString();
    }

    void setSource(Lhs<PathExt> lhs)
    {
        assert pathBits == null;
        pathBits = new PathExt.Bit[lhs.width()];
        int lsh = 0;
        for (Lhrange<PathExt> range : lhs.ranges)
        {
            Svar<PathExt> svar = range.getVar();
            Util.check(svar.getDelay() == 0);
            PathExt pathExt = svar.getName();
            for (int bit = 0; bit < range.getWidth(); bit++)
            {
                pathBits[lsh + bit] = pathExt.getBit(range.getRsh() + bit);
                if (pathExt instanceof PathExt.PortInst)
                {
                    Util.check(pathBits[lsh + bit] == ((PathExt.PortInst)pathExt).getParentBit(lsh + bit));
                }
            }
            lsh += range.getWidth();
        }
    }

    public final List<Svar<PathExt>> collectVars()
    {
        return b.vars;
    }

    void markUsed()
    {
        for (Svar<PathExt> svar : collectVars())
        {
            ((WireExt)svar.getName()).markUsed();
        }
    }

    Map<Svar<PathExt>, BigInteger> getCrudeDeps(boolean clockHigh)
    {
        return clockHigh ? crudeDeps1 : crudeDeps0;
    }

    List<Map<Svar<PathExt>, BigInteger>> getFineBitLocDeps(boolean clockHigh)
    {
        return clockHigh ? fineBitLocDeps1 : fineBitLocDeps0;
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
        BigInteger mask = BigIntegerUtil.logheadMask(width);
        Map<Svar<PathExt>, BigInteger> varsWithMasks = patched.collectVarsWithMasks(mask, true);
        if (clkVal)
        {
            crudeDeps1 = varsWithMasks;
        } else
        {
            crudeDeps0 = varsWithMasks;
        }
        Map<Svar<PathExt>, BigInteger> crudeDepsCheck = new HashMap<>();
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = getFineBitLocDeps(clkVal);
        fineDeps.clear();
        for (int bit = 0; bit < width; bit++)
        {
            mask = BigInteger.ONE.shiftLeft(bit);
            Map<Svar<PathExt>, BigInteger> bitVarsWithMasks = patched.collectVarsWithMasks(mask, true);
            fineDeps.add(bitVarsWithMasks);
            for (Map.Entry<Svar<PathExt>, BigInteger> e : bitVarsWithMasks.entrySet())
            {
                Svar<PathExt> svar = e.getKey();
                mask = e.getValue();
                if (mask == null || mask.signum() == 0)
                {
                    continue;
                }
                if (svar.getDelay() != 0)
                {
                    assert svar.getDelay() == 1;
                    Map<Svar<PathExt>, BigInteger> stateVars = clkVal ? parent.stateVars1 : parent.stateVars0;
                    BigInteger oldMask = stateVars.get(svar);
                    if (oldMask == null)
                    {
                        oldMask = BigInteger.ZERO;
                    }
                    stateVars.put(svar, oldMask.or(mask));
                }
                BigInteger crudeMask = crudeDepsCheck.get(svar);
                if (crudeMask == null)
                {
                    crudeMask = BigInteger.ZERO;
                }
                crudeDepsCheck.put(svar, crudeMask.or(mask));
            }
        }
        Util.check(varsWithMasks.equals(crudeDepsCheck));
    }

    public String showFinePortDeps(Map<Object, Set<Object>> graph0, Map<Object, Set<Object>> graph1)
    {
        return parent.showFinePortDeps(pathBits, graph0, graph1);
    }

    List<Map<Svar<PathExt>, BigInteger>> gatherFineBitDeps(BitSet stateDeps, Map<Object, Set<Object>> graph)
    {
        return parent.gatherFineBitDeps(stateDeps, pathBits, graph);
    }
}
