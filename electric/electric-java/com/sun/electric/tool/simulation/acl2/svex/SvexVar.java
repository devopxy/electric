/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvexVar.java
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
import java.util.Map;

/**
 * A variable, which represents a 4vec.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-VAR>.
 */
public class SvexVar extends Svex
{
    public Svar svar;

    public SvexVar(Svar svar)
    {
        if (svar == null)
        {
            throw new NullPointerException();
        }
        this.svar = svar;
    }

    @Override
    public ACL2Object makeACL2Object()
    {
        return svar.makeACL2Object();
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data)
    {
        return visitor.visitVar(svar, data);
    }

    @Override
    public Vec4 xeval(Map<Svex, Vec4> memoize)
    {
        return Vec4.X;
    }

    @Override
    public Svex patch(Map<Svar, Vec4> subst, Map<SvexCall, SvexCall> memoize)
    {
        Vec4 val = subst.get(svar);
        return val != null ? new SvexQuote(val) : this;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof SvexVar && svar.equals(((SvexVar)o).svar);
    }

    @Override
    public int hashCode()
    {
        return svar.hashCode();
    }
}
