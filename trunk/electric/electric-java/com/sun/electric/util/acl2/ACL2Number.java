/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2Number.java
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
 * ACL2 number.
 * It is an atom. Its value is a complex number with rational real and imaginary parts.
 * This is abstract type. Its concrete subclasses are
 * {@link com.sun.electric.util.acl2.ACL2Integer}
 * {@link com.sun.electric.util.acl2.ACL2Rational}
 * {@link com.sun.electric.util.acl2.ACL2Complex}
 */
public abstract class ACL2Number extends ACL2Atom
{

    ACL2Number(int id)
    {
        super(id, false);
    }
}
