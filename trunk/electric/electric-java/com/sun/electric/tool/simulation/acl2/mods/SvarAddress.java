/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvarAddress.java
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
import static com.sun.electric.tool.simulation.acl2.svex.Svar.KEYWORD_VAR;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Svex variable with Address name
 */
public class SvarAddress extends Svar
{
    private final ACL2Object impl;
    private final Address address;

    private SvarAddress(ACL2Object impl)
    {
        this.impl = impl;
        address = new Address(consp(impl).bool() ? car(cdr(impl)) : impl);
    }

    @Override
    public ACL2Object getACL2Name()
    {
        return address.getACL2Object();
    }

    public Address getName()
    {
        return address;
    }

    @Override
    public int getDelay()
    {
        if (consp(impl).bool())
        {
            int delay = cdr(cdr(impl)).intValueExact();
            return delay >= 0 ? delay : ~delay;
        } else
        {
            return 0;
        }
    }

    @Override
    public boolean isNonblocking()
    {
        if (consp(impl).bool())
        {
            int delay = cdr(cdr(impl)).intValueExact();
            return delay < 0;
        } else
        {
            return false;
        }
    }

    public ACL2Object getACL2Object()
    {
        return impl;
    }

    @Override
    public String toString(BigInteger mask)
    {
        return impl.rep();
    }

    public static class Builder implements Svar.Builder<SvarAddress>
    {
        private final Map<ACL2Object, SvarAddress> cache = new HashMap<>();

        @Override
        public SvarAddress newVar(ACL2Object name, int delay, boolean nonblocking)
        {
            assert delay >= 0;
            ACL2Object impl;
            boolean simpleName = (stringp(name).bool() || (symbolp(name).bool() && !booleanp(name).bool()));
            if (simpleName && !nonblocking && delay == 0)
            {
                impl = name;
            } else
            {
                if (nonblocking)
                {
                    delay = ~delay;
                }
                impl = cons(KEYWORD_VAR, cons(name, ACL2Object.valueOf(delay)));
            }
            SvarAddress svar = cache.get(impl);
            if (svar == null)
            {
                svar = new SvarAddress(impl);
                cache.put(impl, svar);
            }
            return svar;
        }

    }
}
