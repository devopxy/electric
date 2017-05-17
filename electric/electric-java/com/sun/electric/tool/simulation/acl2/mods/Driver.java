/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Driver.java
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
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

import java.util.Set;

/**
 * Driver - SVEX expression with strength.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____DRIVER>.
 */
public class Driver
{

    final ACL2Object impl;

    final Module parent;

    public final Svex svex;
    final int strength;

    Driver(Module parent, ACL2Object impl)
    {
        this.parent = parent;
        this.impl = impl;
        svex = Svex.valueOf(parent, car(impl), parent.svexCache);
        strength = cdr(impl).intValueExact();
        Util.check(strength == 6);
        for (Svar svar : svex.collectVars())
        {
            Util.check(svar instanceof SVarExt.LocalWire);
        }
        Util.check(strength >= 0);
    }
    
    @Override
    public String toString()
    {
        assert strength == 6;
        return svex.toString();
    }

    public final Set<SVarExt.LocalWire> collectVars()
    {
        return svex.collectVars(SVarExt.LocalWire.class);
    }

    void markUsed()
    {
        for (SVarExt.LocalWire svar : collectVars())
        {
            svar.markUsed();
        }
    }
}
