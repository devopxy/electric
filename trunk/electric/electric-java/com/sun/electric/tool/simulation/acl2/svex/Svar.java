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
import java.math.BigInteger;

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
    
    public abstract String toString(BigInteger mask);

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
        default T fromACL2(ACL2Object rep)
        {
            if (consp(rep).bool())
            {
                ACL2Object name = car(cdr(rep));
                int delay = cdr(cdr(rep)).intValueExact(); // Support only int delay
                boolean nonblocking = false;
                if (delay < 0)
                {
                    delay = ~delay;
                    nonblocking = true;
                }
                if (KEYWORD_VAR.equals(car(rep)))
                {
                    if (!(stringp(name).bool() || symbolp(name).bool() && !booleanp(name).bool())
                        || delay != 0
                        || nonblocking)
                    {
                        return Builder.this.newVar(name, delay, nonblocking);
                    }
                }
            } else if (stringp(rep).bool() || symbolp(rep).bool() && !booleanp(rep).bool())
            {
                return newVar(rep);
            }
            throw new IllegalArgumentException();
        }

        default T newVar(ACL2Object name)
        {
            return Builder.this.newVar(name, 0);
        }

        default T newVar(ACL2Object name, int delay)
        {
            return Builder.this.newVar(name, delay, false);
        }

        T newVar(ACL2Object name, int delay, boolean nonblocking);
    }
}
