/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Object.java
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
 * ACL2 Object is a binary tree.
 * Its non-leaf nodes are conses. They are represented by {@link com.sun.electric.util.acl2.ACL2Atom}.
 * Its leafs are atoms represented by concrete subclasses:
 * {@link com.sun.electric.util.acl2.ACL2Integer}
 * {@link com.sun.electric.util.acl2.ACL2Rational}
 * {@link com.sun.electric.util.acl2.ACL2Complex}
 * {@link com.sun.electric.util.acl2.ACL2Character},
 * {@link com.sun.electric.util.acl2.ACL2String},
 * {@link com.sun.electric.util.acl2.ACL2Symbol},
 */
public abstract class ACL2Object
{

    private static long nextId = 0;
    private long id = -1;
    private boolean norm;

    ACL2Object(boolean norm)
    {
        this.norm = norm;
    }

    public static ACL2Object valueOf(BigInteger v)
    {
        return new ACL2Integer(v);
    }

    public static ACL2Object valueOf(long v)
    {
        return new ACL2Integer(BigInteger.valueOf(v));
    }

    public static ACL2Object valueOf(int v)
    {
        return new ACL2Integer(BigInteger.valueOf(v));
    }

    static ACL2Object zero()
    {
        return new ACL2Integer(BigInteger.ZERO);
    }

    static ACL2Object valueOf(Rational r)
    {
        return r.isInteger() ? new ACL2Integer(r.n) : new ACL2Rational(r);
    }

    static ACL2Object valueOf(Rational re, Rational im)
    {
        return im.signum() == 0 ? valueOf(re) : new ACL2Complex(re, im);
    }

    public static ACL2Object valueOf(String pk, String nm)
    {
        return ACL2Symbol.getPackage(pk).getSymbol(nm);
    }

    public static ACL2Object valueOf(boolean v)
    {
        return v ? ACL2Symbol.T : ACL2Symbol.NIL;
    }

    public boolean bool()
    {
        return !ACL2Symbol.NIL.equals(this);
    }

    public int intValueExact()
    {
        throw new ArithmeticException();
    }

    public long longValueExact()
    {
        throw new ArithmeticException();
    }

    public BigInteger bigIntegerValueExact()
    {
        throw new ArithmeticException();
    }

    public String stringValueExact()
    {
        throw new ArithmeticException();
    }

    int len()
    {
        return 0;
    }

    boolean isACL2Number()
    {
        return false;
    }

    ACL2Object fix()
    {
        return isACL2Number() ? this : zero();
    }

    Rational ratfix()
    {
        return Rational.valueOf(BigInteger.ZERO, BigInteger.ONE);
    }

    ACL2Object unaryMinus()
    {
        return zero();
    }

    ACL2Object unarySlash()
    {
        return zero();
    }

    ACL2Object binaryPlus(ACL2Object y)
    {
        return y.fix();
    }

    ACL2Object binaryPlus(ACL2Integer y)
    {
        return y;
    }

    ACL2Object binaryPlus(ACL2Rational y)
    {
        return y;
    }

    ACL2Object binaryPlus(ACL2Complex y)
    {
        return y;
    }

    ACL2Object binaryStar(ACL2Object y)
    {
        return y.fix();
    }

    ACL2Object binaryStar(ACL2Integer y)
    {
        return y;
    }

    ACL2Object binaryStar(ACL2Rational y)
    {
        return y;
    }

    ACL2Object binaryStar(ACL2Complex y)
    {
        return y;
    }

    int signum()
    {
        return 0;
    }

    int compareTo(ACL2Object y)
    {
        return -y.signum();
    }

    int compareTo(ACL2Integer y)
    {
        return -y.signum();
    }

    int compareTo(ACL2Rational y)
    {
        return -y.signum();
    }

    int compareTo(ACL2Complex y)
    {
        return -y.signum();
    }

    public abstract String rep();

    public long id()
    {
        if (id >= 0)
        {
            return id;
        } else
        {
            return id = nextId++;
        }
    }

    @Override
    public String toString()
    {
        return id() + ":" + rep();
    }
}
