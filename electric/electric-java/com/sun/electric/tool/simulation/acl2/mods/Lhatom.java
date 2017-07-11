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
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.SvexVar;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Rsh;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.Map;
import java.util.Objects;

/**
 * An SVar or X at left-hand side of SVEX assignment.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHATOM>.
 *
 * @param <N> Type of name of Svex Variables
 */
public abstract class Lhatom<N extends SvarName>
{
    public abstract ACL2Object getACL2Object();

    public abstract Svar<N> getVar();

    public abstract int getRsh();

    public abstract <N1 extends SvarName> Lhatom<N1> convertVars(Svar.Builder<N1> builder);

    public abstract Vec4 eval(Map<Svar<N>, Vec4> env);

    public abstract Svex<N> toSvex();

    @Override
    public String toString()
    {
        return getACL2Object().rep();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Lhatom)
        {
            Lhatom<?> that = (Lhatom<?>)o;
            return Objects.equals(this.getVar(), that.getVar())
                && this.getRsh() == that.getRsh();
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(getVar()) * 13 + getRsh();
    }

    public static <N extends SvarName> Lhatom<N> valueOf(Svar.Builder<N> builder, ACL2Object impl)
    {
        if (symbolp(impl).bool())
        {
            if (impl.equals(Util.KEYWORD_Z))
            {
                return new Lhatom.Z<>();
            }
        }
        return new Lhatom.Var<>(builder, impl);
    }

    public static class Z<N extends SvarName> extends Lhatom<N>
    {
        @Override
        public ACL2Object getACL2Object()
        {
            return Util.KEYWORD_Z;
        }

        @Override
        public Svar<N> getVar()
        {
            return null;
        }

        @Override
        public int getRsh()
        {
            return 0;
        }

        @Override
        public <N1 extends SvarName> Lhatom<N1> convertVars(Svar.Builder<N1> builder)
        {
            return new Z<>();
        }

        @Override
        public Vec4 eval(Map<Svar<N>, Vec4> env)
        {
            return Vec4.Z;
        }

        @Override
        public Svex<N> toSvex()
        {
            return new SvexQuote<>(Vec4.Z);
        }
    }

    public static class Var<N extends SvarName> extends Lhatom<N>
    {
        public final Svar<N> name;
        public final int rsh;

        Var(Svar.Builder<N> builder, ACL2Object impl)
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

        public Var(Svar<N> name, int rsh)
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
        public Svar<N> getVar()
        {
            return name;
        }

        @Override
        public int getRsh()
        {
            return rsh;
        }

        @Override
        public <N1 extends SvarName> Lhatom<N1> convertVars(Svar.Builder<N1> builder)
        {
            Svar<N1> newName = builder.newVar(name);
            return new Var<>(newName, rsh);
        }

        @Override
        public Vec4 eval(Map<Svar<N>, Vec4> env)
        {
            Vec4 sh = new Vec2(rsh);
            Vec4 x = env.getOrDefault(name, Vec4.X);
            return Vec4Rsh.FUNCTION.apply(sh, x);
        }

        @Override
        public Svex<N> toSvex()
        {
            Svex<N> svexVar = new SvexVar<>(name);
            return rsh != 0 ? svexVar.rsh(rsh) : svexVar;
        }
    }
}
