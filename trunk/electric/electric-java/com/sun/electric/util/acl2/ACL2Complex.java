/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Complex.java
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
 * ACL2 complex-rational number.
 * Its value is a complex number with rational real part and rational nonzero imaginary part.
 */
class ACL2Complex extends ACL2Object
{

    final Rational re;
    final Rational im;

    ACL2Complex(Rational re, Rational im)
    {
        super(false);
        if (im.signum() == 0)
        {
            throw new IllegalArgumentException();
        }
        this.re = re;
        this.im = im;
    }

    @Override
    boolean isACL2Number()
    {
        return true;
    }

    @Override
    ACL2Object unaryMinus()
    {
        return new ACL2Complex(re.negate(), im.negate());
    }

    @Override
    ACL2Object unarySlash()
    {
        Rational sqrInv = re.mul(re).add(im.mul(im)).inverse();
        return new ACL2Complex(re.mul(sqrInv), im.negate().mul(sqrInv));
    }

    @Override
    ACL2Object binaryPlus(ACL2Object y)
    {
        return y.binaryPlus(this);
    }

    @Override
    ACL2Object binaryPlus(ACL2Integer y)
    {
        return new ACL2Complex(re.add(y.v), im);
    }

    @Override
    ACL2Object binaryPlus(ACL2Rational y)
    {
        return new ACL2Complex(re.add(y.r), im);
    }

    @Override
    ACL2Object binaryPlus(ACL2Complex y)
    {
        return valueOf(re.add(y.re), im.add(y.im));
    }

    @Override
    ACL2Object binaryStar(ACL2Object y)
    {
        return y.binaryStar(this);
    }

    @Override
    ACL2Object binaryStar(ACL2Integer y)
    {
        return new ACL2Complex(re.mul(y.v), im.mul(y.v));
    }

    @Override
    ACL2Object binaryStar(ACL2Rational y)
    {
        return new ACL2Complex(re.mul(y.r), im.mul(y.r));
    }

    @Override
    ACL2Object binaryStar(ACL2Complex y)
    {
        Rational zre = re.mul(y.re).add(im.mul(y.im).negate());
        Rational zim = re.mul(y.im).add(im.mul(y.re));
        return valueOf(zre, zim);
    }

    @Override
    int signum()
    {
        int rsig = re.signum();
        return rsig != 0 ? rsig : im.signum();
    }

    @Override
    int compareTo(ACL2Object y)
    {
        return -y.compareTo(this);
    }

    @Override
    int compareTo(ACL2Integer y)
    {
        int resig = re.compareTo(y.v);
        return resig != 0 ? resig : im.signum();
    }

    @Override
    int compareTo(ACL2Rational y)
    {
        int resig = re.compareTo(y.r);
        return resig != 0 ? resig : im.signum();
    }

    @Override
    int compareTo(ACL2Complex y)
    {
        int resig = re.compareTo(y.re);
        return resig != 0 ? resig : im.compareTo(y.im);
    }

    @Override
    public String rep()
    {
        return "#c(" + re.toString() + "," + im + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ACL2Complex
            && re.equals(((ACL2Complex)o).re)
            && im.equals(((ACL2Complex)o).im);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 59 * hash + re.hashCode();
        hash = 59 * hash + im.hashCode();
        return hash;
    }
}
