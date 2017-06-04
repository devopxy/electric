/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvexQuote.java
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
 * A "quoted constant" 4vec which represents itself.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-QUOTE>.
 */
public class SvexQuote<V extends Svar> extends Svex<V>
{

    public final Vec4 val;

    public SvexQuote(Vec4 val)
    {
        if (val == null)
        {
            throw new NullPointerException();
        }
        this.val = val;
    }

    @Override
    public ACL2Object makeACL2Object()
    {
        if (val.isVec2())
        {
            return val.makeAcl2Object();
        }
        return cons(QUOTE, cons(val.makeAcl2Object(), NIL));
    }

    @Override
    protected void collectVars(Set<V> result, Set<SvexCall<V>> visited)
    {
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data)
    {
        return visitor.visitConst(val, data);
    }

    @Override
    public Vec4 xeval(Map<Svex<V>, Vec4> memoize)
    {
        return val;
    }

    @Override
    public Svex patch(Map<Svar, Vec4> subst, Map<SvexCall, SvexCall> memoize)
    {
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof SvexQuote && val.equals(((SvexQuote)o).val);
    }

    @Override
    public int hashCode()
    {
        return val.hashCode();
    }
}
