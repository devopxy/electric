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

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

import java.util.Map;

/**
 * An SVar or X at left-hand side of SVEX assignment.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHATOM>.
 */
public abstract class Lhatom
{

    final ACL2Object impl;

    Lhatom(ACL2Object impl)
    {
        this.impl = impl;
    }

    public static Lhatom valueOf(Module parent, ACL2Object impl)
    {
        if (symbolp(impl).bool())
        {
            if (impl.equals(Util.KEYWORD_Z))
            {
                return new Z(impl);
            }
        }
        return new Var(parent, impl);
    }

    public abstract void check(Map<ModName, Module> modalist, boolean assign);

    public abstract String toString(int w);

    public abstract String toLispString(int w);

    public static class Z extends Lhatom
    {

        Z(ACL2Object impl)
        {
            super(impl);
        }

        @Override
        public void check(Map<ModName, Module> modalist, boolean assign)
        {
            Util.check(!assign);
        }

        @Override
        public String toString(int w)
        {
            return "Z";
        }

        @Override
        public String toLispString(int w)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class Var extends Lhatom
    {

        public final SVarExt name;
        public final int rsh;

        Var(Module parent, ACL2Object impl)
        {
            super(impl);
            if (consp(impl).bool())
            {
                if (car(impl).equals(Util.KEYWORD_VAR) && consp(cdr(impl)).bool())
                {
                    name = parent.fromACL2(impl);
                    rsh = 0;
                } else
                {
                    name = parent.fromACL2(car(impl));
                    rsh = cdr(impl).intValueExact();
                    Util.check(rsh >= 0);
                }
            } else
            {
                name = parent.fromACL2(impl);
                rsh = 0;
            }
        }

        @Override
        public void check(Map<ModName, Module> modalist, boolean assign)
        {
            name.check(modalist, assign);
        }

        @Override
        public String toString(int w)
        {
            return name.toString(w, rsh);
        }

        @Override
        public String toLispString(int w)
        {
            return name.toLispString(w, rsh);
        }
    }
}
