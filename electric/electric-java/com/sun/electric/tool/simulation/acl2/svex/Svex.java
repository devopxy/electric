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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;

/**
 * Symbolic Vector EXpression.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX>.
 * It maybe either a constant, a variable, or a function applied to subexpressions.
 *
 * @param <N> Type of name of Svex variables
 */
public abstract class Svex<N extends SvarName>
{
    public static <N extends SvarName> Svex<N> valueOf(Svar.Builder<N> sb, ACL2Object rep,
        Map<ACL2Object, Svex<N>> cache)
    {
        Svex<N> svex = cache != null ? cache.get(rep) : null;
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
                Svex<N>[] argsArray = newSvexArray(n);
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
        Map<Svar<N>, BigInteger> varsWithMasks = collectVarsWithMasks(BigIntegerUtil.MINUS_ONE, false);
        boolean first = true;
        for (Map.Entry<Svar<N>, BigInteger> e : varsWithMasks.entrySet())
        {
            Svar<N> svar = e.getKey();
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
    public static <N extends SvarName> Svex<N>[] newSvexArray(int length)
    {
        return new Svex[length];
    }

    public abstract <N1 extends SvarName> Svex<N1> convertVars(Svar.Builder<N1> builder, Map<Svex<N>, Svex<N1>> cache);

    public abstract <N1 extends SvarName> Svex<N1> addDelay(int delay, Svar.Builder<N1> builder, Map<Svex<N>, Svex<N1>> cache);

    public Set<Svar<N>> collectVars()
    {
        Set<Svar<N>> result = new LinkedHashSet<>();
        Set<SvexCall<N>> visited = new HashSet<>();
        collectVars(result, visited);
        return result;
    }

    protected abstract void collectVars(Set<Svar<N>> result, Set<SvexCall<N>> vusited);

    public abstract <R, D> R accept(Visitor<N, R, D> visitor, D data);

    public static interface Visitor<N extends SvarName, R, P>
    {
        R visitConst(Vec4 val, P p);

        R visitVar(Svar<N> name, P p);

        R visitCall(SvexFunction fun, Svex<N>[] args, P p);
    }

    public abstract Vec4 xeval(Map<Svex<N>, Vec4> memoize);

    public static <N extends SvarName> Vec4[] listXeval(Svex<N>[] list, Map<Svex<N>, Vec4> memoize)
    {
        Vec4[] result = new Vec4[list.length];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = list[i].xeval(memoize);
        }
        return result;
    }

    void toposort(Set<Svex<N>> downTop)
    {
        downTop.add(this);
    }

    public Svex<N>[] toposort()
    {
        Set<Svex<N>> downTop = new LinkedHashSet<>();
        toposort(downTop);
        Svex<N>[] topDown = newSvexArray(downTop.size());
        int i = topDown.length;
        for (Svex<N> svex : downTop)
        {
            topDown[--i] = svex;
        }
        assert i == 0;
        return topDown;
    }

    public static <N extends SvarName> Svex<N>[] listToposort(Collection<Svex<N>> list)
    {
        Set<Svex<N>> downTop = new LinkedHashSet<>();
        for (Svex<N> svex : list)
        {
            if (svex == null)
            {
                throw new NullPointerException();
            }
            svex.toposort(downTop);
        }
        Svex<N>[] topDown = newSvexArray(downTop.size());
        int i = topDown.length;
        for (Svex<N> svex : downTop)
        {
            topDown[--i] = svex;
        }
        assert i == 0;
        return topDown;
    }

    private static <N extends SvarName> void svexArgsApplyMasks(Svex<N>[] args, BigInteger[] masks, Map<Svex<N>, BigInteger> maskMap)
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

    private static <N extends SvarName> void listComputeMasks(Svex<N>[] x, Map<Svex<N>, BigInteger> maskMap)
    {
        for (Svex<N> svex : x)
        {
            if (svex instanceof SvexCall)
            {
                SvexCall<N> sc = (SvexCall<N>)svex;
                BigInteger mask = maskMap.get(sc);
                if (mask != null && mask.signum() != 0)
                {
                    BigInteger[] argmasks = sc.fun.argmasks(mask, sc.args);
                    svexArgsApplyMasks(sc.args, argmasks, maskMap);
                }
            }
        }
    }

    public Map<Svex<N>, BigInteger> maskAlist(BigInteger mask)
    {
        Svex<N>[] toposort = toposort();
        Map<Svex<N>, BigInteger> maskMap = new HashMap<>();
        maskMap.put(this, mask);
        listComputeMasks(toposort, maskMap);
        return maskMap;
    }

    public Map<Svar<N>, BigInteger> collectVarsWithMasks(BigInteger mask, boolean omitNulls)
    {
        Set<Svar<N>> vars = collectVars();
        Map<Svex<N>, BigInteger> maskAl = maskAlist(mask);
        Map<Svar<N>, BigInteger> result = new LinkedHashMap<>();
        for (Svar<N> var : vars)
        {
            SvexVar<N> svv = new SvexVar<>(var);
            BigInteger varMask = maskAl.get(svv);
            if (!omitNulls || varMask != null)
            {
                result.put(var, varMask);
            }
        }
        return result;
    }

    public static <N extends SvarName> Map<Svex<N>, BigInteger> listMaskAlist(Collection<Svex<N>> list)
    {
        Svex<N>[] toposort = listToposort(list);
        Map<Svex<N>, BigInteger> maskMap = new HashMap<>();
        for (Svex<N> svex : list)
        {
            maskMap.put(svex, BigIntegerUtil.MINUS_ONE);
        }
        listComputeMasks(toposort, maskMap);
        return maskMap;
    }

    public abstract Svex<N> patch(Map<Svar<N>, Vec4> subst, Map<SvexCall<N>, SvexCall<N>> memoize);

    /* rsh-concat.lisp */
    public abstract boolean isLhsUnbounded();

    public abstract boolean isLhs();

    public static class MatchConcat<N extends SvarName>
    {
        final int width;
        final Svex<N> lsbs;
        final Svex<N> msbs;

        MatchConcat(int width, Svex<N> lsbs, Svex<N> msbs)
        {
            this.width = width;
            this.lsbs = lsbs;
            this.msbs = msbs;
        }
    }

    public MatchConcat<N> matchConcat()
    {
        return null;
    }

    public static class MatchExt<N extends SvarName>
    {
        final int width;
        final Svex<N> lsbs;
        final boolean signExtend;

        MatchExt(int width, Svex<N> lsbs, boolean signExtend)
        {
            this.width = width;
            this.lsbs = lsbs;
            this.signExtend = signExtend;
        }
    }

    public MatchExt<N> matchExt()
    {
        return null;
    }

    public static class MatchRsh<N extends SvarName>
    {
        final int width;
        final Svex<N> subexp;

        MatchRsh(int width, Svex<N> subexp)
        {
            this.width = width;
            this.subexp = subexp;
        }
    }

    public MatchRsh<N> matchRsh()
    {
        return null;
    }

    public Svex<N> rsh(int sh)
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
            return new SvexQuote<>(Vec4Rsh.FUNCTION.apply(new Vec2(sh),
                ((SvexQuote<N>)this).val));
        }
        MatchRsh<N> matchRsh = matchRsh();
        if (matchRsh != null)
        {
            Svex<N> shift = new SvexQuote<>(new Vec2(matchRsh.width + sh));
            Svex<N>[] newArgs = Svex.newSvexArray(2);
            newArgs[0] = shift;
            newArgs[1] = matchRsh.subexp;
            return SvexCall.newCall(Vec4Rsh.FUNCTION, newArgs);
        }
        MatchConcat<N> matchConcat = matchConcat();
        if (matchConcat != null && sh >= matchConcat.width)
        {
            Svex<N> shift = new SvexQuote<>(new Vec2(sh - matchRsh.width));
            Svex<N>[] newArgs = Svex.newSvexArray(2);
            newArgs[0] = shift;
            newArgs[1] = matchRsh.subexp;
            return SvexCall.newCall(Vec4Rsh.FUNCTION, newArgs);
        }
        MatchExt<N> matchExt = matchExt();
        if (matchExt != null && sh >= matchExt.width && !matchExt.signExtend)
        {
            return new SvexQuote<>(Vec2.ZERO);
        }
        Svex<N> shift = new SvexQuote<>(new Vec2(sh));
        Svex<N>[] newArgs = Svex.newSvexArray(2);
        newArgs[0] = shift;
        newArgs[1] = this;
        return SvexCall.newCall(Vec4Rsh.FUNCTION, newArgs);
    }

    public Svex<N> concat(int w, Svex<N> y)
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
            Vec4 val = Vec4Rsh.FUNCTION.apply(new Vec2(w),
                ((SvexQuote<N>)this).val,
                ((SvexQuote<N>)y).val);
            return new SvexQuote<>(val);
        }
        MatchConcat<N> matchConcat = matchConcat();
        if (matchConcat != null && w <= matchConcat.width)
        {
            return matchConcat.lsbs.concat(w, y);
        }
        MatchExt<N> matchExt = matchExt();
        if (matchExt != null && w <= matchExt.width)
        {
            return matchExt.lsbs.concat(w, y);
        }
        if (!(this instanceof SvexQuote))
        {
            Svex<N> width = new SvexQuote<>(new Vec2(w));
            Svex<N>[] newArgs = Svex.newSvexArray(3);
            newArgs[0] = width;
            newArgs[1] = this;
            newArgs[2] = y;
            return SvexCall.newCall(Vec4Concat.FUNCTION, newArgs);
        }
        MatchConcat<N> matchConcatY = y.matchConcat();
        if (matchConcatY != null && matchConcatY.lsbs instanceof SvexQuote)
        {
            Vec4 lsbVal = Vec4Concat.FUNCTION.apply(new Vec2(w),
                ((SvexQuote<N>)this).val,
                ((SvexQuote<N>)matchConcatY.lsbs).val);
            Svex<N> newLsb = new SvexQuote<>(lsbVal);
            return newLsb.concat(w + matchConcatY.width, matchConcatY.msbs);
        }
        Svex<N> width = new SvexQuote<>(new Vec2(w));
        Svex<N>[] newArgs = Svex.newSvexArray(3);
        newArgs[0] = width;
        newArgs[1] = this;
        newArgs[2] = y;
        return SvexCall.newCall(Vec4Concat.FUNCTION, newArgs);
    }

    public Svex<N> zerox(int w)
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
            Vec4 val = Vec4ZeroExt.FUNCTION.apply(new Vec2(w),
                ((SvexQuote<N>)this).val);
            return new SvexQuote<>(val);
        }
        MatchConcat<N> matchConcat = matchConcat();
        if (matchConcat != null && w <= matchConcat.width)
        {
            return matchConcat.lsbs.zerox(w);
        }
        MatchExt<N> matchExt = matchExt();
        if (matchExt != null && w <= matchExt.width)
        {
            return matchExt.lsbs.zerox(w);
        }
        Svex<N> width = new SvexQuote<>(new Vec2(w));
        Svex<N>[] newArgs = Svex.newSvexArray(2);
        newArgs[0] = width;
        newArgs[1] = this;
        return SvexCall.newCall(Vec4ZeroExt.FUNCTION, newArgs);
    }

    public Svex<N> signx(int w)
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
            Vec4 val = Vec4SignExt.FUNCTION.apply(new Vec2(w),
                ((SvexQuote<N>)this).val);
            return new SvexQuote<>(val);
        }
        MatchConcat<N> matchConcat = matchConcat();
        if (matchConcat != null && w <= matchConcat.width)
        {
            return matchConcat.lsbs.signx(w);
        }
        MatchExt<N> matchExt = matchExt();
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
        Svex<N> width = new SvexQuote<>(new Vec2(w));
        Svex<N>[] newArgs = Svex.newSvexArray(2);
        newArgs[0] = width;
        newArgs[1] = this;
        return SvexCall.newCall(Vec4ZeroExt.FUNCTION, newArgs);
    }

    /* rewrite.lisp */
    void multirefs(Set<SvexCall<N>> seen, Set<SvexCall<N>> multirefs)
    {
    }

    public Set<SvexCall<N>> multirefs()
    {
        return multirefs(Collections.singleton(this));
    }

    public static <N extends SvarName> Set<SvexCall<N>> multirefs(Collection<Svex<N>> list)
    {
        Set<SvexCall<N>> seen = new HashSet<>();
        Set<SvexCall<N>> multirefs = new LinkedHashSet<>();
        for (Svex<N> svex : list)
        {
            svex.multirefs(seen, multirefs);
        }
        List<SvexCall<N>> multirefsList = new ArrayList<>(multirefs);
        multirefs.clear();
        for (int i = multirefsList.size() - 1; i >= 0; i--)
        {
            multirefs.add(multirefsList.get(i));
        }
        return multirefs;
    }
}
