/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvarExt.java
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

import java.util.Map;

/**
 * SVAR extended by parent, instance and wire.
 */
public class SVarExt extends Svar
{

    final Module parent;
    public final ACL2Object name;
    public final int delay;
    public final boolean nonblocking;

    public ModInst inst;
    public Wire wire;

    SVarExt(Module parent, ACL2Object name, int delay, boolean nonblocking)
    {
        this.parent = parent;
        this.name = name;
        this.delay = delay;
        this.nonblocking = nonblocking;
    }

    @Override
    public ACL2Object getACL2Name()
    {
        return name;
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

    public String toString(int width, int rsh)
    {
        return (inst == null ? "" : inst.instname + ".")
            + wire.toString(width, rsh)
            + (delay != 0 ? "@" + delay : "")
            + (nonblocking ? "NONBLOCKING" : "");
    }

    public String toLispString(int width, int rsh)
    {
        if (inst != null || delay != 0 || nonblocking)
        {
            throw new UnsupportedOperationException();
        }
        if (width == wire.width)
        {
            return wire.name.toLispString();
        } else
        {
            return wire.toLispString(width, rsh);
        }
    }

    @Override
    public String toString()
    {
        return toString(wire.width, 0);
    }

    public void check(Map<ModName, Module> modalist)
    {
        Wire w;
        if (consp(name).bool())
        {
            inst = parent.instsIndex.get(new Name(car(name)));
            Module sm = modalist.get(inst.modname);
            w = sm.wiresIndex.get(new Name(cdr(name)));
            w.exported = true;
        } else
        {
            inst = null;
            w = parent.wiresIndex.get(new Name(name));
        }
        Util.check(w != null);
        if (wire == null)
        {
            wire = w;
        } else
        {
            Util.check(wire == w);
        }
    }

    public void check(Map<ModName, Module> modalist, boolean assign)
    {
        check(modalist);
        if (inst == null)
        {
            if (assign)
            {
                wire.assigned = true;
            } else
            {
                wire.used = true;
            }
        }
    }
}
