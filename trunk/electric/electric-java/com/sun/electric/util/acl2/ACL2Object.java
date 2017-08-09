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
    final HonsManager normed;

    ACL2Object(HonsManager normed)
    {
        this.normed = normed;
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

    static ACL2Object valueOf(Rational r)
    {
        return r.isInteger() ? new ACL2Integer(r.n) : new ACL2Rational(r);
    }

    static ACL2Object valueOf(Complex c)
    {
        return c.isRational() ? valueOf(c.re) : new ACL2Complex(c);
    }

    public static ACL2Object valueOf(String s)
    {
        return new ACL2String(s);
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

    static ACL2Integer zero()
    {
        HonsManager hm = HonsManager.current.get();
        if (hm == null)
        {
            hm = HonsManager.DUMMY;
        }
        return hm.ZERO;
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

    static ACL2String emptyStr()
    {
        HonsManager hm = HonsManager.current.get();
        if (hm == null)
        {
            hm = HonsManager.DUMMY;
        }
        return hm.EMPTY_STR;
    }

    public abstract String rep();

    public boolean isNormed()
    {
        return normed != null;
    }

    public ACL2Object intern()
    {
        HonsManager hm = HonsManager.current.get();
        return normed == hm ? this : internImpl(hm);
    }

    ACL2Object internImpl(HonsManager hm)
    {
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        if (o != null && o.getClass() == getClass())
        {
            ACL2Object that = (ACL2Object)o;
            if (this.normed != null && that.normed == this.normed)
            {
                return false;
            }
            return equalsImpl(that);
        }
        return false;
    }

    boolean equalsImpl(ACL2Object that)
    {
        throw new AssertionError(); // should not be called
    }

    @Override
    public String toString()
    {
        return rep();
    }

    public static void initHonsMananger(String name)
    {
        HonsManager.init(name);
    }

    public static void closeHonsManager()
    {
        HonsManager.close();
    }
}
