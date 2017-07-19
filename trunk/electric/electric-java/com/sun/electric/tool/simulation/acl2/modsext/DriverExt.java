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
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    WireExt.Bit[] wireBits;
    boolean splitIt;

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

    void setSource(Lhs<PathExt> lhs)
    {
        assert wireBits == null;
        wireBits = new WireExt.Bit[lhs.width()];
        int lsh = 0;
        for (Lhrange<PathExt> range : lhs.ranges)
        {
            Svar<PathExt> svar = range.getVar();
            Util.check(svar.getDelay() == 0);
            if (svar.getName() instanceof PathExt.LocalWire)
            {
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                for (int i = 0; i < range.getWidth(); i++)
                {
                    wireBits[lsh + i] = lw.wire.getBit(range.getRsh() + i);
                }
            } else
            {
                assert lhs.ranges.size() == 1 && range.getRsh() == 0;
                PathExt.PortInst pi = (PathExt.PortInst)svar.getName();
                for (int i = 0; i < range.getWidth(); i++)
                {
                    wireBits[i] = pi.wire.newBit(pi, i);
                }
            }
            lsh += range.getWidth();
        }
    }

//    void setSource(Lhrange<PathExt> range)
//    {
//        assert wireBits == null;
//        wireBits = new WireExt.Bit[range.getWidth()];
//        Svar<PathExt> svar = range.getVar();
//        Util.check(svar.getDelay() == 0);
//        PathExt.PortInst pi = (PathExt.PortInst)svar.getName();
//        for (int i = 0; i < range.getWidth(); i++)
//        {
//            wireBits[i] = pi.wire.newBit(pi, i);
//        }
//    }
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
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = getFineDeps(clkVal);
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

    List<Map<Svar<PathExt>, BigInteger>> gatherFineDeps(Map<Object, Set<Object>> graph)
    {
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = new ArrayList<>();
        fineDeps.clear();
        for (int i = 0; i < wireBits.length; i++)
        {
            WireExt.Bit wb = wireBits[i];
            Map<Svar<PathExt>, BigInteger> fineDep = new LinkedHashMap<>();
            for (Object o : graph.get(wb))
            {
                WireExt.Bit wb1 = (WireExt.Bit)o;
                BigInteger mask = fineDep.get(wb1.getWire().getVar(0));
                if (mask == null)
                {
                    mask = BigInteger.ZERO;
                }
                fineDep.put(wb1.getWire().getVar(0), mask.setBit(wb1.bit));
            }
            fineDeps.add(fineDep);
        }
        return fineDeps;
    }

}
