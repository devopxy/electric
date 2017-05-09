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
public abstract class SVarExt extends Svar
{

    final Module parent;
    public final ACL2Object name;

    public Wire wire;

    SVarExt(Module parent, ACL2Object name)
    {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public ACL2Object getACL2Name()
    {
        return name;
    }

    @Override
    public boolean isNonblocking()
    {
        return false;
    }

    public abstract String toString(int width, int rsh);

    public abstract String toLispString(int width, int rsh);

    @Override
    public String toString()
    {
        return toString(wire.width, 0);
    }

    public static class PortInst extends SVarExt
    {
        public final ModInst inst;

        public PortInst(Module parent, ACL2Object name)
        {
            super(parent, name);
            inst = parent.instsIndex.get(new Name(car(name)));
            Module sm = inst.proto;
            wire = sm.wiresIndex.get(new Name(cdr(name)));
            wire.exported = true;
        }

        @Override
        public int getDelay()
        {
            return 0;
        }

        @Override
        public String toString(int width, int rsh)
        {
            return inst.instname + "." + wire.toString(width, rsh);
        }

        @Override
        public String toLispString(int width, int rsh)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString()
        {
            return toString(wire.width, 0);
        }

    }

    public static class LocalWire extends SVarExt
    {
        public final int delay;

        public LocalWire(Module parent, ACL2Object name, int delay)
        {
            super(parent, name);
            this.delay = delay;
            wire = parent.wiresIndex.get(new Name(name));
        }

        @Override
        public int getDelay()
        {
            return delay;
        }

        public void check(Map<ModName, Module> modalist, boolean assign)
        {
            if (!assign)
            {
                wire.used = true;
            }
        }

        @Override
        public String toString(int width, int rsh)
        {
            return wire.toString(width, rsh)
                + (delay != 0 ? "@" + delay : "");
        }

        @Override
        public String toLispString(int width, int rsh)
        {
            if (delay != 0)
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

    }
}
