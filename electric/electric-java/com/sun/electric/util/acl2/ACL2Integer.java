/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Integer.java
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
 * ACL2 integer number.
 */
public class ACL2Integer extends ACL2Number
{

    public final BigInteger v;

    ACL2Integer(int id, BigInteger v)
    {
        super(id);
        this.v = v;
    }

    @Override
    public ACL2Integer asInt()
    {
        return this;
    }

    @Override
    public String rep()
    {
        return v.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ACL2Integer && v.equals(((ACL2Integer)o).v);
    }

    @Override
    public int hashCode()
    {
        return v.hashCode();
    }
}
