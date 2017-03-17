/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Rational.java
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
package com.sun.electric.util.acl2;

import java.math.BigInteger;

/**
 * ACL2 rational number.
 * Its value is a rational number
 */
public class ACL2Rational extends ACL2Number
{

    public final BigInteger n;
    public final BigInteger d;

    ACL2Rational(int id, BigInteger n, BigInteger d)
    {
        super(id);
        if (d.compareTo(BigInteger.ONE) <= 0 || !n.gcd(d).equals(BigInteger.ONE))
        {
            throw new IllegalArgumentException();
        }
        this.n = n;
        this.d = d;
    }

    @Override
    public ACL2Rational asRat()
    {
        return this;
    }

    @Override
    public String rep()
    {
        return n.toString() + "/" + d.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ACL2Rational
            && n.equals(((ACL2Rational)o).n)
            && d.equals(((ACL2Rational)o).d);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + n.hashCode();
        hash = 71 * hash + d.hashCode();
        return hash;
    }
}
