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

import com.sun.electric.tool.simulation.acl2.mods.Util;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a single variable in a symbolic vector expression.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVAR>.
 *
 * @param <N> Type of name of Svex variables
 */
public class SvarImpl<N extends SvarName> implements Svar<N>
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

    @Override
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
    public String toString()
    {
        return toString(null);
    }

    @Override
    public String toString(BigInteger mask)
    {
        String s = mask != null ? name.toString(mask) : name.toString();
        if (isNonblocking())
        {
            s = "#?" + getDelay() + " " + s;
        } else if (getDelay() != 0)
        {
            s = "#" + getDelay() + " " + s;
        }
        return s;
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

    public abstract static class Builder<N extends SvarName> implements Svar.Builder<N>
    {
        private final Map<ACL2Object, N> nameCache = new HashMap<>();
        private final Map<ACL2Object, Svar<N>> svarCache = new HashMap<>();

        @Override
        public Svar<N> newVar(N name, int delay, boolean nonblocking)
        {
            assert delay >= 0;
            ACL2Object nameImpl = name.getACL2Object();
            boolean simpleName = (stringp(nameImpl).bool()
                || (symbolp(nameImpl).bool() && !booleanp(nameImpl).bool()));
            int delayImpl = nonblocking ? ~delay : delay;
            ACL2Object impl = simpleName && delayImpl == 0
                ? honscopy(nameImpl)
                : hons(KEYWORD_VAR, hons(nameImpl, ACL2Object.valueOf(delayImpl)));
            Svar<N> svar = svarCache.get(impl);
            if (svar == null)
            {
                N cachedName = nameCache.get(nameImpl);
                if (cachedName == null)
                {
                    cachedName = name;
                    nameCache.put(nameImpl, name);
                }
                Util.check(cachedName == name);
                svar = new SvarImpl<>(name, delayImpl, impl);
                svarCache.put(impl, svar);
            }
            return svar;
        }

        @Override
        public Svar<N> newVar(ACL2Object nameImpl, int delay, boolean nonblocking)
        {
            assert delay >= 0;
            boolean simpleName = (stringp(nameImpl).bool()
                || (symbolp(nameImpl).bool() && !booleanp(nameImpl).bool()));
            int delayImpl = nonblocking ? ~delay : delay;
            ACL2Object impl = simpleName && delayImpl == 0
                ? honscopy(nameImpl)
                : hons(KEYWORD_VAR, hons(nameImpl, ACL2Object.valueOf(delayImpl)));
            Svar<N> svar = svarCache.get(impl);
            if (svar == null)
            {
                N name = nameCache.get(nameImpl);
                if (name == null)
                {
                    name = newName(nameImpl);
                    nameCache.put(nameImpl, name);
                }
                svar = new SvarImpl<>(name, delayImpl, impl);
                svarCache.put(impl, svar);
            }
            return svar;
        }
    };
}
