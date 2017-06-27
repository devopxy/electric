/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Lhrange.java
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

import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Concat;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.Map;

/**
 * An atom with width from left-hand side of SVEX assignment.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____LHRANGE>.
 *
 * @param <V> Type of Svex variables
 */
public class Lhrange<V extends Svar>
{
    private final int w;
    private final Lhatom<V> atom;

    Lhrange(Svar.Builder<V> builder, ACL2Object impl, int lsh)
    {
        if (consp(impl).bool())
        {
            if (integerp(car(impl)).bool())
            {
                w = car(impl).intValueExact();
                atom = Lhatom.valueOf(builder, cdr(impl));
            } else
            {
                w = 1;
                atom = Lhatom.valueOf(builder, impl);
            }
        } else
        {
            w = 1;
            atom = Lhatom.valueOf(builder, impl);
        }
        Util.check(w >= 1);
    }

    public Lhrange(int w, Lhatom<V> atom)
    {
        this.w = w;
        this.atom = atom;
    }

    public ACL2Object getACL2Object()
    {
        ACL2Object atomImpl = atom.getACL2Object();
        return w == 1 ? atomImpl : cons(ACL2Object.valueOf(w), atomImpl);
    }

    public <V1 extends Svar> Lhrange<V1> convertVars(Svar.Builder<V1> builder)
    {
        return new Lhrange<>(w, atom.convertVars(builder));
    }

    public Vec4 eval(Map<V, Vec4> env)
    {
        return Vec4Concat.FUNCTION.apply(new Vec2(BigInteger.valueOf(w)), atom.eval(env), Vec4.Z);
    }

    public Svex<V> toSvex()
    {
        Svex<V>[] args = Svex.newSvexArray(3);
        args[0] = new SvexQuote<>(new Vec2(BigInteger.valueOf(w)));
        args[1] = atom.toSvex();
        args[2] = new SvexQuote<>(Vec4.Z);
        return Vec4Concat.FUNCTION.build(args);
    }

    public Lhatom<V> nextbit()
    {
        if (atom instanceof Lhatom.Var)
        {
            return new Lhatom.Var<>(atom.getVar(), w + atom.getRsh());
        } else
        {
            return atom;
        }
    }

    public boolean combinable(Lhatom<V> y)
    {
        V vx = getVar();
        V vy = y.getVar();
        if (vx == null)
        {
            return vy == null;
        } else
        {
            return vx.equals(vy) && y.getRsh() == getRsh() + w;
        }
    }

    public Lhrange<V> combine(Lhrange<V> y)
    {
        V vx = getVar();
        V vy = y.getVar();
        if (vx == null)
        {
            if (vy == null)
            {
                return new Lhrange<>(w + y.w, atom);
            }
        } else if (vx.equals(vy) && y.getRsh() == getRsh() + w)
        {
            return new Lhrange<>(w + y.w, atom);
        }
        return null;
    }

    public Lhatom<V> getAtom()
    {
        return atom;
    }

    public V getVar()
    {
        return atom.getVar();
    }

    public int getWidth()
    {
        return w;
    }

    public int getRsh()
    {
        return atom.getRsh();
    }

    @Override
    public String toString()
    {
        V name = getVar();
        if (name != null)
        {
            return name.toString(BigIntegerUtil.logheadMask(getWidth()).shiftLeft(getRsh()));
        } else
        {
            return w + "'Z";
        }
    }
}
