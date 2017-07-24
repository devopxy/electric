/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WireExt.java
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

import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Util;
import com.sun.electric.tool.simulation.acl2.mods.Wire;
import com.sun.electric.tool.simulation.acl2.mods.Wiretype;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import static com.sun.electric.util.acl2.ACL2.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Wire info as stored in an svex module.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____WIRE>.
 */
public class WireExt
{
    public final Wire b;

    final ModuleExt parent;
    final PathExt.LocalWire path;
    final int index;

    public boolean used, exported;
    BigInteger assignedBits;
    String global;
    final SortedMap<Lhrange<PathExt>, WireDriver> drivers = new TreeMap<>(LHRANGE_COMPARATOR);

    // only for exports
    private final BitSet fineBitStateDeps0 = new BitSet();
    private final BitSet fineBitStateDeps1 = new BitSet();
    private final List<Map<Svar<PathExt>, BigInteger>> fineBitDeps0 = new ArrayList<>();
    private final List<Map<Svar<PathExt>, BigInteger>> fineBitDeps1 = new ArrayList<>();
    final Map<Svar<PathExt>, BigInteger> crudePortDeps0 = new LinkedHashMap<>();
    final Map<Svar<PathExt>, BigInteger> crudePortDeps1 = new LinkedHashMap<>();
    boolean crudePortStateDep0;
    boolean crudePortStateDep1;

    WireExt(ModuleExt parent, Wire b, int index)
    {
        this.parent = parent;
        this.b = b;
        this.index = index;
        if (!stringp(b.name.impl).bool())
        {
            Util.check(integerp(b.name.impl).bool() || Util.KEYWORD_SELF.equals(b.name.impl));
            Util.check(parent.modName.isCoretype);
        }
        Util.check(b.delay == 0);
        Util.check(!b.revp);
//        if (b.low_idx != 0) {
//            System.out.println("Wire " + this + " in " + parent);
//        }
        path = new PathExt.LocalWire(this);
    }

    public Name getName()
    {
        return b.name;
    }

    public int getWidth()
    {
        return b.width;
    }

    public int getLowIdx()
    {
        return b.low_idx;
    }

    public int getDelay()
    {
        return b.delay;
    }

    public boolean isRev()
    {
        return b.revp;
    }

    public Wiretype getWiretype()
    {
        return b.wiretype;
    }

    public int getFirstIndex()
    {
        return b.getFirstIndex();
    }

    public int getSecondIndex()
    {
        return b.getSecondIndex();
    }

    public String toString(int width, int rsh)
    {
        return b.toString(width, rsh);
    }

    public String toString(BigInteger mask)
    {
        return b.toString(mask);
    }

    public String toLispString(int width, int rsh)
    {
        return b.toLispString(width, rsh);
    }

    @Override
    public String toString()
    {
        return b.toString();
    }

    public PathExt.Bit getBit(int bit)
    {
        return path.getBit(bit);
    }

    public void markAssigned(BigInteger assignedBits)
    {
        if (assignedBits.signum() == 0)
        {
            return;
        }
        Util.check(assignedBits.signum() >= 0 && assignedBits.bitLength() <= getWidth());
        if (this.assignedBits == null)
        {
            this.assignedBits = BigInteger.ZERO;
        }
        if (assignedBits.and(this.assignedBits).signum() != 0)
        {
            System.out.println(this + " has multiple assignement");
        }
        this.assignedBits = this.assignedBits.or(assignedBits);
    }

    public boolean isAssigned()
    {
        return assignedBits != null;
    }

    public boolean isExport()
    {
        return exported;
    }

    public boolean isInput()
    {
        return isExport() && !isAssigned();
    }

    public boolean isOutput()
    {
        return isExport() && isAssigned();
    }

    public BigInteger getAssignedBits()
    {
        return assignedBits != null ? assignedBits : BigInteger.ZERO;
    }

    public void markGlobal(String name)
    {
        if (global == null)
        {
            global = name;
        } else if (!global.equals(name))
        {
            global = "";
        }
    }

