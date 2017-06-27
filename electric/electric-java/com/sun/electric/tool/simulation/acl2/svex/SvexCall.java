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

import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Concat;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Rsh;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4SignExt;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4ZeroExt;
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
    private final ACL2Object impl;

    @SafeVarargs
    public static <V extends Svar> SvexCall<V> newCall(SvexFunction fun, Svex<V>... args)
    {
        return new SvexCall<>(fun, args);
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
        impl = makeACL2Object(fun, args);
    }

    private static <V extends Svar> ACL2Object makeACL2Object(SvexFunction fun, Svex<V>... args)
    {
        ACL2Object impl = NIL;
        for (int i = args.length - 1; i >= 0; i--)
        {
            impl = hons(args[i].getACL2Object(), impl);
        }
        return hons(fun.fn, impl);
    }

    public Svex<V>[] getArgs()
    {
        return args.clone();
    }

    @Override
    public ACL2Object getACL2Object()
    {
        return impl;
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
    public <V1 extends Svar> Svex<V1> addDelay(int delay, Svar.Builder<V1> builder, Map<Svex<V>, Svex<V1>> cache)
    {
        Svex<V1> svex = cache.get(this);
        if (svex == null)
        {
            Svex<V1>[] newArgs = Svex.newSvexArray(fun.arity);
            for (int i = 0; i < fun.arity; i++)
            {
                newArgs[i] = args[i].addDelay(delay, builder, cache);
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
            svex = changed ? SvexCall.newCall(fun, newArgs) : this;
            memoize.put(this, svex);
        }
        return svex;
    }

    @Override
    public boolean isLhsUnbounded()
    {
        if (fun == Vec4Concat.FUNCTION)
        {
            Vec4Concat<V> svex = (Vec4Concat<V>)this;
            Svex<V> w = svex.width;
            Svex<V> lo = svex.low;
            Svex<V> hi = svex.high;
            return w instanceof SvexQuote && ((SvexQuote<V>)w).val.isIndex() && lo.isLhsUnbounded() && hi.isLhsUnbounded();
        } else if (fun == Vec4Rsh.FUNCTION)
        {
            Vec4Rsh<V> svex = (Vec4Rsh<V>)this;
            Svex<V> sh = svex.shift;
            Svex<V> x = svex.x;
            return sh instanceof SvexQuote && ((SvexQuote<V>)sh).val.isIndex() && x.isLhsUnbounded();
        }
        return false;
    }

    @Override
    public boolean isLhs()
    {
        if (fun == Vec4Concat.FUNCTION)
        {
            Vec4Concat<V> svex = (Vec4Concat<V>)this;
            Svex<V> w = svex.width;
            Svex<V> lo = svex.low;
            Svex<V> hi = svex.high;
            return w instanceof SvexQuote && ((SvexQuote<V>)w).val.isIndex() && lo.isLhsUnbounded() && hi.isLhs();
        } else if (fun == Vec4Rsh.FUNCTION)
        {
            Vec4Rsh<V> svex = (Vec4Rsh<V>)this;
            Svex<V> sh = svex.shift;
            Svex<V> x = svex.x;
            return sh instanceof SvexQuote && ((SvexQuote<V>)sh).val.isIndex() && x.isLhs();
        }
        return false;
    }

    @Override
    public MatchConcat<V> matchConcat()
    {
        if (fun == Vec4Concat.FUNCTION)
        {
            Vec4Concat<V> svex = (Vec4Concat<V>)this;
            Svex<V> width = svex.width;
            if (width instanceof SvexQuote)
            {
                Vec4 wval = ((SvexQuote)width).val;
                if (wval.isVec2() && ((Vec2)wval).getVal().signum() >= 0)
                {
                    return new MatchConcat<>(((Vec2)wval).getVal().intValueExact(), svex.low, svex.high);
                }
            }
        }
        return null;
    }

    @Override
    public MatchExt<V> matchExt()
    {
        if (fun == Vec4ZeroExt.FUNCTION || fun == Vec4SignExt.FUNCTION)
        {
            Svex<V> width = args[0];
            if (width instanceof SvexQuote)
            {
                Vec4 wval = ((SvexQuote)width).val;
                if (wval.isVec2() && ((Vec2)wval).getVal().signum() >= 0)
                {
                    return new MatchExt<>(((Vec2)wval).getVal().intValueExact(), args[1],
                        fun == Vec4SignExt.FUNCTION);
                }
            }
        }
        return null;
    }

    @Override
    public MatchRsh<V> matchRsh()
    {
        if (fun == Vec4Rsh.FUNCTION)
        {
            Svex<V> shift = args[0];
            if (shift instanceof SvexQuote)
            {
                Vec4 sval = ((SvexQuote)shift).val;
                if (sval.isVec2() && ((Vec2)sval).getVal().signum() >= 0)
                {
                    return new MatchRsh<>(((Vec2)sval).getVal().intValueExact(), args[1]);
                }
            }
        }
        return null;
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
        return impl.hashCode();
    }
}
