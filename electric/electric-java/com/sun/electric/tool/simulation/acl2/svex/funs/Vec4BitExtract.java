/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vec4BitExtract.java
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
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import java.math.BigInteger;

/**
 * Coerces an arbitrary 4vec to a 3vec by “unfloating” it, i.e., by turning any Zs into Xes.
 * See<http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____3VEC-FIX>.
 */
public class Vec4BitExtract extends SvexCall
{
    public static final Function FUNCTION = new Function();
    public final Svex index;
    public final Svex x;

    public Vec4BitExtract(Svex index, Svex x)
    {
        super(FUNCTION, index, x);
        this.index = index;
        this.x = x;
    }

    public static class Function extends SvexFunction
    {
        private Function()
        {
            super(FunctionSyms.SV_BITSEL, 2);
        }

        @Override
        public Vec4BitExtract build(Svex... args)
        {
            return new Vec4BitExtract(args[0], args[1]);

        }

        @Override
        public Vec4 apply(Vec4... args)
        {
            Vec4 index = args[0];
            Vec4 x = args[1];
            if (index.isVec2())
            {
                int indv = ((Vec2)index).getVal().intValueExact();
                if (indv >= 0)
                {
                    if (x.isVec2())
                    {
                        BigInteger xv = ((Vec2)x).getVal();
                        return xv.testBit(indv) ? Vec2.ONE : Vec2.ZERO;
                    }
                    return x.getUpper().testBit(indv)
                        ? (x.getLower().testBit(indv) ? Vec2.ONE : Vec4.X1)
                        : (x.getLower().testBit(indv) ? Vec2.Z1 : Vec2.ZERO);
                }
            }
            return Vec4.X1;
        }
    }
}
