/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvexQuote.java
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
package com.sun.electric.tool.simulation.acl2.svex;

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

/**
 * A "quoted constant" 4vec which represents itself.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-QUOTE>.
 */
public class SvexQuote extends Svex
{

    public final Vec4 val;

    SvexQuote(ACL2Object impl)
    {
        this.val = Vec4.valueOf(impl);
    }

    public ACL2Object makeACL2Object()
    {
        if (val.isVec2())
        {
            return val.makeAcl2Object();
        }
        return cons(QUOTE, cons(val.makeAcl2Object(), NIL));
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data)
    {
        return visitor.visitConst(val, data);
    }
}
