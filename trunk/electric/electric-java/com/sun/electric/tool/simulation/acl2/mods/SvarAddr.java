/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvarAddr.java
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
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Svar whose name is Address
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVAR-ADDR-P>
 */
public class SvarAddr extends Svar
{
    private final Address name;
    private final int delay;
    private final boolean nonblocking;
    private final ACL2Object impl;

    SvarAddr(Address name, int delay, boolean nonblocking)
    {
        this.name = name;
        this.delay = delay;
        this.nonblocking = nonblocking;
        ACL2Object nameImpl = name.getACL2Object();
        if ((stringp(nameImpl).bool() || symbolp(nameImpl).bool() && !booleanp(nameImpl).bool())
            && !nonblocking && delay == 0)
        {
            impl = nameImpl;
        } else
        {
            impl = hons(KEYWORD_VAR, hons(nameImpl, ACL2Object.valueOf(nonblocking ? -delay - 1 : delay)));
        }
    }

    public Address getAddress()
    {
        return name;
    }

    @Override
    public ACL2Object getACL2Name()
    {
        return name.getACL2Object();
    }

    @Override
    public int getDelay()
    {
        return delay;
    }

    @Override
    public boolean isNonblocking()
    {
        return nonblocking;
    }

    public SvarAddr withIndex(int index)
    {
        if (name.index == index)
        {
            return this;
        }
        Address newAddress = new Address(name.path, index, name.scope);
        return new SvarAddr(newAddress, delay, nonblocking);
    }

    @Override
    public String toString(BigInteger mask)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static SvarAddr makeSimpleSvar(Name name)
    {
        Path path = Path.simplePath(name);
        return new SvarAddr(new Address(path, Address.INDEX_NIL, 0), 0, false);
    }

    public static SvarAddr makeScopedSvar(Name scope, Name name)
    {
        Path path = Path.makePath(Arrays.asList(scope), name);
        return new SvarAddr(new Address(path, Address.INDEX_NIL, 0), 0, false);
    }

    public static class Builder implements Svar.Builder<SvarAddr>
    {

        @Override
        public SvarAddr newVar(ACL2Object name, int delay, boolean nonblocking)
        {
            return new SvarAddr(new Address(name), delay, nonblocking);
        }

    }
}
