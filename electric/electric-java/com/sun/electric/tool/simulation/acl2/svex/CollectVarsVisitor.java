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

import com.sun.electric.util.acl2.ACL2Object;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Collect variables from SVEX expression.
 */
class CollectVarsVisitor<T extends Svar> implements Svex.Visitor<Set<T>, Set<T>>
{
    private static final CollectVarsVisitor<Svar> SVAR_VISITOR = new CollectVarsVisitor<>(Svar.class);
    private final Class<T> cls;

    CollectVarsVisitor(Class<T> cls)
    {
        this.cls = cls;
    }

    public static <T extends Svar> Set<T> collectVars(Svex svex, Class<T> cls)
    {
        return svex.accept(new CollectVarsVisitor<>(cls), new LinkedHashSet<>());
    }

    public static Set<Svar> collectVars(Svex svex)
    {
        return svex.accept(SVAR_VISITOR, new LinkedHashSet<>());
    }

    @Override
    public Set<T> visitConst(Vec4 val, Set<T> p)
    {
        return p;
    }

    @Override
    public Set<T> visitVar(Svar name, Set<T> p)
    {
        if (cls.isInstance(name))
        {
            p.add(cls.cast(name));
        }
        return p;
    }

    @Override
    public Set<T> visitCall(SvexFunction fun, Svex[] args, Set<T> p)
    {
        for (Svex a : args)
        {
            a.accept(this, p);
        }
        return p;
    }

}
