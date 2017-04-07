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

/**
 * A function applied to some expressions.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-CALL>.
 */
public class SvexCall extends Svex
{
    public final SvexFunction fun;
    private final Svex[] args;

    static SvexCall newCall(ACL2Object fn, Svex... args)
    {
        SvexFunction fun = SvexFunction.valueOf(fn, args.length);
        return fun.build(args);
    }

    protected SvexCall(SvexFunction fun, Svex... args)
    {
        assert fun.arity == args.length;
        this.fun = fun;
        this.args = args.clone();
        for (Svex arg : this.args)
        {
            if (arg == null)
            {
                throw new NullPointerException();
            }
        }
    }

    public Svex[] getArgs()
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
    public <R, D> R accept(Visitor<R, D> visitor, D data)
    {
        return visitor.visitCall(fun, args, data);
    }

}
