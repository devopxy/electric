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
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public abstract class PathExt implements SvarName
{
    final Path b;

    final ModuleExt parent;
    public final WireExt wire;
    private final Bit[] bits;

    PathExt(ModuleExt parent, Path b, WireExt wire)
    {
        this.b = b;
        this.parent = parent;
        this.wire = wire;
        bits = new Bit[getWidth()];
        for (int bit = 0; bit < bits.length; bit++)
        {
            bits[bit] = new Bit(bit);
        }
    }

    @Override
    public ACL2Object getACL2Object()
    {
        return b.getACL2Object();
    }

    public String showFinePortDeps(Map<Object, Set<Object>> graph0, Map<Object, Set<Object>> graph1)
    {
        return parent.showFinePortDeps(bits, graph0, graph1);
    }

    List<Map<Svar<PathExt>, BigInteger>> gatherFineBitDeps(BitSet stateDeps, Map<Object, Set<Object>> graph)
    {
        return parent.gatherFineBitDeps(stateDeps, bits, graph);
    }

    @Override
    public String toString()
    {
        return toString(BigIntegerUtil.logheadMask(getWidth()));
    }

    public final int getWidth()
    {
        return wire.getWidth();
    }

    public Svar<PathExt> getVar(int delay)
    {
        return parent.newVar(this, delay, false);
    }

    public Bit getBit(int bit)
    {
        return bits[bit];
    }

    public static class PortInst extends PathExt
    {
        public final ModInstExt inst;
        public final LocalWire subpath;
        private final Bit[] parentBits;
        Lhs<PathExt> source;
        Object driver;
        public boolean splitIt;

        PortInst(ModInstExt inst, Path.Scope path)
        {
            super(inst.parent, path, inst.proto.wiresIndex.get(((Path.Wire)path.subpath).name));
            this.inst = inst;
            subpath = wire.path;
            assert inst.proto == wire.parent;
            parentBits = new Bit[getWidth()];
            wire.exported = true;
        }

        Bit getParentBit(int bit)
        {
            return parentBits[bit];
        }

        PathExt.Bit getProtoBit(int bit)
        {
            return subpath.getBit(bit);
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
            for (int bit = 0; bit < getWidth(); bit++)
            {
                parentBits[bit] = getBit(bit);
            }
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
            Util.check(lhs.width() == getWidth());
            int lsh = 0;
            for (Lhrange<PathExt> range : lhs.ranges)
            {
                Svar<PathExt> svar = range.getVar();
                Util.check(svar.getDelay() == 0);
                PathExt.LocalWire lw = (PathExt.LocalWire)svar.getName();
                for (int i = 0; i < range.getWidth(); i++)
                {
                    Util.check(parentBits[lsh + i] == null);
                    parentBits[lsh + i] = lw.getBit(range.getRsh() + i);
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

//        @Override
//        public String showFinePortDeps(Map<Object, Set<Object>> graph0, Map<Object, Set<Object>> graph1)
//        {
//            return parent.showFineExportDeps(parentBits, graph0, graph1);
//        }
//
//        @Override
//        List<Map<Svar<PathExt>, BigInteger>> gatherFineBitDeps(BitSet stateDeps, Map<Object, Set<Object>> graph)
//        {
//            return parent.gatherFineBitDeps(stateDeps, parentBits, graph);
//        }
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
            super(wire.parent, new Path.Wire(wire.getName()), wire);
            this.name = wire.getName();
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

    public class Bit
    {
        final int bit;

        private Bit(int bit)
        {
            this.bit = bit;
        }

        public PathExt getPath()
        {
            return PathExt.this;
        }

        @Override
        public String toString()
        {
            return PathExt.this.toString(BigInteger.ONE.shiftLeft(bit));
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Bit)
            {
                Bit that = (Bit)o;
                return this.getPath().equals(that.getPath())
                    && this.bit == that.bit;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 29 * hash + getPath().hashCode();
            hash = 29 * hash + bit;
            return hash;
        }
    }
}
