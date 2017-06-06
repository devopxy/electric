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

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.ArrayList;

import java.util.LinkedList;
import java.util.List;

/**
 * A shorthand format for an expression consisting of a concatenation of parts of variables.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHS>.
 *
 * @param <V> Type of Svex variables
 */
public class Lhs<V extends Svar>
{
    public final List<Lhrange<V>> ranges = new LinkedList<>();

    Lhs(Svar.Builder<V> builder, ACL2Object impl)
    {
        List<ACL2Object> l = Util.getList(impl, true);
        Util.check(!l.isEmpty());
        int lsh = 0;
        for (ACL2Object o : l)
        {
            Lhrange<V> lhr = new Lhrange<>(builder, o, lsh);
            ranges.add(lhr);
            lsh += lhr.getWidth();
        }
    }

    private Lhs(List<Lhrange<V>> ranges)
    {
        this.ranges.addAll(ranges);
    }

    public <V1 extends Svar> Lhs<V1> convertVars(Svar.Builder<V1> builder)
    {
        List<Lhrange<V1>> newRanges = new ArrayList<>();
        for (Lhrange<V> range : ranges)
        {
            newRanges.add(range.convertVars(builder));
        }
        return new Lhs<>(newRanges);
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof Lhs && ranges.equals(((Lhs)o).ranges);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 67 * hash + ranges.hashCode();
        return hash;
    }

    public int width()
    {
        int size = 0;
        for (Lhrange lr : ranges)
        {
            size += lr.getWidth();
        }
        return size;
    }

    @Override
    public String toString()
    {
        String s = "";
        for (int i = ranges.size() - 1; i >= 0; i--)
        {
            s += ranges.get(i);
            if (i > 0)
            {
                s += ",";
            }
        }
        return s;
    }
}
