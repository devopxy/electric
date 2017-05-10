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

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

/**
 * An atom with width from left-hand side of SVEX assignment.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHRANGE>.
 */
public class Lhrange
{

    final ACL2Object impl;

    public final int w;
    public final Lhatom atom;

    Lhrange(Module parent, ACL2Object impl)
    {
        this.impl = impl;
        if (consp(impl).bool())
        {
            if (integerp(car(impl)).bool())
            {
                w = car(impl).intValueExact();
                atom = Lhatom.valueOf(parent, cdr(impl));
            } else
            {
                w = 1;
                atom = Lhatom.valueOf(parent, impl);
            }
        } else
        {
            w = 1;
            atom = Lhatom.valueOf(parent, impl);
        }
        Util.check(w >= 1);
    }

    @Override
    public String toString()
    {
        return atom.toString(w);
    }

    public String toLispString()
    {
        return atom.toLispString(w);
    }

    public void markAssigned(BigInteger assignedBits)
    {
        BigInteger mask = BigInteger.ONE.shiftLeft(w).subtract(BigInteger.ONE);
        atom.markAssigned(assignedBits.and(mask));
    }

    public void markUsed()
    {
        atom.markUsed();
    }
}
