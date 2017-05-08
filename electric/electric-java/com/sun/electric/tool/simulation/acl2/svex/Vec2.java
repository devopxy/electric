/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vec2.java
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

import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

/**
 * A 2vec is a 4vec that has no X or Z bits..
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____2VEC>.
 */
public class Vec2 extends Vec4
{
    public static final BigInteger BI_MINUS_ONE = BigInteger.valueOf(-1);
    
    public static final Vec2 ZERO = new Vec2(BigInteger.ZERO);
    public static final Vec2 ONE = new Vec2(BigInteger.ONE);
    public static final Vec2 MINUS_ONE = new Vec2(BI_MINUS_ONE);
    
    private final BigInteger val;

    public Vec2(BigInteger val)
    {
        if (val == null)
        {
            throw new NullPointerException();
        }
        this.val = val;
    }
    
    public static Vec2 valueOf(boolean b) {
        return b ? MINUS_ONE : ZERO;
    }

    public Vec2(ACL2Object rep)
    {
        val = rep.bigIntegerValueExact();
    }

    public BigInteger getVal()
    {
        return val;
    }

    @Override
    public boolean isVec2()
    {
        return true;
    }

    @Override
    public boolean isVec3()
    {
        return true;
    }

    @Override
    public BigInteger getUpper()
    {
        return val;
    }

    @Override
    public BigInteger getLower()
    {
        return val;
    }

    @Override
    public Vec2 fix3()
    {
        return this;
    }

    @Override
    public ACL2Object makeAcl2Object()
    {
        return ACL2Object.valueOf(val);
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof Vec2 && val.equals(((Vec2)o).val);
    }

    @Override
    public int hashCode()
    {
        return val.hashCode();
    }
}
