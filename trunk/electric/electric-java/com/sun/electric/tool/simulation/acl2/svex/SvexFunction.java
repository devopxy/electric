/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvexFunction.java
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

import com.sun.electric.tool.simulation.acl2.svex.funs.*;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Our expressions may involve the application of a fixed set of known functions.
 * There are functions available for modeling many bit-vector operations like bitwise and, plus,
 * less-than, and other kinds of hardware operations like resolving multiple drivers, etc.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____FUNCTIONS>.
 */
public abstract class SvexFunction
{
    public final ACL2Object fn;
    public final int arity;

    private static final Map<ACL2Object, SvexFunction> FUNCTIONS = new HashMap<>();

    static
    {
        b(Vec4Fix.FUNCTION);
        b(Vec4BitExtract.FUNCTION);
        b(Vec3Fix.FUNCTION);
        b(Vec4Bitnot.FUNCTION);
        b(Vec4Bitand.FUNCTION);
        b(Vec4Bitor.FUNCTION);
        b(Vec4Bitxor.FUNCTION);
        b(Vec4Res.FUNCTION);
        b(Vec4Resand.FUNCTION);
        b(Vec4Resor.FUNCTION);
        b(Vec4Override.FUNCTION);
        b(Vec4Onset.FUNCTION);
        b(Vec4Offset.FUNCTION);
        b(Vec4ReductionAnd.FUNCTION);
        b(Vec4ReductionOr.FUNCTION);
        b(Vec4Parity.FUNCTION);
        b(Vec4ZeroExt.FUNCTION);
        b(Vec4SignExt.FUNCTION);
        b(Vec4Concat.FUNCTION);
        b(Vec4RevBlocks.FUNCTION);
        b(Vec4Rsh.FUNCTION);
        b(Vec4Lsh.FUNCTION);
        b(Vec4Plus.FUNCTION);
        b(Vec4Minus.FUNCTION);
        b(Vec4Uminus.FUNCTION);
        b(Vec4Times.FUNCTION);
        b(Vec4Quotient.FUNCTION);
        b(Vec4Remainder.FUNCTION);
        b(Vec4Xdet.FUNCTION);
        b(Vec4Countones.FUNCTION);
        b(Vec4Onehot.FUNCTION);
        b(Vec4Onehot0.FUNCTION);
        b(Vec4Lt.FUNCTION);
        b(Vec4Equality.FUNCTION);
        b(Vec4CaseEquality.FUNCTION);
        b(Vec4Wildeq.FUNCTION);
        b(Vec4WildeqSafe.FUNCTION);
        b(Vec4Symwildeq.FUNCTION);
        b(Vec4Clog2.FUNCTION);
        b(Vec4Pow.FUNCTION);
        b(Vec4Ite.FUNCTION);
        b(Vec4IteStmt.FUNCTION);
        b(Vec4IteBit.FUNCTION);
        b(Vec4PartSelect.FUNCTION);
        b(Vec4PartInstall.FUNCTION);
    }

    private static void b(SvexFunction builder)
    {
        SvexFunction old = FUNCTIONS.put(builder.fn, builder);
        assert old == null;
    }

    static SvexFunction valueOf(ACL2Object fn, int arity)
    {
        SvexFunction fun = FUNCTIONS.get(fn);
        if (fun != null && fun.arity == arity)
        {
            assert fun.fn.equals(fn);
        } else
        {
            fun = new Undefined(fn, arity);
        }
        return fun;
    }

    public SvexFunction(ACL2Object fn, int arity)
    {
        if (!isFnSym(fn))
        {
            throw new IllegalArgumentException();
        }
        this.fn = fn;
        this.arity = arity;
    }

    public abstract SvexCall build(Svex... args);

    public abstract Vec4 apply(Vec4... args);

    protected Vec4 shiftCore(int amt, Vec4 src)
    {
        if (amt >= Vec4.BIT_LIMIT)
        {
            if (src.getUpper().signum() != 0 || src.getLower().signum() != 0)
            {
                throw new IllegalArgumentException("very large integer");
            }
        }
        if (src.isVec2())
        {
            BigInteger srcv = ((Vec2)src).getVal();
            return new Vec2(srcv.shiftLeft(amt));
        }
        return Vec4.valueOf(
            src.getUpper().shiftLeft(amt),
            src.getLower().shiftLeft(amt));
    }

    protected Vec4 eq(Vec4 x, Vec4 y, BigInteger zMask)
    {
        if (x.isVec2() && y.isVec2() && zMask.signum() == 0)
        {
            BigInteger xv = ((Vec2)x).getVal();
            BigInteger yv = ((Vec2)y).getVal();
            return Vec2.valueOf(xv.equals(yv));
        }
        BigInteger xMask = x.getUpper().xor(x.getLower())
            .or(y.getUpper().xor(y.getLower()));
        // 3vec-bitnot(4vec-bitxor(x,y))
        BigInteger u = x.getLower().xor(y.getLower()).andNot(xMask).not();
        BigInteger l = x.getUpper().xor(y.getUpper()).or(xMask).not();
        u = u.or(zMask);
        l = l.or(zMask);
        if (l.equals(u)) {
            return Vec2.valueOf(u.equals(Vec2.BI_MINUS_ONE));
        }
        return u.equals(Vec2.BI_MINUS_ONE) ? Vec4.X : Vec2.ZERO;
    }

    public static boolean isFnSym(ACL2Object o)
    {
        return symbolp(o).bool() && !QUOTE.equals(o) && !keywordp(o).bool();
    }

    private static class Undefined extends SvexFunction
    {
        public Undefined(ACL2Object fn, int arity)
        {
            super(fn, arity);
        }

        @Override
        public SvexCall build(Svex... args)
        {
            assert args.length == arity;
            return new SvexCall(this, args);
        }

        @Override
        public Vec4 apply(Vec4... args)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
