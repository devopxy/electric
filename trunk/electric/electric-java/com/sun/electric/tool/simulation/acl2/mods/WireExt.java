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
package com.sun.electric.tool.simulation.acl2.mods;

import static com.sun.electric.util.acl2.ACL2.*;
import java.math.BigInteger;
import java.util.ArrayList;
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
    final SVarExt.LocalWire curVar;
    final SVarExt.LocalWire prevVar;

    public boolean used, exported;
    BigInteger assignedBits;
    String global;
    final SortedMap<Lhrange, WireDriver> drivers = new TreeMap<>(LHRANGE_COMPARATOR);

    // only for exports
    List<Map<SVarExt.LocalWire, BigInteger>> fineDeps0;
    List<Map<SVarExt.LocalWire, BigInteger>> fineDeps1;

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
        curVar = new SVarExt.LocalWire(this, 0);
        prevVar = new SVarExt.LocalWire(this, 1);
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

    public static class WireDriver
    {
        public final int lsh;
        public final DriverExt driver;
        public final SVarExt.PortInst pi;

        WireDriver(int lsh, DriverExt driver)
        {
            this.lsh = lsh;
            this.driver = driver;
            this.pi = null;
        }

        WireDriver(int lsh, SVarExt.PortInst pi)
        {
            this.lsh = lsh;
            this.driver = null;
            this.pi = pi;
        }
    }

    public void addDriver(Lhrange lr, int lsh, Object driver)
    {
        SVarExt.LocalWire lw = (SVarExt.LocalWire)lr.getVar();
        Util.check(lw.wire == this);
        WireDriver wd;
        if (driver instanceof DriverExt)
        {
            wd = new WireDriver(lsh, (DriverExt)driver);
        } else if (driver instanceof SVarExt.PortInst)
        {
            wd = new WireDriver(lsh, (SVarExt.PortInst)driver);
        } else
        {
            throw new UnsupportedOperationException();
        }
        WireDriver old = drivers.put(lr, wd);
        Util.check(old == null);
    }

    public SVarExt.LocalWire getVar(int delay)
    {
        switch (delay)
        {
            case 0:
                return curVar;
            case 1:
                return prevVar;
            default:
                throw new IllegalArgumentException();
        }
    }

    public List<Map<SVarExt.LocalWire, BigInteger>> getFineDeps(boolean clockHigh)
    {
        assert exported && isAssigned();
        return clockHigh ? fineDeps1 : fineDeps0;
    }

    public String showFineDeps(int bit)
    {
        Map<SVarExt.LocalWire, BigInteger> dep0 = getFineDeps(false).get(bit);
        Map<SVarExt.LocalWire, BigInteger> dep1 = getFineDeps(true).get(bit);
        if (dep0.equals(dep1))
        {
            return showFineDeps(dep0);
        } else
        {
            return "0=>" + showFineDeps(dep0) + " | 1=>" + showFineDeps(dep1);
        }
    }

    private String showFineDeps(Map<SVarExt.LocalWire, BigInteger> dep)
    {
        String s = "";
        for (Map.Entry<SVarExt.LocalWire, BigInteger> e : dep.entrySet())
        {
            SVarExt.LocalWire lw = e.getKey();
            BigInteger mask = e.getValue();
            if (!s.isEmpty())
            {
                s += ",";
            }
            s += lw.toString(mask);
        }
        return s;
    }

    void setFineDeps(boolean clockHigh, Map<Object, Set<Object>> closure)
    {
        assert exported && isAssigned();
        List<Map<SVarExt.LocalWire, BigInteger>> fineDeps = new ArrayList<>();
        fineDeps.clear();
        for (int i = 0; i < getWidth(); i++)
        {
            ModuleExt.WireBit wb = new ModuleExt.WireBit(this, i);
            Map<SVarExt.LocalWire, BigInteger> fineDep = new LinkedHashMap<>();
            for (Object o : closure.get(wb))
            {
                ModuleExt.WireBit wb1 = (ModuleExt.WireBit)o;
                BigInteger mask = fineDep.get(wb1.wire.curVar);
                if (mask == null)
                {
                    mask = BigInteger.ZERO;
                }
                fineDep.put(wb1.wire.curVar, mask.setBit(wb1.bit));
            }
            fineDeps.add(fineDep);
        }
        if (clockHigh)
        {
            fineDeps1 = fineDeps;
        } else
        {
            fineDeps0 = fineDeps;
        }
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
