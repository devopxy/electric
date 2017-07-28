/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IndexName.java
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
import com.sun.electric.tool.simulation.acl2.svex.SvarImpl;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class IndexName implements SvarName, Svar<IndexName>
{
    private final ACL2Object impl;

    private IndexName(int index)
    {
        if (index < Address.INDEX_NIL)
        {
            throw new IllegalArgumentException();
        }
        impl = honscopy(ACL2Object.valueOf(BigInteger.valueOf(index)));
    }

    @Override
    public ACL2Object getACL2Object()
    {
        return impl;
    }

    @Override
    public String toString(BigInteger mask)
    {
        return Integer.toString(impl.intValueExact());
    }

    @Override
    public IndexName getName()
    {
        return this;
    }

    public Name asName()
    {
        return new Name(impl);
    }

    public Path asPath()
    {
        return Path.simplePath(asName());
    }

    public Address asAddress()
    {
        return Address.fromACL2(impl);
    }

    public int getIndex()
    {
        return impl.intValueExact();
    }

    @Override
    public int getDelay()
    {
        return 0;
    }

    @Override
    public boolean isNonblocking()
    {
        return false;
    }

    public static class SvarBuilder extends SvarImpl.Builder<IndexName>
    {
        private final IndexName INDEX_NIL = new IndexName(Address.INDEX_NIL);
        private final List<IndexName> cache = new ArrayList<>();

        @Override
        public Svar<IndexName> newVar(ACL2Object name, int delay, boolean nonblocking)
        {
            assert delay >= 0;
            if (delay == 0 && !nonblocking)
            {
                return newName(name);
            }
            return super.newVar(name, delay, nonblocking);
        }

        @Override
        public IndexName newName(ACL2Object nameImpl)
        {
            return newName(nameImpl.intValueExact());
        }

        public IndexName newName(int index)
        {
            if (index < 0)
            {
                if (index != Address.INDEX_NIL)
                {
                    throw new IllegalArgumentException();
                }
                return INDEX_NIL;
            }
            while (index >= cache.size())
            {
                cache.add(new IndexName(cache.size()));
            }
            return cache.get(index);
        }

    }
}
