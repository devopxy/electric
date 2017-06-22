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

import java.util.HashMap;
import java.util.Map;

/**
 * ACL2 rational number.
 * Its value is a rational number but not an integer number.
 */
class ACL2Rational extends ACL2Object
{
    final Rational r;
    private static final Map<Rational, ACL2Rational> allNormed = new HashMap<>();

    ACL2Rational(Rational r)
    {
        this(false, r);
    }

    private ACL2Rational(boolean normed, Rational r)
    {
        super(normed);
        if (r.isInteger())
        {
            throw new IllegalArgumentException();
        }
        this.r = r;
    }

    static ACL2Rational intern(Rational r)
    {
        ACL2Rational result = allNormed.get(r);
        if (result == null)
        {
            result = new ACL2Rational(true, r);
            allNormed.put(r, result);
        }
        return result;
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
    ACL2Object internImpl()
    {
        return intern(r);
    }

    @Override
    boolean equalsImpl(ACL2Object o)
    {
        return r.equals(((ACL2Rational)o).r);
    }

    @Override
    public int hashCode()
    {
        return r.hashCode();
    }
}
