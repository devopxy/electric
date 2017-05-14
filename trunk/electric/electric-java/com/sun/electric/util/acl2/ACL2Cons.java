/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Cons.java
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
 * Non-leaf node of ACL2 object.
 * Often a ACL2 object are used to represent a list.
 * In this case {@link #car} is the first element
 * and {@link #cdr} is the tail.
 */
class ACL2Cons extends ACL2Object
{

    /**
     * The left son.
     */
    final ACL2Object car;
    /**
     * The right sun.
     */
    final ACL2Object cdr;
    private int hashCode;

    ACL2Cons(boolean norm, ACL2Object car, ACL2Object cdr)
    {
        super(norm);
        if (car == null || cdr == null)
        {
            throw new NullPointerException();
        }
        this.car = car;
        this.cdr = cdr;
    }

    @Override
    int len()
    {
        ACL2Object o = this;
        int n = 0;
        while (o instanceof ACL2Cons)
        {
            n++;
            o = ((ACL2Cons)o).cdr;
        }
        return n;
    }

    @Override
    public String toString()
    {
        return id() + "!" + len();
    }

    @Override
    public String rep()
    {
        return "(" + car.rep() + " . " + cdr.rep() + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ACL2Cons)
        {
            ACL2Cons that = (ACL2Cons)o;
            if (this.hashCode() != that.hashCode())
            {
                return false;
            }
            return this == that
                || car.equals(that.car) && this.cdr.equals(that.cdr);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = hashCode;
        if (hash == 0)
        {
            hash = 7;
            hash = 29 * hash + car.hashCode();
            hash = 29 * hash + cdr.hashCode();
            hashCode = hash;
        }
        return hash;
    }
}
