/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Wire.java
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
 * Wire info as stored in an svex module.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____WIRE>.
 */
public class Wire
{
    final ACL2Object impl;

    public final Name name;
    public final int width;
    final int low_idx;
    final int delay;
    final boolean revp;
    final ACL2Object wiretype;

    final Module parent;

    public boolean assigned, used, exported;

    Wire(Module parent, ACL2Object impl)
    {
        this.parent = parent;
        this.impl = impl;
        ACL2Object cons00 = car(impl);
        name = new Name(car(cons00));
        ACL2Object cons001 = cdr(cons00);
        width = car(cons001).intValueExact();
        Util.check(width >= 1);
        low_idx = cdr(cons001).intValueExact();
        if (consp(cdr(impl)).bool())
        {
            ACL2Object cons01 = cdr(impl);
            if (symbolp(car(cons01)).bool())
            {
                Util.checkNil(car(cons01));
                delay = 0;
            } else
            {
                delay = car(cons01).intValueExact();
            }
            if (consp(cdr(cons01)).bool())
            {
                ACL2Object cons011 = cdr(cons01);
                if (NIL.equals(car(cons011)))
                {
                    Util.checkNil(car(cons011));
                    revp = false;
                } else
                {
                    Util.check(car(cons011).equals(T));
                    revp = true;
                }
                wiretype = cdr(cons011);
            } else
            {
                Util.checkNil(cdr(cons01));
                revp = false;
                wiretype = cdr(cons01);
            }
        } else
        {
            Util.checkNil(cdr(impl));
            delay = 0;
            revp = false;
            wiretype = cdr(impl);
        }
    }

    public String toString(int width, int rsh)
    {
        if (revp)
        {
            throw new UnsupportedOperationException();
        }
        return name
            + "[" + (width == 1 ? "" : (low_idx + rsh + width - 1) + ":")
            + (low_idx + rsh) + "]";
    }

    public String toLispString(int width, int rsh)
    {
        if (revp)
        {
            throw new UnsupportedOperationException();
        }
        return name.toLispString()
            + "[" + (width == 1 ? "" : (low_idx + rsh + width - 1) + "..")
            + (low_idx + rsh) + "]";
    }

    @Override
    public String toString()
    {
        Util.checkNil(wiretype);
        return toString(width, 0)
            + (delay != 0 ? "@" + delay : "");
    }
}
