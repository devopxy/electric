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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import java.util.Set;

/**
 * Symbolic Vector EXpression.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX>.
 * It maybe either a constant, a variable, or a function applied to subexpressions.
 */
public abstract class Svex
{
    public static Svex valueOf(Svar.Builder sb, ACL2Object rep, Map<ACL2Object, Svex> cache)
    {
        Svex svex = cache != null ? cache.get(rep) : null;
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
                svex = new SvexVar(sb.fromACL2(rep));
            } else if (fn.equals(QUOTE))
            {
                if (NIL.equals(cdr(args)) && consp(car(args)).bool())
                {
                    svex = new SvexQuote(Vec4.valueOf(car(args)));
                }
            } else
            {
                int n = 0;
                for (ACL2Object as = args; consp(as).bool(); as = cdr(as))
                {
                    n++;
                }
                Svex[] argsArray = new Svex[n];
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
                    svex = SvexCall.newCall(fn, argsArray);
                }
            }
        } else if (stringp(rep).bool() || symbolp(rep).bool())
        {
            svex = new SvexVar(sb.fromACL2(rep));
        } else if (integerp(rep).bool())
        {
            svex = new SvexQuote(new Vec2(rep));
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
        Map<Svar, BigInteger> varsWithMasks = collectVarsWithMasks(BigIntegerUtil.MINUS_ONE, Svar.class);
        boolean first = true;
        for (Map.Entry<Svar, BigInteger> e: varsWithMasks.entrySet())
        {
            Svar svar = e.getKey();
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

    public <T extends Svar> Set<T> collectVars(Class<T> cls, Map<Svar, Vec2> env)
    {
        return CollectVarsVisitor.collectVars(this, cls, env);
    }

    public <T extends Svar> Set<T> collectVars(Class<T> cls)
    {
        return CollectVarsVisitor.collectVars(this, cls, null);
    }

    public Set<Svar> collectVars()
    {
        return CollectVarsVisitor.collectVars(this);
    }

    public abstract <R, D> R accept(Visitor<R, D> visitor, D data);

    public static interface Visitor<R, P>
    {
        R visitConst(Vec4 val, P p);

        R visitVar(Svar name, P p);

        R visitCall(SvexFunction fun, Svex[] args, P p);
    }

    public abstract Vec4 xeval(Map<Svex, Vec4> memoize);

    public static Vec4[] listXeval(Svex[] list, Map<Svex, Vec4> memoize)
    {
        Vec4[] result = new Vec4[list.length];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = list[i].xeval(memoize);
        }
        return result;
    }

    void toposort(Set<Svex> downTop)
    {
        downTop.add(this);
    }

    public Svex[] toposort()
    {
        Set<Svex> downTop = new LinkedHashSet<>();
        toposort(downTop);
        Svex[] topDown = new Svex[downTop.size()];
        int i = topDown.length;
        for (Svex svex : downTop)
        {
            topDown[--i] = svex;
        }
        assert i == 0;
        return topDown;
    }

    public static Svex[] listToposort(Collection<Svex> list)
    {
        Set<Svex> downTop = new LinkedHashSet<>();
        for (Svex svex : list)
        {
            if (svex == null)
            {
                throw new NullPointerException();
            }
            svex.toposort(downTop);
        }
        Svex[] topDown = new Svex[downTop.size()];
        int i = topDown.length;
        for (Svex svex : downTop)
        {
            topDown[--i] = svex;
        }
        assert i == 0;
        return topDown;
    }

    private static void svexArgsApplyMasks(Svex[] args, BigInteger[] masks, Map<Svex, BigInteger> maskMap)
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

    private static void listComputeMasks(Svex[] x, Map<Svex, BigInteger> maskMap)
    {
        for (Svex svex : x)
        {
            if (svex instanceof SvexCall)
            {
                SvexCall sc = (SvexCall)svex;
                BigInteger mask = maskMap.get(sc);
                if (mask != null && mask.signum() != 0)
                {
                    BigInteger[] argmasks = sc.fun.argmasks(mask, sc.args);
                    svexArgsApplyMasks(sc.args, argmasks, maskMap);
                }
            }
        }
    }

    public Map<Svex, BigInteger> maskAlist(BigInteger mask)
    {
        Svex[] toposort = toposort();
        Map<Svex, BigInteger> maskMap = new HashMap<>();
        maskMap.put(this, mask);
        listComputeMasks(toposort, maskMap);
        return maskMap;
    }

    public <T extends Svar> Map<T, BigInteger> collectVarsWithMasks(BigInteger mask, Class<T> cls)
    {
        Set<T> vars = collectVars(cls);
        Map<Svex, BigInteger> maskAl = maskAlist(mask);
        Map<T, BigInteger> result = new LinkedHashMap<>();
        for (T var : vars)
        {
            SvexVar svv = new SvexVar(var);
            BigInteger varMask = maskAl.get(svv);
            result.put(var, varMask);
        }
        return result;
    }

    public static Map<Svex, BigInteger> listMaskAlist(Collection<Svex> list)
    {
        Svex[] toposort = listToposort(list);
        Map<Svex, BigInteger> maskMap = new HashMap<>();
        for (Svex svex : list)
        {
            maskMap.put(svex, BigIntegerUtil.MINUS_ONE);
        }
        listComputeMasks(toposort, maskMap);
        return maskMap;
    }
}