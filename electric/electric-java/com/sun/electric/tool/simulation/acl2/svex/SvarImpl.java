/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SVarImpl.java
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
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a single variable in a symbolic vector expression.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVAR>.
 */
public class SvarImpl<N> extends Svar
{
    private final N name;
    private final int delayImpl;
    private final ACL2Object impl;

    private SvarImpl(N name, int delayImpl, ACL2Object impl)
    {
        this.name = name;
        this.delayImpl = delayImpl;
        this.impl = impl;
    }

    public N getName()
    {
        return name;
    }

    @Override
    public ACL2Object getACL2Name()
    {
        return consp(impl).bool() ? car(cdr(impl)) : impl;
    }

    @Override
    public int getDelay()
    {
        return delayImpl >= 0 ? delayImpl : ~delayImpl;
    }

    @Override
    public boolean isNonblocking()
    {
        return delayImpl < 0;
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

    public abstract static class Builder<N> implements Svar.Builder<SvarImpl<N>>
    {

        private final Map<ACL2Object, SvarImpl<N>> cache = new HashMap<>();

        public abstract N newName(ACL2Object nameImpl);

        public abstract ACL2Object getACL2Object(N name);

        public SvarImpl<N> newVar(N name, int delay, boolean nonblocking)
        {
            ACL2Object nameImpl = getACL2Object(name);
            return newVar(nameImpl, delay, nonblocking);
        }

        @Override
        public SvarImpl<N> newVar(ACL2Object nameImpl, int delay, boolean nonblocking)
        {
            assert delay >= 0;
            boolean simpleName = (stringp(nameImpl).bool()
                || (symbolp(nameImpl).bool() && !booleanp(nameImpl).bool()));
            int delayImpl = nonblocking ? ~delay : delay;
            ACL2Object impl = simpleName && delayImpl == 0
                ? honscopy(nameImpl)
                : hons(KEYWORD_VAR, hons(nameImpl, ACL2Object.valueOf(delayImpl)));
            SvarImpl<N> svar = cache.get(impl);
            if (svar == null)
            {
                N name = newName(nameImpl);
                svar = new SvarImpl<>(name, delayImpl, impl);
                cache.put(impl, svar);
            }
            return svar;
        }
    };
}
