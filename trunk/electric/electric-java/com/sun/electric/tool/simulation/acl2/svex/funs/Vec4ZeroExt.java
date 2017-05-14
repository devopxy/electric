/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vec4ZeroExt.java
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

import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexFunction;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import java.math.BigInteger;
import java.util.Map;

/**
 * Like loghead for 4vecs; the width is also a 4vec.
 * See<http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____4VEC-ZERO-EXT>.
 */
public class Vec4ZeroExt extends SvexCall
{
    public static final Function FUNCTION = new Function();
    public final Svex width;
    public final Svex x;

    public Vec4ZeroExt(Svex width, Svex x)
    {
        super(FUNCTION, width, x);
        this.width = width;
        this.x = x;
    }

    public static class Function extends SvexFunction
    {
        private Function()
        {
            super(FunctionSyms.SV_ZEROX, 2);
        }

        @Override
        public Vec4ZeroExt build(Svex... args)
        {
            return new Vec4ZeroExt(args[0], args[1]);
        }

        @Override
        public Vec4 apply(Vec4... args)
        {
            Vec4 width = args[0];
            Vec4 x = args[1];
            if (width.isVec2())
            {
                int wval = ((Vec2)width).getVal().intValueExact();
                if (wval >= 0)
                {
                    if (wval >= Vec4.BIT_LIMIT)
                    {
                        if (x.getUpper().signum() < 0 || x.getLower().signum() < 0)
                        {
                            throw new IllegalArgumentException("very large integer");

                        }
                    }
                    BigInteger mask = BigIntegerUtil.logheadMask(wval);
                    if (x.isVec2())
                    {
                        BigInteger xv = ((Vec2)x).getVal();
                        return new Vec2(xv.and(mask));
                    }
                    return Vec4.valueOf(
                        x.getUpper().and(mask),
                        x.getLower().and(mask));
                }
            }
            return Vec4.X;
        }

        @Override
        protected BigInteger[] svmaskFor(BigInteger mask, Svex[] args, Map<Svex, Vec4> xevalMemoize)
        {
            Svex width = args[0];
            BigInteger nMask = v4maskAllOrNone(mask);
            Vec4 widthVal = width.xeval(xevalMemoize);
            if (!widthVal.isVec2())
            {
                return new BigInteger[]
                {
                    nMask, nMask
                };
            }
            int widthV = ((Vec2)widthVal).getVal().intValueExact();
            if (widthV < 0)
            {
                return new BigInteger[]
                {
                    nMask, BigInteger.ZERO
                };
            }
            BigInteger widthMask = BigInteger.ONE.shiftLeft(widthV).subtract(BigInteger.ONE);
            return new BigInteger[]
            {
                nMask, mask.and(widthMask)
            };
        }
    }
}
