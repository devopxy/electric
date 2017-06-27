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

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.Map;
import java.util.Set;

/**
 * A variable, which represents a 4vec.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-VAR>.
 *
 * @param <V> Type of Svex variables
 */
public class SvexVar<V extends Svar> extends Svex<V>
{
    public V svar;
    private final ACL2Object impl;

    public SvexVar(V svar)
    {
        if (svar == null)
        {
            throw new NullPointerException();
        }
        this.svar = svar;
        impl = honscopy(svar.makeACL2Object());
    }

    @Override
    public ACL2Object getACL2Object()
    {
        return impl;
    }

    @Override
    public <V1 extends Svar> Svex<V1> convertVars(Svar.Builder<V1> builder, Map<Svex<V>, Svex<V1>> cache)
    {
        Svex<V1> svex = cache.get(this);
        if (svex == null)
        {
            V1 newVar = builder.newVar(svar);
            svex = new SvexVar<>(newVar);
            cache.put(this, svex);
        }
        return svex;
    }

    @Override
    public <V1 extends Svar> Svex<V1> addDelay(int delay, Svar.Builder<V1> builder, Map<Svex<V>, Svex<V1>> cache)
    {
        Svex<V1> svex = cache.get(this);
        if (svex == null)
        {
            svex = new SvexVar<>(builder.addDelay(svar, delay));
            cache.put(this, svex);
        }
        return svex;
    }

    @Override
    protected void collectVars(Set<V> result, Set<SvexCall<V>> visited)
    {
        result.add(svar);
    }

    @Override
    public <R, D> R accept(Visitor<V, R, D> visitor, D data)
    {
        return visitor.visitVar(svar, data);
    }

    @Override
    public Vec4 xeval(Map<Svex<V>, Vec4> memoize)
    {
        return Vec4.X;
    }

    @Override
    public Svex<V> patch(Map<V, Vec4> subst, Map<SvexCall<V>, SvexCall<V>> memoize)
    {
        Vec4 val = subst.get(svar);
        return val != null ? new SvexQuote(val) : this;
    }

    @Override
    public boolean isLhsUnbounded()
    {
        return true;
    }

    @Override
    public boolean isLhs()
    {
        return false;
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
