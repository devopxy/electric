/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvarExt.java
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
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

/**
 * SVAR extended by parent, instance and wire.
 */
public abstract class SVarExt extends Svar
{

    final ModuleExt parent;

    public WireExt wire;

    SVarExt(ModuleExt parent)
    {
        this.parent = parent;
    }

    @Override
    public boolean isNonblocking()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return toString(BigIntegerUtil.logheadMask(wire.getWidth()));
    }

    public String toLispString(int width, int rsh)
    {
        return toString(BigIntegerUtil.logheadMask(width).shiftLeft(rsh));
    }

    public static class PortInst extends SVarExt
    {
        public final ModInstExt inst;
        Lhs source;
        Object driver;

        public PortInst(ModInstExt inst, WireExt wire)
        {
            super(inst.parent);
            this.inst = inst;
            ModuleExt sm = inst.proto;
            this.wire = wire;
            wire.exported = true;
        }

        @Override
        public ACL2Object getACL2Name()
        {
            return cons(inst.getInstname().getACL2Object(), wire.getName().getACL2Object());
        }

        void addSource(Lhs<SVarExt> source)
        {
            Util.check(this.source == null);
            this.source = source;
            assert source.width() == wire.getWidth();
            for (Lhrange<SVarExt> lhr : source.ranges)
            {
                SVarExt svar = lhr.getVar();
                assert svar.getDelay() == 0;
            }
        }

        void addDriver(Object driver)
        {
            Util.check(this.driver == null);
            Util.check(driver instanceof DriverExt || driver instanceof Lhs);
            this.driver = driver;
        }

        DriverExt getDriverExt()
        {
            assert driver instanceof DriverExt;
            return (DriverExt)driver;
        }

        @SuppressWarnings("unchecked")
        Lhs<SVarExt> getDriverLhs()
        {
            assert driver instanceof Lhs;
            return (Lhs<SVarExt>)driver;
        }

        @Override
        public int getDelay()
        {
            return 0;
        }

        @Override
        public String toString(BigInteger mask)
        {
            return inst.getInstname() + "." + wire.toString(mask);
        }

    }

    public static class LocalWire extends SVarExt
    {
        public final Name name;
        public final int delay;

        LocalWire(WireExt wire, int delay)
        {
            super(wire.parent);
            this.name = wire.getName();
            this.wire = wire;
            this.delay = delay;
        }

        @Override
        public ACL2Object getACL2Name()
        {
            return name.getACL2Object();
        }

        @Override
        public int getDelay()
        {
            return delay;
        }

        public void markUsed()
        {
            wire.used = true;
        }

        @Override
        public String toString(BigInteger mask)
        {
            String s = wire.toString(mask);
            if (delay != 0)
            {
                s = "#" + delay + " " + s;
            }
            return s;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof LocalWire)
            {
                LocalWire that = (LocalWire)o;
                return this.name.equals(that.name)
                    && this.delay == that.delay
                    && this.parent == that.parent;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = name.hashCode();
            hash = 43 * hash + this.delay;
            return hash;
        }
    }
}
