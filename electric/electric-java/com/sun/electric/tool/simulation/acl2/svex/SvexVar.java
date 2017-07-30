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

import com.sun.electric.tool.simulation.acl2.mods.Lhatom;
import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A variable, which represents a 4vec.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-VAR>.
 *
 * @param <N> Type of name of Svex variables
 */
public class SvexVar<N extends SvarName> extends Svex<N>
{
    public Svar<N> svar;
    private final ACL2Object impl;

    public SvexVar(Svar<N> svar)
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
    public <N1 extends SvarName> Svex<N1> convertVars(Svar.Builder<N1> builder, Map<Svex<N>, Svex<N1>> cache)
    {
        Svex<N1> svex = cache.get(this);
        if (svex == null)
        {
            Svar<N1> newVar = builder.newVar(svar);
            svex = new SvexVar<>(newVar);
            cache.put(this, svex);
        }
        return svex;
    }

    @Override
    public <N1 extends SvarName> Svex<N1> addDelay(int delay, Svar.Builder<N1> builder, Map<Svex<N>, Svex<N1>> cache)
    {
        Svex<N1> svex = cache.get(this);
        if (svex == null)
        {
            svex = new SvexVar<>(builder.addDelay(svar, delay));
            cache.put(this, svex);
        }
        return svex;
    }

    @Override
    protected void collectVars(Set<Svar<N>> result, Set<SvexCall<N>> visited)
    {
        result.add(svar);
    }

    @Override
    public <R, D> R accept(Visitor<N, R, D> visitor, D data)
    {
        return visitor.visitVar(svar, data);
    }

    @Override
    public Vec4 xeval(Map<Svex<N>, Vec4> memoize)
    {
        return Vec4.X;
    }

    @Override
    public Svex<N> patch(Map<Svar<N>, Vec4> subst, Map<SvexCall<N>, SvexCall<N>> memoize)
    {
        Vec4 val = subst.get(svar);
        return val != null ? SvexQuote.valueOf(val) : this;
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
    public Lhs<N> lhsBound(int w)
    {
        Lhatom<N> atom = Lhatom.valueOf(svar);
        Lhrange<N> range = new Lhrange<>(w, atom);
        return new Lhs<>(Collections.singletonList(range));
    }

    @Override
    public Lhs<N> toLhs()
    {
        return new Lhs<>(Collections.emptyList());
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
