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

import java.util.HashMap;
import java.util.Map;

/**
 * ACL2 symbol.
 * This is an atom. It has package name and symbol name
 * which are nonempty ACII strings.
 */
class ACL2Symbol extends ACL2Object
{

    public final String nm;
    private final Package pkg;

    private static Map<String, Package> knownPackages = new HashMap<>();

    static final ACL2Symbol NIL = valueOf("COMMON-LISP", "NIL");
    static final ACL2Symbol T = valueOf("COMMON-LISP", "T");

    private ACL2Symbol(Package pkg, String nm)
    {
        super(true);
        if (nm.isEmpty())
        {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < nm.length(); i++)
        {
            if (nm.charAt(i) >= 0x100)
            {
                throw new IllegalArgumentException();
            }
        }
        this.pkg = pkg;
        this.nm = nm;
    }

    public static ACL2Symbol valueOf(String pk, String nm)
    {
        return getPackage(pk).getSymbol(nm);
    }

    public String getPkgName()
    {
        return pkg.name;
    }

    @Override
    public String rep()
    {
        return pkg.name + "::" + nm;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 97 * hash + nm.hashCode();
        hash = 97 * hash + pkg.hashCode();
        return hash;
    }

    static class Package
    {
        private final String name;
        private final Map<String, ACL2Symbol> symbols = new HashMap<>();

        Package(String name)
        {
            if (name.isEmpty())
            {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < name.length(); i++)
            {
                if (name.charAt(i) >= 0x100)
                {
                    throw new IllegalArgumentException();
                }
            }
            this.name = name;
        }

        ACL2Symbol getSymbol(String symName)
        {
            ACL2Symbol sym = symbols.get(symName);
            if (sym == null)
            {
                sym = new ACL2Symbol(this, symName);
                symbols.put(symName, sym);
            }
            return sym;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    }

    static Package getPackage(String pkgName)
    {
        Package pkg = knownPackages.get(pkgName);
        if (pkg == null)
        {
            pkg = new Package(pkgName);
            knownPackages.put(pkgName, pkg);
        }
        return pkg;
    }

}
