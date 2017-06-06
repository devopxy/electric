/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Lhrange.java
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

import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

/**
 * An atom with width from left-hand side of SVEX assignment.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHRANGE>.
 *
 * @param <V> Type of Svex variables
 */
public class Lhrange<V extends Svar>
{
    private final int w;
    private final Lhatom<V> atom;

    Lhrange(Svar.Builder<V> builder, ACL2Object impl, int lsh)
    {
        if (consp(impl).bool())
        {
            if (integerp(car(impl)).bool())
            {
                w = car(impl).intValueExact();
                atom = Lhatom.valueOf(builder, cdr(impl));
            } else
            {
                w = 1;
                atom = Lhatom.valueOf(builder, impl);
            }
        } else
        {
            w = 1;
            atom = Lhatom.valueOf(builder, impl);
        }
        Util.check(w >= 1);
    }

    Lhrange(int w, Lhatom<V> atom)
    {
        this.w = w;
        this.atom = atom;
    }

    public <V1 extends Svar> Lhrange<V1> convertVars(Svar.Builder<V1> builder)
    {
        return new Lhrange<>(w, atom.convertVars(builder));
    }

    public V getVar()
    {
        return atom.getVar();
    }

    public int getWidth()
    {
        return w;
    }

    public int getRsh()
    {
        return atom.getRsh();
    }

    @Override
    public String toString()
    {
        V name = getVar();
        if (name != null)
        {
            return name.toString(BigIntegerUtil.logheadMask(getWidth()).shiftLeft(getRsh()));
        } else
        {
            return w + "'Z";
        }
    }
}
