/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vec4Concat.java
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
 * Like logapp for 4vecs; the width is also a 4vec.
 * See<http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____4VEC-CONCAT>.
 */
public class Vec4Concat extends SvexCall
{
    public static final Function FUNCTION = new Function();
    public Svex width;
    public Svex low;
    public Svex high;

    public Vec4Concat(Svex width, Svex low, Svex high)
    {
        super(FUNCTION, width, low, high);
        this.width = width;
        this.low = low;
        this.high = high;
    }

    public static class Function extends SvexFunction
    {
        private Function()
        {
            super(FunctionSyms.SV_CONCAT, 3);
        }

        @Override
        public Vec4Concat build(Svex... args)
        {
            return new Vec4Concat(args[0], args[1], args[2]);
        }

        @Override
        public Vec4 apply(Vec4... args)
        {
            Vec4 width = args[0];
            Vec4 l = args[1];
            Vec4 h = args[2];
            if (width.isVec2())
            {
                int wval = ((Vec2)width).getVal().intValueExact();
                if (wval >= 0)
                {
                    if (l.isVec2() && h.isVec2())
                    {
                        BigInteger lv = ((Vec2)l).getVal();
                        BigInteger hv = ((Vec2)h).getVal();
                        if (wval >= Vec4.BIT_LIMIT)
                        {
                            if (hv.intValueExact() != (lv.signum() < 0 ? -1 : 0))
                            {
                                throw new IllegalArgumentException("very large integer");
                            }
                        }
                        return new Vec2(BigIntegerUtil.loghead(wval, lv).or(hv.shiftLeft(wval)));
                    }
                    if (wval >= Vec4.BIT_LIMIT)
                    {
                        if (h.getUpper().intValueExact() != (l.getUpper().signum() < 0 ? -1 : 0))
                        {
                            throw new IllegalArgumentException("very large integer");
                        }
                        if (h.getLower().intValueExact() != (l.getLower().signum() < 0 ? -1 : 0))
                        {
                            throw new IllegalArgumentException("very large integer");
                        }
                    }
                    BigInteger mask = BigIntegerUtil.logheadMask(wval);
                    return Vec4.valueOf(
                        l.getUpper().and(mask).or(h.getUpper().shiftLeft(wval)),
                        l.getLower().and(mask).or(h.getLower().shiftLeft(wval))
                    );
                }
            }
            return Vec4.X;
        }

        @Override
        protected BigInteger[] svmaskFor(BigInteger mask, Svex[] args, Map<Svex, Vec4> xevalMemoize)
        {
            if (mask.signum() == 0)
            {
                return new BigInteger[]
                {
                    BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
                };
            }
            Svex width = args[0];
            Vec4 widthVal = width.xeval(xevalMemoize);
            if (!widthVal.isVec2())
            {
                BigInteger argMask = maskForGenericSignx(mask);
                return new BigInteger[]
                {
                    BigIntegerUtil.MINUS_ONE, argMask, argMask
                };
            }
            int widthV = ((Vec2)widthVal).getVal().intValueExact();
            if (widthV < 0)
            {
                return new BigInteger[]
                {
                    BigIntegerUtil.MINUS_ONE, BigInteger.ZERO, BigInteger.ZERO
                };
            }
            return new BigInteger[]
            {
                BigIntegerUtil.MINUS_ONE,
                BigIntegerUtil.loghead(widthV, mask),
                mask.shiftRight(widthV)
            };
        }
    }
}