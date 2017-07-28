/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Address.java
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
import java.util.Arrays;
import java.util.List;

/**
 * Type of the names of wires, module instances, and namespaces (such as datatype fields).
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____ADDRESS>.
 */
public class Address implements SvarName
{
    public static final ACL2Object KEYWORD_ADDRESS = ACL2Object.valueOf("KEYWORD", "ADDRESS");
    public static final ACL2Object KEYWORD_ROOT = ACL2Object.valueOf("KEYWORD", "ROOT");
    public static final int INDEX_NIL = -1;
    public static final int SCOPE_ROOT = -1;

    private final ACL2Object impl;

    public final Path path;
    public final int index;
    public final int scope;

    Address(ACL2Object impl)
    {
        this.impl = impl;
        if (consp(impl).bool() && KEYWORD_ADDRESS.equals(car(impl)))
        {
            List<ACL2Object> list = Util.getList(impl, true);
            path = Path.fromACL2(list.get(1));
            if (integerp(list.get(2)).bool())
            {
                index = list.get(2).intValueExact();
                Util.check(index >= 0);
            } else
            {
                Util.checkNil(list.get(2));
                index = INDEX_NIL;
            }
            if (KEYWORD_ROOT.equals(list.get(3)))
            {
                scope = SCOPE_ROOT;
            } else
            {
                scope = list.get(3).intValueExact();
                Util.check(scope >= 0);
            }
        } else
        {
            path = Path.fromACL2(impl);
            index = INDEX_NIL;
            scope = 0;
        }
    }

    Address(Path path, int index, int scope)
    {
        if (path == null)
        {
            throw new NullPointerException();
        }
        if (index < INDEX_NIL || scope < SCOPE_ROOT)
        {
            throw new IllegalArgumentException();
        }
        if (scope == 0 && index == INDEX_NIL)
        {
            impl = path.getACL2Object();
        } else
        {
            impl = cons(KEYWORD_ADDRESS, cons(
                path.getACL2Object(), cons(
                index >= 0 ? ACL2Object.valueOf(index) : NIL, cons(
                scope >= 0 ? ACL2Object.valueOf(scope) : KEYWORD_ROOT,
                NIL))));
        }
        this.path = path;
        this.index = index;
        this.scope = scope;
    }

    @Override
    public ACL2Object getACL2Object()
    {
        return impl;
    }

    public static Address fromACL2(ACL2Object impl)
    {
        return new Address(impl);
    }

    public Path getPath()
    {
        return path;
    }

    public Integer getIndex()
    {
        return index >= 0 ? index : null;
    }

    public Integer getScope()
    {
        return scope >= 0 ? scope : null;
    }

    public Svar<Address> toVar(SvarBuilder builder)
    {
        return builder.newVar(impl, 0, false);
    }

    @Override
    public String toString(BigInteger mask)
    {
        throw new UnsupportedOperationException();
    }

    public static boolean svarIdxaddrOkp(Svar<Address> svar, int bound)
    {
        Address addr = svar.getName();
        return addr.index == INDEX_NIL || addr.index < bound;
    }

    public static class SvarBuilder extends SvarImpl.Builder<Address>
    {
        @Override
        public Address newName(ACL2Object nameImpl)
        {
            return Address.fromACL2(nameImpl);
        }

        public Svar<Address> makeSimpleSvar(Name name)
        {
            Path path = Path.simplePath(name);
            return newVar(new Address(path, Address.INDEX_NIL, 0), 0, false);
        }

        public Svar<Address> makeScopedSvar(Name scope, Name name)
        {
            Path path = Path.makePath(Arrays.asList(scope), name);
            return newVar(new Address(path, Address.INDEX_NIL, 0), 0, false);
        }
    }
}
