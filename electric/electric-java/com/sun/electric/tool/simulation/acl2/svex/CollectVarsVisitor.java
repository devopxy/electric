/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CollectVarsVisitor.java
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

import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Bitand;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4IteStmt;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collect variables from SVEX expression.
 */
class CollectVarsVisitor<T extends Svar> implements Svex.Visitor<Vec2, Set<T>>
{
    private final Class<T> cls;
    private final Map<Svex, Vec2> visited = new HashMap<>();
    private final Map<Svar, Vec2> env;

    CollectVarsVisitor(Class<T> cls, Map<Svar, Vec2> env)
    {
        this.cls = cls;
        this.env = env;
    }

    public static Set<Svar> collectVars(Svex svex)
    {
        return collectVars(svex, Svar.class, null);
    }

    public static <T extends Svar> Set<T> collectVars(Svex svex, Class<T> cls, Map<Svar, Vec2> env)
    {
        Set<T> result = new LinkedHashSet<>();
        svex.accept(new CollectVarsVisitor<>(cls, env), result);
        return result;
    }

    @Override
    public Vec2 visitConst(Vec4 val, Set<T> p)
    {
        return val.isVec2() ? (Vec2)val : null;
    }

    @Override
    public Vec2 visitVar(Svar name, Set<T> p)
    {
        if (env != null)
        {
            Vec2 val = env.get(name);
            if (val != null)
            {
                return val;
            }
        }
        if (cls.isInstance(name))
        {
            p.add(cls.cast(name));
        }
        return null;
    }

    private Vec2 visitArg(Svex arg, Set<T> p)
    {
        if (visited.containsKey(arg))
        {
            return visited.get(arg);
        }
        Vec2 result = arg.accept(this, p);
        visited.put(arg, result);
        return result;
    }

    @Override
    public Vec2 visitCall(SvexFunction fun, Svex[] args, Set<T> p)
    {
        if (fun == Vec4IteStmt.FUNCTION)
        {
            Svex test = args[0];
            Svex th = args[1];
            Svex el = args[2];
            Vec2 testVal = visitArg(test, p);
            if (Vec2.ZERO.equals(testVal))
            {
                return visitArg(el, p);
            } else if (testVal != null)
            {
                return visitArg(th, p);
            }
        } else if (fun == Vec4Bitand.FUNCTION)
        {
            Svex x = args[0];
            Svex y = args[1];
            Vec2 xVal = visitArg(x, p);
            if (Vec2.ZERO.equals(xVal))
            {
                return Vec2.ZERO;
            } else if (xVal != null)
            {
                Vec2 yVal = visitArg(y, p);
                if (yVal != null)
                {
                    Vec4 result = Vec4Bitand.FUNCTION.apply(xVal, yVal);
                    return result.isVec2() ? (Vec2)result : null;
                }
            }
        }
        Vec4[] argVals = new Vec4[args.length];
        boolean allVec2 = true;
        for (int i = 0; i < args.length; i++)
        {
            Svex a = args[i];
            Vec4 aVal = visitArg(a, p);
            allVec2 = allVec2 && aVal != null && aVal.isVec2();
            argVals[i] = aVal;
        }
        if (allVec2)
        {
            Vec4 result = fun.apply(argVals);
            if (result.isVec2())
            {
                return (Vec2)result;
            }
        }
        return null;
    }

}
