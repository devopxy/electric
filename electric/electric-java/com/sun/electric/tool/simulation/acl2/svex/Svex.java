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
        sb.append("svex");
        for (Svar sv : collectVars())
        {
            sb.append(" ").append(sv);
        }
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
}
