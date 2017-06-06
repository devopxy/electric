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
                    svex = SvexCall.<V>newCall(fn, argsArray);
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

    public abstract ACL2Object makeACL2Object();

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
}
