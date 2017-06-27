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
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Concat;
import com.sun.electric.util.acl2.ACL2;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.ArrayList;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    public Lhs(List<Lhrange<V>> ranges)
    {
        this.ranges.addAll(ranges);
    }

    public ACL2Object getACL2Object()
    {
        ACL2Object list = NIL;
        for (int i = ranges.size() - 1; i >= 0; i--)
        {
            list = ACL2.cons(ranges.get(i).getACL2Object(), list);
        }
        return list;
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

    public Vec4 eval(Map<V, Vec4> env)
    {
        Vec4 result = Vec4.Z;
        for (int i = ranges.size() - 1; i >= 0; i--)
        {
            Lhrange<V> range = ranges.get(i);
            result = Vec4Concat.FUNCTION.apply(
                new Vec2(BigInteger.valueOf(range.getWidth())),
                range.eval(env),
                result);
        }
        return result;
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

    public Lhs<V> cons(Lhrange<V> x)
    {
        List<Lhrange<V>> newRanges = new ArrayList<>();
        if (ranges.isEmpty())
        {
            if (x.getVar() != null)
            {
                newRanges.add(x);
            }
        } else
        {
            Lhrange<V> comb = x.combine(ranges.get(0));
            if (comb != null)
            {
                if (ranges.size() > 1 || comb.getVar() != null)
                {
                    newRanges.addAll(ranges);
                    ranges.set(0, comb);
                }
            } else
            {
                newRanges.add(x);
                newRanges.addAll(ranges);
            }
        }
        return new Lhs<>(newRanges);
    }

    Lhs<V> norm()
    {
        List<Lhrange<V>> newRanges = new ArrayList<>();
        newRanges.addAll(ranges);
        for (int i = newRanges.size() - 1; i >= 0; i--)
        {
            Lhrange<V> range = newRanges.get(i);
            if (i == newRanges.size() - 1)
            {
                if (range.getVar() == null)
                {
                    newRanges.remove(i - 1);
                }
            } else
            {
                Lhrange<V> comb = range.combine(newRanges.get(i + 1));
                if (comb != null)
                {
                    newRanges.remove(i + 1);
                    newRanges.set(i, comb);
                }
            }
        }
        if (newRanges.size() == 1 && newRanges.get(0).getVar() == null)
        {
            newRanges.remove(0);
        }
        return newRanges.equals(ranges) ? this : new Lhs<>(newRanges);
    }

    public boolean isNormp()
    {
        return norm().equals(this);
    }

    public Lhs<V> concat(int w, Lhs<V> y)
    {
        List<Lhrange<V>> newRanges = new ArrayList<>();
        int ww = 0;
        for (int i = 0; i < ranges.size() && ww < w; i++)
        {
            Lhrange<V> range = ranges.get(i);
            if (ww + range.getWidth() <= w)
            {
                newRanges.add(range);
                ww += range.getWidth();
            } else
            {
                newRanges.add(new Lhrange<>(w - ww, range.getAtom()));
                ww = w;
            }
        }
        if (ww < w)
        {
            newRanges.add(new Lhrange<>(w - ww, new Lhatom.Z<>()));
        }
        newRanges.addAll(y.ranges);
        return new Lhs<>(newRanges).norm();
    }

    public Lhs<V> rsh(int sh)
    {
        List<Lhrange<V>> newRanges = new ArrayList<>();
        newRanges.addAll(ranges);
        while (sh > 0 && !newRanges.isEmpty())
        {
            Lhrange<V> range = ranges.get(0);
            if (range.getWidth() < sh)
            {
                newRanges.remove(0);
                sh -= range.getWidth();
            } else
            {
                V svar = range.getVar();
                if (svar != null)
                {
                    newRanges.add(new Lhrange<>(sh, new Lhatom.Var<>(svar, range.getRsh() + sh)));
                }
            }
        }
        return new Lhs<>(newRanges).norm();
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
