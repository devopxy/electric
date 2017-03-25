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

/**
 * ACL2 rational number.
 * Its value is a rational number but not an integer number.
 */
class ACL2Rational extends ACL2Object
{
    final Rational r;

    ACL2Rational(Rational r)
    {
        super(false);
        if (r.isInteger())
        {
            throw new IllegalArgumentException();
        }
        this.r = r;
    }

    @Override
    boolean isACL2Number()
    {
        return true;
    }

    @Override
    Rational ratfix()
    {
        return r;
    }

    @Override
    ACL2Object unaryMinus()
    {
        return valueOf(r.negate());
    }

    @Override
    ACL2Object unarySlash()
    {
        return valueOf(r.inverse());
    }

    @Override
    ACL2Object binaryPlus(ACL2Object y)
    {
        return y.binaryPlus(this);
    }

    @Override
    ACL2Object binaryPlus(ACL2Integer y)
    {
        return new ACL2Rational(r.add(y.v));
    }

    @Override
    ACL2Object binaryPlus(ACL2Rational y)
    {
        return valueOf(r.add(y.r));
    }

    @Override
    ACL2Object binaryPlus(ACL2Complex y)
    {
        return y.binaryPlus(this);
    }

    @Override
    ACL2Object binaryStar(ACL2Object y)
    {
        return y.binaryStar(this);
    }

    @Override
    ACL2Object binaryStar(ACL2Integer y)
    {
        return valueOf(r.mul(y.v));
    }

    @Override
    ACL2Object binaryStar(ACL2Rational y)
    {
        return valueOf(r.mul(y.r));
    }

    @Override
    ACL2Object binaryStar(ACL2Complex y)
    {
        return y.binaryStar(this);
    }

    @Override
    int signum()
    {
        return r.signum();
    }

    @Override
    int compareTo(ACL2Object y)
    {
        return -y.compareTo(this);
    }

    @Override
    int compareTo(ACL2Integer y)
    {
        return r.compareTo(y.v);
    }

    @Override
    int compareTo(ACL2Rational y)
    {
        return r.compareTo(y.r);
    }

    @Override
    int compareTo(ACL2Complex y)
    {
        return -y.compareTo(this);
    }

    @Override
    public String rep()
    {
        return r.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ACL2Rational
            && r.equals(((ACL2Rational)o).r);
    }

    @Override
    public int hashCode()
    {
        return r.hashCode();
    }
}
