/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SVar.java
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
 * A single variable in a symbolic vector expression.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVAR>.
 */
public abstract class Svar
{
    public static ACL2Object KEYWORD_VAR = ACL2Object.valueOf("KEYWORD", "VAR");

    public abstract ACL2Object getACL2Name();

    public abstract int getDelay();

    public abstract boolean isNonblocking();

    public ACL2Object makeACL2Object()
    {
        ACL2Object name = getACL2Name();
        int delay = getDelay();
        boolean nonblocking = isNonblocking();
        if ((stringp(name).bool() || symbolp(name).bool() && !booleanp(name).bool())
            && !nonblocking && delay == 0)
        {
            return name;
        }
        return cons(KEYWORD_VAR, cons(name, ACL2Object.valueOf(nonblocking ? -delay - 1 : delay)));
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Svar)
        {
            Svar that = (Svar)o;
            return this.getACL2Name().equals(that.getACL2Name())
                && this.getDelay() == that.getDelay()
                && this.isNonblocking() == that.isNonblocking();
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + getACL2Name().hashCode();
        hash = 59 * hash + getDelay();
        return hash;
    }

    public static abstract interface Builder<T extends Svar>
    {
        default T createVar(ACL2Object impl)
        {
            if (stringp(impl).bool() || symbolp(impl).bool() && !booleanp(impl).bool())
            {
                return buildVar(impl);
            }
            if (!consp(impl).bool() || !KEYWORD_VAR.equals(car(impl)))
            {
                throw new IllegalArgumentException();
            }
            ACL2Object o = cdr(impl);
            if (!consp(o).bool())
            {
                throw new IllegalArgumentException();
            }
            ACL2Object name = car(o);
            int d = cdr(o).intValueExact();
            if (d == 0 && (stringp(name).bool() || symbolp(name).bool() && !booleanp(name).bool()))
            {
                throw new IllegalArgumentException();
            }
            if (d >= 0)
            {
                return buildVar(name, d);
            } else
            {
                return buildVar(name, -d - 1, true);
            }
        }

        default T buildVar(ACL2Object name)
        {
            return buildVar(name, 0);
        }

        default T buildVar(ACL2Object name, int delay)
        {
            return buildVar(name, delay, false);
        }

        T buildVar(ACL2Object name, int delay, boolean nonblocking);
    }
}
