/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Lhs.java
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

import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

import java.util.LinkedList;
import java.util.List;

/**
 * A shorthand format for an expression consisting of a concatenation of parts of variables.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHS>.
 */
public class Lhs
{

    final ACL2Object impl;

    public final List<Lhrange> ranges = new LinkedList<>();

    Lhs(Module parent, ACL2Object impl)
    {
        this.impl = impl;
        List<ACL2Object> l = Util.getList(impl, true);
        Util.check(!l.isEmpty());
        for (ACL2Object o : l)
        {
            ranges.add(new Lhrange(parent, o));
        }
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof Lhs && impl == ((Lhs)o).impl;
    }

    @Override
    public int hashCode()
    {
        return (int)impl.id();
    }

    public int size()
    {
        int size = 0;
        for (Lhrange lr : ranges)
        {
            size += lr.w;
        }
        return size;
    }

    @Override
    public String toString()
    {
        return impl.rep();
    }

    public String toElectricString()
    {
        String s = "";
        for (int i = ranges.size() - 1; i >= 0; i--)
        {
            s += ranges.get(i).toLispString();
            if (i > 0)
            {
                s += ",";
            }
        }
        return s;
    }

    public void markAssigned(BigInteger assignedBits)
    {
        for (Lhrange lr : ranges)
        {
            lr.markAssigned(assignedBits);
            assignedBits = assignedBits.shiftRight(lr.w);
        }
    }

    public void markUsed()
    {
        for (Lhrange lr : ranges)
        {
            lr.markUsed();
        }
    }
}
