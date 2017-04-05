/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ModName.java
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

import com.sun.electric.util.acl2.ACL2Object;

import java.util.HashMap;
import java.util.Map;

/**
 * A type for names of modules and other hierarchical scopes.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODNAME>.
 */
public class ModName
{
    public final ACL2Object impl;

    private ModName(ACL2Object impl)
    {
        this.impl = impl;
    }

    private static final Map<ACL2Object, ModName> allModNames = new HashMap<>();

    public static ModName valueOf(ACL2Object o)
    {
        ModName mn = allModNames.get(o);
        if (mn == null)
        {
            mn = new ModName(o);
            allModNames.put(o, mn);
        }
        return mn;
    }

    @Override
    public String toString()
    {
        return impl.rep();
    }
}
