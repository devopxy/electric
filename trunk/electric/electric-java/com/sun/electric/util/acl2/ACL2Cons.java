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
    private final int hashCode;

    ACL2Cons(ACL2Object car, ACL2Object cdr)
    {
        this(null, car, cdr);
    }

    private ACL2Cons(HonsManager hm, ACL2Object car, ACL2Object cdr)
    {
        super(hm);
        this.car = car;
        this.cdr = cdr;
        hashCode = hashCode(car, cdr);
    }

    static ACL2Cons intern(ACL2Object car, ACL2Object cdr, HonsManager hm)
    {
        car = car.intern();
        cdr = cdr.intern();
        Key key = new Key(car, cdr);
        Map<Key, ACL2Cons> allNormed = hm.conses;
        ACL2Cons result = allNormed.get(key);
        if (result == null)
        {
            result = new ACL2Cons(hm, car, cdr);
            allNormed.put(key, result);
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
        return "cons" + len();
    }

    @Override
    public String rep()
    {
        return "(" + car.rep() + " . " + cdr.rep() + ")";
    }

    @Override
    ACL2Object internImpl(HonsManager hm)
    {
        return intern(car, cdr, hm);
    }

    @Override
    boolean equalsImpl(ACL2Object o)
    {
        ACL2Cons x = this;
        ACL2Cons y = (ACL2Cons)o;
        while (x.hashCode == y.hashCode && x.car.equals(y.car))
        {
            if (x.cdr == y.cdr)
            {
                return true;
            }
            if (x.cdr.normed != null && x.cdr.normed == y.cdr.normed)
            {
                return false;
            }
            if (!(x.cdr instanceof ACL2Cons) || !(y.cdr instanceof ACL2Object))
            {
                return x.cdr.equals(y.cdr);
            }
            x = (ACL2Cons)x.cdr;
            y = (ACL2Cons)y.cdr;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    static int hashCode(ACL2Object car, ACL2Object cdr)
    {
        int hash = 7;
        hash = 29 * hash + car.hashCode();
        hash = 29 * hash + cdr.hashCode();
        return hash;
    }

    static class Key
    {
        final ACL2Object car;
        final ACL2Object cdr;

        Key(ACL2Object car, ACL2Object cdr)
        {
            this.car = car;
            this.cdr = cdr;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Key)
            {
                Key that = (Key)o;
                return this.car.equals(that.car) && this.cdr.equals(that.cdr);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return ACL2Cons.hashCode(car, cdr);
        }
    }
}
