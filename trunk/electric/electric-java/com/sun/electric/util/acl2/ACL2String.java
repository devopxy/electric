/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2String.java
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
 * ACL2 string.
 * This is an atom. Its value is 8-bit ASCII string.
 */
class ACL2String extends ACL2Object
{

    public final String s;

    static final ACL2String EMPTY = new ACL2String(false, "");

    ACL2String(boolean norm, String s)
    {
        super(norm);
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) >= 0x100)
            {
                throw new IllegalArgumentException();
            }
        }
        this.s = s;
    }

    @Override
    public String stringValueExact()
    {
        return s;
    }

    @Override
    public String rep()
    {
        return "\"" + s + "\"";
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ACL2String && s.equals(((ACL2String)o).s);
    }

    @Override
    public int hashCode()
    {
        return s.hashCode();
    }

}
