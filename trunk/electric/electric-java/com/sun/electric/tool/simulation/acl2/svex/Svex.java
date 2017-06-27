/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SVar.java
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
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import java.util.Set;

/**
 * Symbolic Vector EXpression.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX>.
 * It maybe either a constant, a variable, or a function applied to subexpressions.
 *
 * @param <V> Type of Svex variables
 */
public abstract class Svex<V extends Svar>
{
    public static <V extends Svar> Svex<V> valueOf(Svar.Builder<V> sb, ACL2Object rep, Map<ACL2Object, Svex<V>> cache)
    {
        Svex<V> svex = cache != null ? cache.get(rep) : null;
        if (svex != null)
        {
            return svex;
        }
        if (consp(rep).bool())
        {
            ACL2Object fn = car(rep);
            ACL2Object args = cdr(rep);
            if (Svar.KEYWORD_VAR.equals(fn))
            {
                svex = new SvexVar<>(sb.fromACL2(rep));
            } else if (fn.equals(QUOTE))
            {
                if (NIL.equals(cdr(args)) && consp(car(args)).bool())
                {
                    svex = new SvexQuote<>(Vec4.valueOf(car(args)));
                }
            } else
            {
                int n = 0;
                for (ACL2Object as = args; consp(as).bool(); as = cdr(as))
                {
                    n++;
                }
                Svex<V>[] argsArray = newSvexArray(n);
                for (n = 0; n < argsArray.length; n++)
                {
                    if (!consp(args).bool())
                    {
                        throw new IllegalArgumentException();
                    }
                    argsArray[n] = valueOf(sb, car(args), cache);
                    args = cdr(args);
                }
                if (NIL.equals(args))
                {
                    SvexFunction fun = SvexFunction.valueOf(fn, argsArray.length);
                    svex = SvexCall.newCall(fun, argsArray);
                }
            }
        } else if (stringp(rep).bool() || symbolp(rep).bool())
        {
            svex = new SvexVar<>(sb.fromACL2(rep));
        } else if (integerp(rep).bool())
        {
            svex = new SvexQuote<>(new Vec2(rep));
        }
        if (svex != null)
        {
            if (cache != null)
            {
                cache.put(rep, svex);
            }
            return svex;
        }
        throw new IllegalArgumentException();
    }

    public abstract ACL2Object getACL2Object();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("svex(");
        Map<V, BigInteger> varsWithMasks = collectVarsWithMasks(BigIntegerUtil.MINUS_ONE);
        boolean first = true;
        for (Map.Entry<V, BigInteger> e : varsWithMasks.entrySet())
        {
            V svar = e.getKey();
            BigInteger mask = e.getValue();
            if (first)
                first = false;
            else
                sb.append(",");
            sb.append(svar.toString(mask));
        }
        sb.append(")");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static <V extends Svar> Svex<V>[] newSvexArray(int length)
    {
        return new Svex[length];
    }

    public abstract <V1 extends Svar> Svex<V1> convertVars(Svar.Builder<V1> builder, Map<Svex<V>, Svex<V1>> cache);

    public abstract <V1 extends Svar> Svex<V1> addDelay(int delay, Svar.Builder<V1> builder, Map<Svex<V>, Svex<V1>> cache);

    public Set<V> collectVars()
    {
        Set<V> result = new LinkedHashSet<>();
        Set<SvexCall<V>> visited = new HashSet<>();
        collectVars(result, visited);
        return result;
    }

    protected abstract void collectVars(Set<V> result, Set<SvexCall<V>> vusited);

    public abstract <R, D> R accept(Visitor<V, R, D> visitor, D data);

    public static interface Visitor<V extends Svar, R, P>
    {
        R visitConst(Vec4 val, P p);

        R visitVar(V name, P p);

        R visitCall(SvexFunction fun, Svex<V>[] args, P p);
    }

    public abstract Vec4 xeval(Map<Svex<V>, Vec4> memoize);

