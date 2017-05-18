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
import java.util.HashMap;

import java.util.Map;

/**
 * SV module instance.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODINST>.
 */
public class ModInst
{

    final ACL2Object impl;

    public final Name instname;
    public final ModName modname;

    final Module parent;
    final Module proto;
    final Map<Name, SVarExt.PortInst> portInsts = new HashMap<>();

    ModInst(Module parent, ACL2Object impl, Map<ModName, Module> downTop)
    {
        this.impl = impl;
        this.parent = parent;
        instname = new Name(car(impl));
        modname = ModName.valueOf(cdr(impl));
        proto = downTop.get(modname);
        Util.check(proto != null);
    }

    SVarExt.PortInst newPortInst(ACL2Object name)
    {
        assert instname.impl.equals(car(name));
        Wire wire = proto.wiresIndex.get(new Name(cdr(name)));
        SVarExt.PortInst pi = portInsts.get(wire.name);
        if (pi == null)
        {
            pi = new SVarExt.PortInst(parent, name);
            portInsts.put(wire.name, pi);
        }
        return pi;
    }

    @Override
    public String toString()
    {
        return instname + ":" + modname;
    }
}
