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

import java.util.ArrayList;
import java.util.List;

/**
 * ACL2 Object is a binary tree.
 * Its leafs are atoms represented by {@link com.sun.electric.util.acl2.ACL2Atom}.
 * Its non-leaf nodes are conses representred by {@link com.sun.electric.util.acl2.ACL2Atom}.
 */
public abstract class ACL2Object
{

    public static void check(boolean p)
    {
        if (!p)
        {
            throw new RuntimeException();
        }
    }

    public int id;
    public boolean norm;

    ACL2Object(int id, boolean norm)
    {
        this.id = id;
        this.norm = norm;
    }

    public int len()
    {
        return 0;
    }

    public ACL2Integer asInt()
    {
        check(false);
        return null;
    }

    public ACL2Rational asRat()
    {
        check(false);
        return null;
    }

    public ACL2Symbol asSym()
    {
        check(false);
        return null;
    }

    public ACL2String asStr()
    {
        check(false);
        return null;
    }

    public ACL2Cons asCons()
    {
        check(false);
        return null;
    }

    public static void checkSym(ACL2Object x, String pk, String nm)
    {
        ACL2Symbol sym = x.asSym();
        ACL2Object.check(sym.pk.equals(pk));
        ACL2Object.check(sym.nm.equals(nm));
    }

    public static void checkNil(ACL2Object x)
    {
        checkSym(x, "COMMON-LISP", "NIL");
    }

    public static void checkT(ACL2Object x)
    {
        checkSym(x, "COMMON-LISP", "T");
    }

    public static List<ACL2Object> getList(ACL2Object l, boolean trueList)
    {
        List<ACL2Object> result = new ArrayList<>();
        while (l instanceof ACL2Cons)
        {
            result.add(((ACL2Cons)l).car);
            l = ((ACL2Cons)l).cdr;
        }
        if (trueList)
        {
            checkNil(l);
        }
        return result;
    }

    public abstract String rep();

    @Override
    public String toString()
    {
        return id + ":" + rep();
    }
}
