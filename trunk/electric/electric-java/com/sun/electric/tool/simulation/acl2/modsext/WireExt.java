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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final Bit[] bits;

    public boolean used, exported;
    BigInteger assignedBits;
    String global;
    final SortedMap<Lhrange<PathExt>, WireDriver> drivers = new TreeMap<>(LHRANGE_COMPARATOR);

    // only for exports
    List<Map<Svar<PathExt>, BigInteger>> fineDeps0;
    List<Map<Svar<PathExt>, BigInteger>> fineDeps1;
    boolean specialOutput;

    WireExt(ModuleExt parent, Wire b)
    {
        this.parent = parent;
        this.b = b;
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
        bits = new Bit[getWidth()];
        for (int bit = 0; bit < bits.length; bit++)
        {
            bits[bit] = new Bit(null, bit);
        }
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

    public Bit getBit(int bit)
    {
        return bits[bit];
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

    public List<Map<Svar<PathExt>, BigInteger>> getFineDeps(boolean clockHigh)
    {
//        assert exported && isAssigned();
        return clockHigh ? fineDeps1 : fineDeps0;
    }

    public String showFineDeps(int bit)
    {
        return showFineDeps(getFineDeps(false), getFineDeps(true), bit);
    }

    public static String showFineDeps(
        List<Map<Svar<PathExt>, BigInteger>> deps0,
        List<Map<Svar<PathExt>, BigInteger>> deps1,
        int bit)
    {
        Map<Svar<PathExt>, BigInteger> dep0 = deps0.get(bit);
        Map<Svar<PathExt>, BigInteger> dep1 = deps1.get(bit);
        if (dep0.equals(dep1))
        {
            return showFineDeps(dep0);
        } else
        {
            return "0=>" + showFineDeps(dep0) + " | 1=>" + showFineDeps(dep1);
        }
    }

    private static String showFineDeps(Map<Svar<PathExt>, BigInteger> dep)
    {
        String s = "";
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

    void setFineDeps(boolean clockHigh, Map<Object, Set<Object>> closure)
    {
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = gatherFineDeps(closure);
        if (clockHigh)
        {
            fineDeps1 = fineDeps;
        } else
        {
            fineDeps0 = fineDeps;
        }
    }

    List<Map<Svar<PathExt>, BigInteger>> gatherFineDeps(Map<Object, Set<Object>> graph)
    {
        List<Map<Svar<PathExt>, BigInteger>> fineDeps = new ArrayList<>();
        fineDeps.clear();
        for (int i = 0; i < getWidth(); i++)
        {
            Bit wb = getBit(i);
            Map<Svar<PathExt>, BigInteger> fineDep = new LinkedHashMap<>();
            for (Object o : graph.get(wb))
            {
                Bit wb1 = (Bit)o;
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

    private static final Comparator<Lhrange> LHRANGE_COMPARATOR = new Comparator<Lhrange>()
    {
        @Override
        public int compare(Lhrange o1, Lhrange o2)
        {
            return Integer.compare(o1.getRsh(), o2.getRsh());
        }
    };

    public Bit newBit(PathExt.PortInst pi, int bit)
    {
        Util.check(pi.wire == WireExt.this);
        return new Bit(pi, bit);
    }

    public class Bit
    {
        final PathExt.PortInst pi;
        final int bit;

        private Bit(PathExt.PortInst pi, int bit)
        {
            this.pi = pi;
            this.bit = bit;
        }

        public WireExt getWire()
        {
            return WireExt.this;
        }

        @Override
        public String toString()
        {
            String s = WireExt.this.toString(BigInteger.ONE.shiftLeft(bit));
            if (pi != null)
            {
                s = pi.inst.getInstname() + "." + s;
            }
            return s;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Bit)
            {
                Bit that = (Bit)o;
                return this.getWire().equals(that.getWire())
                    && Objects.equals(this.pi, that.pi)
                    && this.bit == that.bit;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 29 * hash + WireExt.this.hashCode();
            hash = 29 * hash + Objects.hashCode(pi);
            hash = 29 * hash + bit;
            return hash;
        }
    }
}
