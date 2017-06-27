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

import java.util.HashMap;
import java.util.Map;

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
     * The right son.
     */
    final ACL2Object cdr;
    private int hashCode;
    private static final Map<ACL2Cons, ACL2Cons> allNormed = new HashMap<>();

    ACL2Cons(ACL2Object car, ACL2Object cdr)
    {
        this(false, car, cdr);
    }

    private ACL2Cons(boolean norm, ACL2Object car, ACL2Object cdr)
    {
        super(norm);
        if (car == null || cdr == null)
        {
            throw new NullPointerException();
        }
        this.car = car;
        this.cdr = cdr;
    }

    static ACL2Cons intern(ACL2Object car, ACL2Object cdr)
    {
        car = car.intern();
        cdr = cdr.intern();
        ACL2Cons v = new ACL2Cons(false, car, cdr);
        ACL2Cons result = allNormed.get(v);
        if (result == null)
        {
            result = new ACL2Cons(true, car, cdr);
            allNormed.put(v, result);
        }
        return result;
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
    ACL2Object internImpl()
    {
        return intern(car, cdr);
    }

    @Override
    boolean equalsImpl(ACL2Object o)
    {
        ACL2Cons that = (ACL2Cons)o;
        return this.hashCode() == that.hashCode()
            && this.car.equals(that.car)
            && this.cdr.equals(that.cdr);
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
