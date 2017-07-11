/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Driver.java
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
package com.sun.electric.tool.simulation.acl2.mods;

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.Map;

/**
 * Driver - SVEX expression with strength.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____DRIVER>.
 *
 * @param <N> Type of name of Svex variables
 */
public class Driver<N extends SvarName>
{
    public static final int DEFAULT_STRENGTH = 6;

    public final Svex<N> svex;
    public final int strength;

    Driver(Svar.Builder<N> builder, Map<ACL2Object, Svex<N>> svexCache, ACL2Object impl)
    {
        svex = Svex.valueOf(builder, car(impl), svexCache);
        strength = cdr(impl).intValueExact();
    }

    public Driver(Svex<N> svex)
    {
        this(svex, DEFAULT_STRENGTH);
    }

    public Driver(Svex<N> svex, int strength)
    {
        this.svex = svex;
        this.strength = strength;
    }

    public ACL2Object getACl2Object()
    {
        return cons(svex.getACL2Object(), ACL2Object.valueOf(strength));
    }

    public <N1 extends SvarName> Driver<N1> convertVars(Svar.Builder<N1> builder,
        Map<Svex<N>, Svex<N1>> svexCache)
    {
        Svex<N1> newSvex = svex.convertVars(builder, svexCache);
        return new Driver<>(newSvex, strength);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Driver)
        {
            Driver<?> that = (Driver<?>)o;
            return this.svex.equals(that.svex) && this.strength == that.strength;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + svex.hashCode();
        hash = 53 * hash + strength;
        return hash;
    }

}
