/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Character.java
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
 * ACL2 character.
 * This is an atom. Its value is 8-bit ASCII character.
 */
class ACL2Character extends ACL2Object
{

    final char c;

    ACL2Character(char c)
    {
        super(false);
        if (c >= 0x100)
        {
            throw new IllegalArgumentException();
        }
        this.c = c;
    }

    @Override
    public String rep()
    {
        return "#\\" + c;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ACL2Character && c == ((ACL2Character)o).c;
    }

    @Override
    public int hashCode()
    {
        return 73 + c;
    }
}
