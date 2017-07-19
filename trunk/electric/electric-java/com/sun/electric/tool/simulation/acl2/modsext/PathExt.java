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
package com.sun.electric.tool.simulation.acl2.modsext;

import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.mods.Util;
import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

/**
 *
 */
public abstract class PathExt implements SvarName
{
    final Path b;

    final ModuleExt parent;
    public WireExt wire;

    PathExt(ModuleExt parent, Path b)
    {
        this.b = b;
        this.parent = parent;
    }

    @Override
    public ACL2Object getACL2Object()
    {
        return b.getACL2Object();
    }

    @Override
    public String toString()
    {
        return toString(BigIntegerUtil.logheadMask(wire.getWidth()));
    }

    public int getWidth()
    {
        return wire.getWidth();
    }

    public static class PortInst extends PathExt
    {
        public final ModInstExt inst;
//        public final SvarImpl<PathExt> svar;
        Lhs source;
        Object driver;
        WireExt.Bit[] wireBits;

        PortInst(ModInstExt inst, Path.Scope path)
        {
            super(inst.parent, path);
            this.inst = inst;
            Path.Wire pathWire = (Path.Wire)path.subpath;
            wire = inst.proto.wiresIndex.get(pathWire.name);
            wire.exported = true;
        }

        WireExt.Bit getParentBit(int bit)
        {
            return wireBits[bit];
        }

        WireExt.Bit getProtoBit(int bit)
        {
            return wire.getBit(bit);
        }

        void setSource(Lhs<PathExt> source)
        {
            Util.check(driver == null);
            Util.check(this.source == null);
            this.source = source;
            setLhs(source);
//            assert source.width() == wire.getWidth();
//            for (Lhrange<PathExt> lhr : source.ranges)
//            {
//                Svar<PathExt> svar = lhr.getVar();
//                assert svar.getDelay() == 0;
//            }
        }

        void setDriver(DriverExt driver)
        {
            Util.check(source == null);
            Util.check(this.driver == null);
            this.driver = driver;
            wireBits = driver.wireBits.clone();
        }

        void setDriver(Lhs<PathExt> driver)
        {
            Util.check(source == null);
            Util.check(this.driver == null);
            this.driver = driver;
            setLhs((Lhs<PathExt>)driver);
        }

        private void setLhs(Lhs<PathExt> lhs)
        {
            Util.check(wireBits == null);
            Util.check(lhs.width() == getWidth());
            wireBits = new WireExt.Bit[getWidth()];
            int lsh = 0;
            for (Lhrange<PathExt> range : lhs.ranges)
            {
                Svar<PathExt> svar = range.getVar();
                Util.check(svar.getDelay() == 0);
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                for (int i = 0; i < range.getWidth(); i++)
                {
                    wireBits[lsh + i] = lw.wire.getBit(range.getRsh() + i);
                }
                lsh += range.getWidth();
            }
            assert lsh == getWidth();
        }

        DriverExt getDriverExt()
        {
            assert driver instanceof DriverExt;
            return (DriverExt)driver;
        }

        @SuppressWarnings("unchecked")
        Lhs<PathExt> getDriverLhs()
        {
            assert driver instanceof Lhs;
            return (Lhs<PathExt>)driver;
        }

        @Override
        public String toString(BigInteger mask)
        {
            return inst.getInstname() + "." + wire.toString(mask);
        }
    }

    public static class LocalWire extends PathExt
    {
        public final Name name;

        LocalWire(WireExt wire)
        {
            super(wire.parent, new Path.Wire(wire.getName()));
            this.name = wire.getName();
            this.wire = wire;
        }

        public void markUsed()
        {
            wire.used = true;
        }

        @Override
        public String toString(BigInteger mask)
        {
            return wire.toString(mask);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof LocalWire)
            {
                LocalWire that = (LocalWire)o;
                return this.name.equals(that.name)
                    && this.parent == that.parent;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    }

}
