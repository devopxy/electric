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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ACL2 symbol.
 * This is an atom. It has package name and symbol name
 * which are ACII strings.
 */
class ACL2Symbol extends ACL2Object
{

    public final String nm;
    private final Package pkg;

    private static Map<String, Package> knownPackages = new HashMap<>();

    static
    {
        readPkgImports();
    }
    static final ACL2Symbol NIL = valueOf("COMMON-LISP", "NIL");
    static final ACL2Symbol T = valueOf("COMMON-LISP", "T");

    private static void readPkgImports()
    {
        try (LineNumberReader in = new LineNumberReader(
            new InputStreamReader(ACL2Symbol.class.getResourceAsStream("pkg-imports.dat"))))
        {
            String line;
            String curPkgName = null;
            Set<ACL2Symbol> curImports = null;
            while ((line = in.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == ';')
                {
                    continue;
                }
                switch (line.charAt(0))
                {
                    case '+':
                    case '-':
                        int ind;
                        String pkgName;
                        if (line.charAt(1) == '|') {
                            ind = line.indexOf(2, '|');
                            pkgName = line.substring(2, ind);
                            ind++;
                        } else {
                            ind = line.indexOf(':');
                            pkgName = line.substring(1, ind);
                        }
                        Package pkg = knownPackages.get(pkgName);
                        assert line.charAt(ind) == ':';
                        String symName = unquote(line.substring(ind + 1));
                        if (line.charAt(0) == '-') {
                            boolean ok = curImports.remove(pkg.symbols.get(symName));
                            assert ok;
                        } else {
                            assert !pkg.imports.containsKey(symName);
                            boolean ok = curImports.add(pkg.getSymbol(symName));
                            assert ok;
                        }
                        break;
                    default:
                        if (curPkgName != null)
                        {
                            Package newPackage = new Package(curPkgName, curImports);
                            Package old = knownPackages.put(curPkgName, newPackage);
                            assert old == null;
                        }
                        if (line.charAt(0) == '|')
                        {
                            ind = line.indexOf('|', 1);
                            curPkgName = line.substring(1, ind);
                            ind++;
                        } else
                        {
                            ind = line.indexOf(' ');
                            if (ind < 0) {
                                ind = line.length();
                            }
                            curPkgName = line.substring(0, ind);
                        }
                        while (ind < line.length() && line.charAt(ind) == ' ')
                        {
                            ind++;
                        }
                        if (ind == line.length())
                        {
                            curImports = new HashSet<>();
                        } else
                        {
                            String oldPkg = unquote(line.substring(ind));
                            curImports = new HashSet<>(knownPackages.get(oldPkg).imports.values());
                        }
                }
            };
            if (curPkgName != null)
            {
                Package newPackage = new Package(curPkgName, curImports);
                Package old = knownPackages.put(curPkgName, newPackage);
                assert old == null;
            }
        } catch (IOException e)
        {
        }
    }

    private static String unquote(String s)
    {
        if (s.charAt(0) == '|')
        {
            if (s.charAt(s.length() - 1) != '|')
            {
                return null;
            }
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private ACL2Symbol(Package pkg, String nm)
    {
        super(true);
//        if (nm.isEmpty())
//        {
//            throw new IllegalArgumentException();
//        }
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
        private final Map<String, ACL2Symbol> imports = new HashMap<>();
        private final Map<String, ACL2Symbol> symbols = new HashMap<>();

        Package(String name, Set<ACL2Symbol> importsSyms)
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
            for (ACL2Symbol impSym : importsSyms)
            {
                ACL2Symbol old = imports.put(impSym.nm, impSym);
                assert old == null;
            }
        }

        ACL2Symbol getSymbol(String symName)
        {
            ACL2Symbol sym = imports.get(symName);
            if (sym != null)
            {
                return sym;
            }
            sym = symbols.get(symName);
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
            pkg = new Package(pkgName, Collections.<ACL2Symbol>emptySet());
            knownPackages.put(pkgName, pkg);
        }
        return pkg;
    }

}