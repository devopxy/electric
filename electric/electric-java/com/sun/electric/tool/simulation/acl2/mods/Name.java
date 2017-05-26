/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Name.java
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

/**
 * Type of the names of wires, module instances, and namespaces (such as datatype fields).
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____NAME>.
 */
public class Name
{

    public final ACL2Object impl;

    Name(ACL2Object impl)
    {
        this.impl = impl;
        if (stringp(impl).bool())
        {
        } else if (integerp(impl).bool())
        {
        } else if (symbolp(impl).bool())
        {
            Util.check(impl.equals(Util.KEYWORD_SELF));
        } else if (consp(impl).bool())
        {
            Util.check(car(impl).equals(Util.KEYWORD_ANONYMOIUS));
        } else
        {
            Util.check(false);
        }
    }

    public ACL2Object getACL2Object()
    {
        return impl;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof Name && impl.equals(((Name)o).impl);
    }

    @Override
    public int hashCode()
    {
        return impl.hashCode();
    }

    @Override
    public String toString()
    {
        if (stringp(impl).bool())
        {
            return impl.stringValueExact();
        } else
        {
            return "'" + impl.rep();
        }
    }

    public String toLispString()
    {
        return stringp(impl).bool() || integerp(impl).bool() ? impl.rep() : "'" + impl.rep();
    }
}