    public boolean isGlobal()
    {
        return global != null && !global.isEmpty();
    }

    /**
     * WireDriver is used together with Lhrange.
     * It says that Lhrange.width bits are driven either by
     * assignement driver[width+:lsh] or by port inst pi[width+:lsh] (without delay).
     */
    public static class WireDriver
    {
        public final int lsh;
        public final DriverExt driver;
        public final PathExt.PortInst pi;

        WireDriver(int lsh, DriverExt driver)
        {
            this.lsh = lsh;
            this.driver = driver;
            this.pi = null;
        }

        WireDriver(int lsh, PathExt.PortInst pi)
        {
            this.lsh = lsh;
            this.driver = null;
            this.pi = pi;
        }
    }

    /**
     * @param lr Lhrange from left-hand side. Must be this wire without delay
     * @param lsh offset in driver bits
     * @param driver either DriverExt or PathExt.PortInst
     */
    public void addDriver(Lhrange<PathExt> lr, int lsh, Object driver)
    {
        Svar<PathExt> svar = lr.getVar();
        assert svar.getDelay() == 0;
        PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
        Util.check(lw.wire == this);
        WireDriver wd;
        if (driver instanceof DriverExt)
        {
            wd = new WireDriver(lsh, (DriverExt)driver);
        } else if (driver instanceof PathExt.PortInst)
        {
            wd = new WireDriver(lsh, (PathExt.PortInst)driver);
        } else
        {
            throw new UnsupportedOperationException();
        }
        WireDriver old = drivers.put(lr, wd);
        Util.check(old == null);
    }

    public Svar<PathExt> getVar(int delay)
    {
        return parent.newVar(path, delay, false);
//        switch (delay)
//        {
//            case 0:
//                return curVar;
//            case 1:
//                return prevVar;
//            default:
//                throw new IllegalArgumentException();
//        }
    }

    BitSet getFineBitStateDeps(boolean clockHigh)
    {
        return clockHigh ? fineBitStateDeps1 : fineBitStateDeps0;
    }

    List<Map<Svar<PathExt>, BigInteger>> getFineBitDeps(boolean clockHigh)
    {
        return clockHigh ? fineBitDeps1 : fineBitDeps0;
    }

    boolean getFinePortStateDeps(boolean clcokHigh)
    {
        return !getFineBitStateDeps(clcokHigh).isEmpty();
    }

    Map<Svar<PathExt>, BigInteger> getFinePortDeps(boolean clockHigh)
    {
        return parent.sortDeps(ModuleExt.combineDeps(getFineBitDeps(clockHigh)));
    }

    boolean getCrudePortStateDeps(boolean clockHigh)
    {
        return clockHigh ? crudePortStateDep1 : crudePortStateDep0;
    }

    Map<Svar<PathExt>, BigInteger> getCrudePortDeps(boolean clockHigh)
    {
        return clockHigh ? crudePortDeps1 : crudePortDeps0;
    }

    void setFineDeps(boolean clockHigh, Map<Object, Set<Object>> closure)
    {
        if (clockHigh)
        {
            fineBitDeps1.addAll(gatherFineBitDeps(fineBitStateDeps1, closure));
        } else
        {
            fineBitDeps0.addAll(gatherFineBitDeps(fineBitStateDeps0, closure));
        }
    }

    public String showFinePortDeps(Map<Object, Set<Object>> graph0, Map<Object, Set<Object>> graph1)
    {
        return path.showFinePortDeps(graph0, graph1);
    }

    List<Map<Svar<PathExt>, BigInteger>> gatherFineBitDeps(BitSet stateDeps, Map<Object, Set<Object>> graph)
    {
        return path.gatherFineBitDeps(stateDeps, graph);
    }

    private static final Comparator<Lhrange> LHRANGE_COMPARATOR = new Comparator<Lhrange>()
    {
        @Override
        public int compare(Lhrange o1, Lhrange o2)
        {
            return Integer.compare(o1.getRsh(), o2.getRsh());
        }
    };
}
