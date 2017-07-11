/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ModInst.java
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
 * SV module instance.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODINST>.
 */
public class ModInst
{
    public final Name instname;
    public final ModName modname;

    public ModInst(Name instname, ModName modname)
    {
        if (instname == null || modname == null)
        {
            throw new NullPointerException();
        }
        this.instname = instname;
        this.modname = modname;
    }

    ModInst(ACL2Object impl)
    {
        instname = new Name(car(impl));
        modname = ModName.valueOf(cdr(impl));
    }

    public ACL2Object getACL2Object()
    {
        return cons(instname.getACL2Object(), modname.getACL2Object());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ModInst)
        {
            ModInst that = (ModInst)o;
            return this.instname.equals(that.instname)
                && this.modname.equals(that.modname);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 73 * hash + instname.hashCode();
        hash = 73 * hash + modname.hashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return instname + ":" + modname;
    }
}