    public static <V extends Svar> Vec4[] listXeval(Svex<V>[] list, Map<Svex<V>, Vec4> memoize)
    {
        Vec4[] result = new Vec4[list.length];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = list[i].xeval(memoize);
        }
        return result;
    }

    void toposort(Set<Svex<V>> downTop)
    {
        downTop.add(this);
    }

    public Svex<V>[] toposort()
    {
        Set<Svex<V>> downTop = new LinkedHashSet<>();
        toposort(downTop);
        Svex<V>[] topDown = newSvexArray(downTop.size());
        int i = topDown.length;
        for (Svex<V> svex : downTop)
        {
            topDown[--i] = svex;
        }
        assert i == 0;
        return topDown;
    }

    public static <V extends Svar> Svex<V>[] listToposort(Collection<Svex<V>> list)
    {
        Set<Svex<V>> downTop = new LinkedHashSet<>();
        for (Svex<V> svex : list)
        {
            if (svex == null)
            {
                throw new NullPointerException();
            }
            svex.toposort(downTop);
        }
        Svex<V>[] topDown = newSvexArray(downTop.size());
        int i = topDown.length;
        for (Svex<V> svex : downTop)
        {
            topDown[--i] = svex;
        }
        assert i == 0;
        return topDown;
    }

    private static <V extends Svar> void svexArgsApplyMasks(Svex<V>[] args, BigInteger[] masks, Map<Svex<V>, BigInteger> maskMap)
    {
        if (args.length != masks.length)
        {
            throw new IllegalArgumentException();
        }
        for (int i = args.length - 1; i >= 0; i--)
        {
            if (masks[i].signum() != 0)
            {
                BigInteger look = maskMap.get(args[i]);
                maskMap.put(args[i], look == null ? masks[i] : look.or(masks[i]));
            }
        }
    }

    private static <V extends Svar> void listComputeMasks(Svex<V>[] x, Map<Svex<V>, BigInteger> maskMap)
    {
        for (Svex<V> svex : x)
        {
            if (svex instanceof SvexCall)
            {
                SvexCall<V> sc = (SvexCall<V>)svex;
                BigInteger mask = maskMap.get(sc);
                if (mask != null && mask.signum() != 0)
                {
                    BigInteger[] argmasks = sc.fun.argmasks(mask, sc.args);
                    svexArgsApplyMasks(sc.args, argmasks, maskMap);
                }
            }
        }
    }

    public Map<Svex<V>, BigInteger> maskAlist(BigInteger mask)
    {
        Svex<V>[] toposort = toposort();
        Map<Svex<V>, BigInteger> maskMap = new HashMap<>();
        maskMap.put(this, mask);
        listComputeMasks(toposort, maskMap);
        return maskMap;
    }

    public Map<V, BigInteger> collectVarsWithMasks(BigInteger mask)
    {
        Set<V> vars = collectVars();
        Map<Svex<V>, BigInteger> maskAl = maskAlist(mask);
        Map<V, BigInteger> result = new LinkedHashMap<>();
        for (V var : vars)
        {
            SvexVar<V> svv = new SvexVar<>(var);
            BigInteger varMask = maskAl.get(svv);
            result.put(var, varMask);
        }
        return result;
    }

    public static <V extends Svar> Map<Svex<V>, BigInteger> listMaskAlist(Collection<Svex<V>> list)
    {
        Svex<V>[] toposort = listToposort(list);
        Map<Svex<V>, BigInteger> maskMap = new HashMap<>();
        for (Svex<V> svex : list)
        {
            maskMap.put(svex, BigIntegerUtil.MINUS_ONE);
        }
        listComputeMasks(toposort, maskMap);
        return maskMap;
    }

    public abstract Svex<V> patch(Map<V, Vec4> subst, Map<SvexCall<V>, SvexCall<V>> memoize);

    public abstract boolean isLhsUnbounded();

    public abstract boolean isLhs();

    public static class MatchConcat<V extends Svar>
    {
        final int width;
        final Svex<V> lsbs;
        final Svex<V> msbs;

        MatchConcat(int width, Svex<V> lsbs, Svex<V> msbs)
        {
            this.width = width;
            this.lsbs = lsbs;
            this.msbs = msbs;
        }
    }

    public MatchConcat<V> matchConcat()
    {
        return null;
    }

    public static class MatchExt<V extends Svar>
    {
        final int width;
        final Svex<V> lsbs;
        final boolean signExtend;

        MatchExt(int width, Svex<V> lsbs, boolean signExtend)
        {
            this.width = width;
            this.lsbs = lsbs;
            this.signExtend = signExtend;
        }
    }

    public MatchExt<V> matchExt()
    {
        return null;
    }

    public static class MatchRsh<V extends Svar>
    {
        final int width;
        final Svex<V> subexp;

        MatchRsh(int width, Svex<V> subexp)
        {
            this.width = width;
            this.subexp = subexp;
        }
    }

    public MatchRsh<V> matchRsh()
    {
        return null;
    }

    public Svex<V> rsh(int sh)
    {
        if (sh <= 0)
        {
            if (sh == 0)
            {
                return this;
            }
            throw new IllegalArgumentException();
        }
        if (this instanceof SvexQuote)
        {
            return new SvexQuote<V>(Vec4Rsh.FUNCTION.apply(new Vec2(BigInteger.valueOf(sh)),
                ((SvexQuote<V>)this).val));
        }
        MatchRsh<V> matchRsh = matchRsh();
        if (matchRsh != null)
        {
            Svex<V> shift = new SvexQuote<>(new Vec2(BigInteger.valueOf(matchRsh.width + sh)));
            Svex<V>[] newArgs = Svex.newSvexArray(2);
            newArgs[0] = shift;
            newArgs[1] = matchRsh.subexp;
            return SvexCall.newCall(Vec4Rsh.FUNCTION, newArgs);
        }
        MatchConcat<V> matchConcat = matchConcat();
        if (matchConcat != null && sh >= matchConcat.width)
        {
            Svex<V> shift = new SvexQuote<>(new Vec2(BigInteger.valueOf(sh - matchRsh.width)));
            Svex<V>[] newArgs = Svex.newSvexArray(2);
            newArgs[0] = shift;
            newArgs[1] = matchRsh.subexp;
            return SvexCall.newCall(Vec4Rsh.FUNCTION, newArgs);
        }
        MatchExt<V> matchExt = matchExt();
        if (matchExt != null && sh >= matchExt.width && !matchExt.signExtend)
        {
            return new SvexQuote<>(Vec2.ZERO);
        }
        Svex<V> shift = new SvexQuote<>(new Vec2(BigInteger.valueOf(sh)));
        Svex<V>[] newArgs = Svex.newSvexArray(2);
        newArgs[0] = shift;
        newArgs[1] = this;
        return SvexCall.newCall(Vec4Rsh.FUNCTION, newArgs);
    }

    public Svex<V> concat(int w, Svex<V> y)
    {
        if (w <= 0)
        {
            if (w == 0)
            {
                return y;
            }
            throw new IllegalArgumentException();
        }
        if (this instanceof SvexQuote && y instanceof SvexQuote)
        {
            Vec4 val = Vec4Rsh.FUNCTION.apply(new Vec2(BigInteger.valueOf(w)),
                ((SvexQuote<V>)this).val,
                ((SvexQuote<V>)y).val);
            return new SvexQuote<>(val);
        }
        MatchConcat<V> matchConcat = matchConcat();
        if (matchConcat != null && w <= matchConcat.width)
        {
            return matchConcat.lsbs.concat(w, y);
        }
        MatchExt<V> matchExt = matchExt();
        if (matchExt != null && w <= matchExt.width)
        {
            return matchExt.lsbs.concat(w, y);
        }
        if (!(this instanceof SvexQuote))
        {
            Svex<V> width = new SvexQuote<>(new Vec2(BigInteger.valueOf(w)));
            Svex<V>[] newArgs = Svex.newSvexArray(3);
            newArgs[0] = width;
            newArgs[1] = this;
            newArgs[2] = y;
            return SvexCall.newCall(Vec4Concat.FUNCTION, newArgs);
        }
        MatchConcat<V> matchConcatY = y.matchConcat();
        if (matchConcatY != null && matchConcatY.lsbs instanceof SvexQuote)
        {
            Vec4 lsbVal = Vec4Concat.FUNCTION.apply(new Vec2(BigInteger.valueOf(w)),
                ((SvexQuote<V>)this).val,
                ((SvexQuote<V>)matchConcatY.lsbs).val);
            Svex<V> newLsb = new SvexQuote<>(lsbVal);
            return newLsb.concat(w + matchConcatY.width, matchConcatY.msbs);
        }
        Svex<V> width = new SvexQuote<>(new Vec2(BigInteger.valueOf(w)));
        Svex<V>[] newArgs = Svex.newSvexArray(3);
        newArgs[0] = width;
        newArgs[1] = this;
        newArgs[2] = y;
        return SvexCall.newCall(Vec4Concat.FUNCTION, newArgs);
    }

    public Svex<V> zerox(int w)
    {
        if (w <= 0)
        {
            if (w == 0)
            {
                return new SvexQuote<>(Vec2.ZERO);
            }
            throw new IllegalArgumentException();
        }
        if (this instanceof SvexQuote)
        {
            Vec4 val = Vec4ZeroExt.FUNCTION.apply(new Vec2(BigInteger.valueOf(w)),
                ((SvexQuote<V>)this).val);
            return new SvexQuote<>(val);
        }
        MatchConcat<V> matchConcat = matchConcat();
        if (matchConcat != null && w <= matchConcat.width)
        {
            return matchConcat.lsbs.zerox(w);
        }
        MatchExt<V> matchExt = matchExt();
        if (matchExt != null && w <= matchExt.width)
        {
            return matchExt.lsbs.zerox(w);
        }
        Svex<V> width = new SvexQuote<>(new Vec2(BigInteger.valueOf(w)));
        Svex<V>[] newArgs = Svex.newSvexArray(2);
        newArgs[0] = width;
        newArgs[1] = this;
        return SvexCall.newCall(Vec4ZeroExt.FUNCTION, newArgs);
    }

    public Svex<V> signx(int w)
    {
        if (w <= 0)
        {
            if (w == 0)
            {
                return new SvexQuote<>(Vec4.X);
            }
            throw new IllegalArgumentException();
        }
        if (this instanceof SvexQuote)
        {
            Vec4 val = Vec4SignExt.FUNCTION.apply(new Vec2(BigInteger.valueOf(w)),
                ((SvexQuote<V>)this).val);
            return new SvexQuote<>(val);
        }
        MatchConcat<V> matchConcat = matchConcat();
        if (matchConcat != null && w <= matchConcat.width)
        {
            return matchConcat.lsbs.signx(w);
        }
        MatchExt<V> matchExt = matchExt();
        if (matchExt != null)
        {
            if (w <= matchExt.width)
            {
                return matchExt.lsbs.signx(w);
            } else if (matchExt.signExtend)
            {
                return matchExt.lsbs.signx(matchExt.width);
            } else
            {
                return matchExt.lsbs.zerox(matchExt.width);
            }
        }
        Svex<V> width = new SvexQuote<>(new Vec2(BigInteger.valueOf(w)));
        Svex<V>[] newArgs = Svex.newSvexArray(2);
        newArgs[0] = width;
        newArgs[1] = this;
        return SvexCall.newCall(Vec4ZeroExt.FUNCTION, newArgs);
    }
}
