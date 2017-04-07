/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vec4IteBit.java
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
package com.sun.electric.tool.simulation.acl2.svex.funs;

import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexFunction;

/**
 * Bitwise multiple if-then-elses of 4vecs; doesn’t unfloat then/else values.
 * See<http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____4VEC-BIT_F3>.
 */
public class Vec4IteBit extends SvexCall
{
    public static final Function FUNCTION = new Function();
    public final Svex test;
    public final Svex then;
    public final Svex els;

    public Vec4IteBit(Svex test, Svex then, Svex els)
    {
        super(FUNCTION, test, then, els);
        this.test = test;
        this.then = then;
        this.els = els;
    }

    public static class Function extends SvexFunction
    {
        private Function()
        {
            super(FunctionSyms.SV_BIT_QUEST, 3);
        }

        @Override
        public Vec4IteBit build(Svex... args)
        {
            return new Vec4IteBit(args[0], args[1], args[2]);
        }
    }
}
