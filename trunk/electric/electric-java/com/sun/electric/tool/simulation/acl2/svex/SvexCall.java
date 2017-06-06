/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvexCall.java
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
package com.sun.electric.tool.simulation.acl2.svex;

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * A function applied to some expressions.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-CALL>.
 *
 * @param <V> Type of Svex variables
 */
public class SvexCall<V extends Svar> extends Svex<V>
{
    public final SvexFunction fun;
    protected final Svex<V>[] args;
    private final int hashCode;

    @SafeVarargs
    static <V extends Svar> SvexCall<V> newCall(ACL2Object fn, Svex<V>... args)
    {
        SvexFunction fun = SvexFunction.valueOf(fn, args.length);
        return fun.build(args);
    }

    @SafeVarargs
    protected SvexCall(SvexFunction fun, Svex<V>... args)
    {
        assert fun.arity == args.length;
        this.fun = fun;
        this.args = args.clone();
        for (Svex<V> arg : this.args)
        {
            if (arg == null)
            {
                throw new NullPointerException();
            }
        }
        int hash = 3;
        hash = 83 * hash + fun.hashCode();
        for (Svex<V> arg : args)
        {
            hash = 31 * hash + arg.hashCode();
        }
        hashCode = hash;
    }

    public Svex<V>[] getArgs()
    {
        return args.clone();
    }

    @Override
    public ACL2Object makeACL2Object()
    {
        ACL2Object a = NIL;
        for (int i = args.length - 1; i >= 0; i++)
        {
            a = cons(args[i].makeACL2Object(), a);
        }
        return cons(fun.fn, a);
    }

    @Override
    public <V1 extends Svar> Svex<V1> convertVars(Svar.Builder<V1> builder, Map<Svex<V>, Svex<V1>> cache)
    {
        Svex<V1> svex = cache.get(this);
        if (svex == null)
        {
            Svex<V1>[] newArgs = Svex.newSvexArray(fun.arity);
            for (int i = 0; i < fun.arity; i++)
            {
                newArgs[i] = args[i].convertVars(builder, cache);
            }
            svex = fun.build(newArgs);
            cache.put(this, svex);
        }
        return svex;
    }

    @Override
    protected void collectVars(Set<V> result, Set<SvexCall<V>> visited)
    {
        if (visited.add(this))
        {
            for (Svex<V> arg : args)
            {
                arg.collectVars(result, visited);
            }
        }
    }

    @Override
    public <R, D> R accept(Visitor<V, R, D> visitor, D data)
    {
        return visitor.visitCall(fun, args, data);
    }

    @Override
    public Vec4 xeval(Map<Svex<V>, Vec4> memoize)
    {
        Vec4 result = memoize.get(this);
        if (result == null)
        {
            result = fun.apply(Svex.listXeval(args, memoize));
            memoize.put(this, result);
        }
        return result;
    }

    @Override
    void toposort(Set<Svex<V>> downTop)
    {
        if (!downTop.contains(this))
        {
            for (Svex<V> arg : args)
            {
                arg.toposort(downTop);
            }
            downTop.add(this);
        }
    }

    @Override
    public Svex<V> patch(Map<V, Vec4> subst, Map<SvexCall<V>, SvexCall<V>> memoize)
    {
        SvexCall<V> svex = memoize.get(this);
        if (svex == null)
        {
            Svex<V>[] newArgs = Svex.newSvexArray(args.length);
            boolean changed = false;
            for (int i = 0; i < args.length; i++)
            {
                newArgs[i] = args[i].patch(subst, memoize);
                changed = changed || newArgs[i] != args[i];
            }
            svex = changed ? new SvexCall<>(fun, newArgs) : this;
            memoize.put(this, svex);
        }
        return svex;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof SvexCall)
        {
            SvexCall that = (SvexCall)o;
            if (!this.fun.equals(that.fun))
            {
                return false;
            }
            return Arrays.equals(this.args, that.args);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
