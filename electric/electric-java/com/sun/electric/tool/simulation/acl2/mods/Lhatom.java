/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Lhatom.java
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

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

/**
 * An SVar or X at left-hand side of SVEX assignment.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHATOM>.
 *
 * @param <V> Type of Svex Variables
 */
public abstract class Lhatom<V extends Svar>
{
    public abstract ACL2Object getACL2Object();

    public abstract V getVar();

    public abstract int getRsh();

    public abstract <V1 extends Svar> Lhatom<V1> convertVars(Svar.Builder<V1> builder);

    @Override
    public String toString()
    {
        return getACL2Object().rep();
    }

    public static <V extends Svar> Lhatom<V> valueOf(Svar.Builder<V> builder, ACL2Object impl)
    {
        if (symbolp(impl).bool())
        {
            if (impl.equals(Util.KEYWORD_Z))
            {
                return new Lhatom.Z();
            }
        }
        return new Lhatom.Var<>(builder, impl);
    }

    public static class Z<V extends Svar> extends Lhatom<V>
    {
        @Override
        public ACL2Object getACL2Object()
        {
            return Util.KEYWORD_Z;
        }

        @Override
        public V getVar()
        {
            return null;
        }

        @Override
        public int getRsh()
        {
            return 0;
        }

        @Override
        public <V1 extends Svar> Lhatom<V1> convertVars(Svar.Builder<V1> builder)
        {
            return new Z<>();
        }

    }

    public static class Var<V extends Svar> extends Lhatom<V>
    {
        public final V name;
        public final int rsh;

        Var(Svar.Builder<V> builder, ACL2Object impl)
        {
            if (consp(impl).bool())
            {
                if (car(impl).equals(Util.KEYWORD_VAR) && consp(cdr(impl)).bool())
                {
                    name = builder.fromACL2(impl);
                    rsh = 0;
                } else
                {
                    name = builder.fromACL2(car(impl));
                    rsh = cdr(impl).intValueExact();
                    Util.check(rsh >= 0);
                }
            } else
            {
                name = builder.fromACL2(impl);
                rsh = 0;
            }
        }

        Var(V name, int rsh)
        {
            if (name == null)
            {
                throw new NullPointerException();
            }
            if (rsh < 0)
            {
                throw new IllegalArgumentException();
            }
            this.name = name;
            this.rsh = rsh;
        }

        @Override
        public ACL2Object getACL2Object()
        {
            ACL2Object nameRep = name.makeACL2Object();
            return rsh == 0 && !Util.KEYWORD_Z.equals(nameRep)
                ? nameRep
                : cons(nameRep, ACL2Object.valueOf(rsh));
        }

        @Override
        public V getVar()
        {
            return name;
        }

        @Override
        public int getRsh()
        {
            return rsh;
        }

        @Override
        public <V1 extends Svar> Lhatom<V1> convertVars(Svar.Builder<V1> builder)
        {
            V1 newName = builder.newVar(name);
            return new Var<>(newName, rsh);
        }
    }
}
