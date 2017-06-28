/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Path.java
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
package com.sun.electric.tool.simulation.acl2.mods;

import com.sun.electric.tool.simulation.acl2.svex.SvarImpl;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.List;

/**
 * Type of the names of wires, module instances, and namespaces (such as datatype fields).
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____PATH>.
 */
public abstract class Path
{
    public static ACL2Object KEYWORD_WIRE = ACL2Object.valueOf("KEYWORD", "WIRE");
    public static ACL2Object KEYWORD_SCOPE = ACL2Object.valueOf("KEYWORD", "SCOPE");

    private final ACL2Object impl;

    Path(ACL2Object impl)
    {
        this.impl = impl;
    }

    public ACL2Object getACL2Object()
    {
        return impl;
    }

    public static Path fromACL2(ACL2Object impl)
    {
        if (consp(impl).bool() && !Util.KEYWORD_ANONYMOIUS.equals(car(impl).bool()))
        {
            Name namespace = new Name(car(impl));
            Path scope = fromACL2(cdr(impl));
            return new Scope(namespace, scope);
        } else
        {
            return new Wire(impl);
        }
    }

    public static Path simplePath(Name name)
    {
        return new Wire(name);
    }

    public static Path makePath(List<Name> scopes, Name name)
    {
        Path path = simplePath(name);
        for (int i = scopes.size() - 1; i >= 0; i--)
        {
            path = new Scope(scopes.get(i), path);
        }
        return path;
    }

    public abstract ACL2Object getKind();

    public int getDepth()
    {
        Path path = this;
        int depth = 0;
        while (path instanceof Scope)
        {
            path = ((Scope)path).subpath;
            depth++;
        }
        return depth;
    }

    public abstract Path append(Path y);

    static class Wire extends Path
    {
        final Name name;

        Wire(ACL2Object impl)
        {
            super(impl);
            name = new Name(impl);
        }

        Wire(Name name)
        {
            super(name.getACL2Object());
            this.name = name;
        }

        @Override
        public ACL2Object getKind()
        {
            return KEYWORD_WIRE;
        }

        @Override
        public Path append(Path y)
        {
            return new Path.Scope(name, y);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            return o instanceof Wire && name.equals(((Wire)o).name);
        }

        @Override
        public String toString()
        {
            return name.toString();
        }
    }

    static class Scope extends Path
    {
        final Path subpath;
        final Name namespace;

        Scope(ACL2Object impl)
        {
            super(impl);
            namespace = new Name(car(impl));
            subpath = fromACL2(cdr(impl));
        }

        Scope(Name namespace, Path subpath)
        {
            super(cons(namespace.getACL2Object(), subpath.getACL2Object()));
            this.namespace = namespace;
            this.subpath = subpath;
        }

        @Override
        public ACL2Object getKind()
        {
            return KEYWORD_SCOPE;
        }

        @Override
        public Path append(Path y)
        {
            return new Path.Scope(namespace, subpath.append(y));
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Scope)
            {
                Scope that = (Scope)o;
                return this.namespace.equals(that.namespace)
                    && this.subpath.equals(that.subpath);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 29 * hash + subpath.hashCode();
            hash = 29 * hash + namespace.hashCode();
            return hash;
        }

        @Override
        public String toString()
        {
            return namespace + "." + subpath;
        }
    }

    public static class SvarBuilder extends SvarImpl.Builder<Path>
    {
        @Override
        public Path newName(ACL2Object nameImpl)
        {
            return Path.fromACL2(nameImpl);
        }

        @Override
        public ACL2Object getACL2Object(Path name)
        {
            return name.getACL2Object();
        }
    }
}
