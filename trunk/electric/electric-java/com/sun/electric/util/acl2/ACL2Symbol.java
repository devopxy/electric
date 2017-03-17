/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Symbol.java
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
 * ACL2 symbol.
 * This is an atom. It has package name and symbol name
 * which are nonempty ACII strings.
 */
public class ACL2Symbol extends ACL2Atom
{

    public final String pk;
    public final String nm;

    ACL2Symbol(int id, String pk, String nm)
    {
        super(id, true);
        for (int i = 0; i < pk.length(); i++)
        {
            if (pk.charAt(i) >= 0x100)
            {
                throw new IllegalArgumentException();
            }
        }
        for (int i = 0; i < nm.length(); i++)
        {
            if (nm.charAt(i) >= 0x100)
            {
                throw new IllegalArgumentException();
            }
        }
        if (pk.isEmpty() || nm.isEmpty())
        {
            throw new IllegalArgumentException();
        }
        this.pk = pk;
        this.nm = nm;
    }

    @Override
    public ACL2Symbol asSym()
    {
        return this;
    }

    @Override
    public String rep()
    {
        return pk + "::" + nm;
    }
}
