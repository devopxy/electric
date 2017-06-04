/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vec4Bitor.java
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

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexFunction;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import java.math.BigInteger;
import java.util.Map;

/**
 * Bitwise logical OR of 4vecs.
 * See<http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____4VEC-BITOR>.
 */
public class Vec4Bitor<V extends Svar> extends SvexCall<V>
{
    public static final Function FUNCTION = new Function();
    public final Svex<V> x;
    public final Svex<V> y;

    public Vec4Bitor(Svex<V> x, Svex<V> y)
    {
        super(FUNCTION, x, y);
        this.x = x;
        this.y = y;
    }

    public static class Function extends SvexFunction
    {
        private Function()
        {
            super(FunctionSyms.SV_BITOR, 2);
        }

        @Override
        public <V extends Svar> Vec4Bitor<V> build(Svex<V>... args)
        {
            return new Vec4Bitor<>(args[0], args[1]);
        }

        @Override
        public Vec4 apply(Vec4... args)
        {
            return apply3(args[0].fix3(), args[1].fix3());
        }

        private Vec4 apply3(Vec4 x, Vec4 y)
        {
            if (x.isVec2() && y.isVec2())
            {
                BigInteger xv = ((Vec2)x).getVal();
                BigInteger yv = ((Vec2)x).getVal();
                return new Vec2(xv.or(yv));
            }
            return Vec4.valueOf(
                x.getUpper().or(y.getUpper()),
                x.getLower().or(y.getLower()));

        }

        @Override
        protected <V extends Svar> BigInteger[] svmaskFor(BigInteger mask, Svex<V>[] args, Map<Svex<V>, Vec4> xevalMemoize)
        {
            Svex<V> x = args[0];
            Svex<V> y = args[1];
            Vec4 xv = x.xeval(xevalMemoize);
            Vec4 yv = y.xeval(xevalMemoize);
            BigInteger xOne = xv.getUpper().and(xv.getLower());
            BigInteger yOne = yv.getUpper().and(yv.getLower());
            BigInteger sharedOnes = xOne.and(yOne).and(mask);
            BigInteger xmNonone = mask.andNot(xOne);
            BigInteger ymNonone = mask.andNot(yOne);
            if (sharedOnes.signum() == 0)
            {
                return new BigInteger[]
                {
                    ymNonone, xmNonone
                };
            }
            BigInteger yX = yv.getUpper().andNot(yv.getLower());
            BigInteger ymX = mask.and(yX);
            if (ymX.signum() == 0)
            {
                return new BigInteger[]
                {
                    ymNonone, xmNonone.or(sharedOnes)
                };
            }
            return new BigInteger[]
            {
                ymNonone.or(sharedOnes), xmNonone
            };
        }
    }
}
