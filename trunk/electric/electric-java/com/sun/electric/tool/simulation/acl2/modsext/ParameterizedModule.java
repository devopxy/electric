/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParameterizedModule.java
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

import com.sun.electric.tool.simulation.acl2.mods.Driver;
import com.sun.electric.tool.simulation.acl2.mods.Lhatom;
import com.sun.electric.tool.simulation.acl2.mods.Lhrange;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModInst;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.mods.Util;
import com.sun.electric.tool.simulation.acl2.mods.Wire;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexCall;
import com.sun.electric.tool.simulation.acl2.svex.SvexQuote;
import com.sun.electric.tool.simulation.acl2.svex.SvexVar;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec3Fix;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4BitExtract;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Bitand;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Bitnot;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Bitor;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Bitxor;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Concat;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Equality;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Ite;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4IteStmt;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4PartSelect;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Plus;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4ReductionOr;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4Rsh;
import com.sun.electric.tool.simulation.acl2.svex.funs.Vec4ZeroExt;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parameterized SVEX module.
 * Subclasses represent concrete modules.
 */
public abstract class ParameterizedModule
{
    public final String libName;
    public final String modName;
    private final String paramPrefix;
    private final String paramDelim;

    /**
     * Current ModName.
     * There are methods to generate SVEX module for current mod name.
     * These variables are not theread-safe.
     */
    private ModName curModName;
    private Builder curBuilder;
    private String curInstName;

    public ParameterizedModule(String libName, String modName)
    {
        this(libName, modName, "$", "=");
    }

    protected ParameterizedModule(String libName, String modName, String paramPrefix, String paramDelim)
    {
        this.libName = libName;
        this.modName = modName;
        this.paramPrefix = paramPrefix;
        this.paramDelim = paramDelim;
    }

    protected boolean hasState()
    {
        return false;
    }

    /**
     * Check if modName is a name of a specialization of this parameterized module
     *
     * @param modName a name of a module specialization
     * @return map from parameter names to parameter values
     */
    protected Map<String, String> matchModName(ModName modName)
    {
        if (!modName.isString)
        {
            return null;
        }
        String modNameStr = modName.toString();
        if (!modNameStr.startsWith(this.modName))
        {
            return null;
        }
        String params = modNameStr.substring(this.modName.length());
        Map<String, String> parMap = new LinkedHashMap<>();
        while (!params.isEmpty())
        {
            if (!params.startsWith(paramPrefix))
            {
                return null;
            }
            params = params.substring(paramPrefix.length());
            int indDelim = params.indexOf(paramDelim);
            if (indDelim < 0)
            {
                return null;
            }
            int nextPrefix = params.indexOf(paramPrefix, indDelim + paramDelim.length());
            if (nextPrefix < 0)
            {
                nextPrefix = params.length();
            }
            String paramName = params.substring(0, indDelim);
            String paramVal = params.substring(indDelim + paramDelim.length(), nextPrefix);
            parMap.put(paramName, paramVal);
            params = params.substring(nextPrefix);
        }
        return parMap;
    }

    /**
     * Check if modName is a specialization of this parameterized module.
     * If it is a specialization then set <code>this.curModName</code>
     * to the <code>modName</code>.
     *
     * @param modName ModName to check
     * @return true if modName is a specialization of this parameterized module.
     */
    public boolean setCurBuilder(ModName modName)
    {
        Map<String, String> params = matchModName(modName);
        curInstName = null;
        if (params == null)
        {
            curModName = null;
            curBuilder = null;
            return false;
        }
        curModName = modName;
        curBuilder = new Builder(params);
        return true;
    }

    public String getModNameStr()
    {
        return modName;
    }

    /**
     * Return a string value of parameter of current ModName.
     *
     * @param paramName name of a parameter
     * @return Stringvalue of a parameter or null if parameter is absent.
     */
    protected String getParam(String paramName)
    {
        return curBuilder.getParam(paramName);
    }

    /**
     * Return an string value of parameter of current ModName.
     *
     * @param paramName name of a parameter
     * @return integer value of a parameter or null if parameter is absent.
     * @throws NullPointerException if parameter is absent
     * @throws NumberFormatException if parameter is not an integer
     */
    protected int getIntParam(String paramName)
    {
        return curBuilder.getIntParam(paramName);
    }

    /**
     * Generate a local wire for current modName.
     *
     * @param wireName name of a wire
     * @param width width of wire
     */
    protected void wire(String wireName, int width)
    {
        curBuilder.wire(wireName, width);
    }

    /**
     * Generate a local wire for current modName.
     *
     * @param name name of a wire
     * @param width width of wire
     */
    protected void wire(Name name, int width)
    {
        curBuilder.wire(name, width);
    }

    /**
     * Generate an input for current modName.
     *
     * @param wireName name of an input
     * @param width width of an input
     */
    protected void input(String wireName, int width)
    {
        wire(wireName, width);
    }

    /**
     * Generate a global input for current modName.
     * Examples of global inputs are power supply or global clock.
     *
     * @param wireName name of a global input
     * @param width width of a global input
     */
    protected void global(String wireName, int width)
    {
        wire(wireName, width);
    }

    /**
     * Generate an output for current modName.
     *
     * @param wireName name of an output
     * @param width width of an output
     */
    protected void output(String wireName, int width)
    {
        wire(wireName, width);
    }

    /**
     * Generate an unused export for current modName.
     *
     * @param wireName name of an unused export
     * @param width width of an unused export
     */
    protected void unused(String wireName, int width)
    {
        wire(wireName, width);
    }

    /**
     * Generate a local wire for current modName.
     *
     * @param name name of a wire
     * @param width width of wire
     */
    protected void unused(Name name, int width)
    {
        wire(name, width);
    }

    /**
     * Generate a module instance for current modName.
     *
     * @param instName name of a module instance
     * @param modName ModName of instance prototype.
     */
    protected void instance(String instName, ModName modName)
    {
        curBuilder.instance(modName, instName);
        curInstName = instName;
    }

    /**
     * Generate a module instance for current modName.
     * Sets current instance for the new instance.
     *
     * @param instName name of a module instance
     * @param modName string name of instance prototype.
     */
    protected void instance(String instName, String modName)
    {
        instance(instName, ModName.valueOf(ACL2Object.valueOf(modName)));
    }

    /**
     * Generate an Lhrange for current modName.
     * Example: <code>r("din", 3, 1)</code> stands for Verilog name din[3:1].
     *
     * @param wireName name of a local wire
     * @param msb most significant bit
     * @param lsb least significant bit
     * @return Lhrange
     */
    protected Lhrange<Path> r(Name wireName, int msb, int lsb)
    {
        return curBuilder.r(wireName, msb, lsb);
    }

    /**
     * Generate an Lhrange for current modName.
     * Example: <code>r("din", 3, 1)</code> stands for Verilog name din[3:1].
     *
     * @param wireName name of a local wire
     * @param msb most significant bit
     * @param lsb least significant bit
     * @return Lhrange
     */
    protected Lhrange<Path> r(String wireName, int msb, int lsb)
    {
        return curBuilder.r(wireName, msb, lsb);
    }

    /**
     * Generate an Lhrange for current modName.
     * Example: <code>r("inst1", "din", 3, 1)</code> stands for Verilog name inst1.din[3:1].
     *
     * @param instName name of an inatance port
     * @param portName name of a port
     * @param msb most significant bit
     * @param lsb least significant bit
     * @return Lhrange
     */
    protected Lhrange<Path> r(String instName, String portName, int msb, int lsb)
    {
        return curBuilder.r(instName, portName, msb, lsb);
    }

    protected Svex<Path> unfloat(Svex<Path> x)
    {
        return SvexCall.newCall(Vec3Fix.FUNCTION, x);
    }

    protected Svex<Path> uor(Svex<Path> x)
    {
        return SvexCall.newCall(Vec4ReductionOr.FUNCTION, x);
    }

    protected Svex<Path> ite(Svex<Path> test, Svex<Path> th, Svex<Path> el)
    {
        return SvexCall.newCall(Vec4Ite.FUNCTION, test, th, el);
    }

    protected Svex<Path> iteStmt(Svex<Path> test, Svex<Path> th, Svex<Path> el)
    {
        return SvexCall.newCall(Vec4IteStmt.FUNCTION, test, th, el);
    }

    protected Svex<Path> bitnot(Svex<Path> x)
    {
        return SvexCall.newCall(Vec4Bitnot.FUNCTION, x);
    }

    protected Svex<Path> bitnotE(int width, Svex<Path> x)
    {
        return zext(q(width), bitnot(x));
    }

    protected Svex<Path> bitnotE(Svex<Path> x)
    {
        return bitnotE(1, x);
    }

    protected Svex<Path> bitand(Svex<Path> x, Svex<Path> y)
    {
        return SvexCall.newCall(Vec4Bitand.FUNCTION, x, y);
    }

    protected Svex<Path> bitandE(int width, Svex<Path>... x)
    {
        Svex<Path> result = null;
        for (Svex<Path> xi : x)
        {
            result = result != null ? zext(q(width), bitand(result, xi)) : xi;
        }
        return result;
    }

    protected Svex<Path> bitandE(Svex<Path>... x)
    {
        return bitandE(1, x);
    }

    protected Svex<Path> bitor(Svex<Path> x, Svex<Path> y)
    {
        return SvexCall.newCall(Vec4Bitor.FUNCTION, x, y);
    }

    protected Svex<Path> bitorE(int width, Svex<Path>... x)
    {
        Svex<Path> result = null;
        for (Svex<Path> xi : x)
        {
            result = result != null ? zext(q(width), bitor(result, xi)) : xi;
        }
        return result;
    }

    protected Svex<Path> bitorE(Svex<Path>... x)
    {
        return bitorE(1, x);
    }

    protected Svex<Path> bitxor(Svex<Path> x, Svex<Path> y)
    {
        return SvexCall.newCall(Vec4Bitxor.FUNCTION, x, y);
    }

    protected Svex<Path> bitxorE(int width, Svex<Path>... x)
    {
        Svex<Path> result = null;
        for (Svex<Path> xi : x)
        {
            result = result != null ? zext(q(width), bitxor(result, xi)) : xi;
        }
        return result;
    }

    protected Svex<Path> bitxorE(int width, List<Svex<Path>> x)
    {
        return bitxorE(width, x.toArray(Svex.newSvexArray(x.size())));
    }

    protected Svex<Path> bitxorE(Svex<Path>... x)
    {
        return bitxorE(1, x);
    }

    protected Svex<Path> plus(Svex<Path> x, Svex<Path> y)
    {
        return SvexCall.newCall(Vec4Plus.FUNCTION, x, y);
    }

    protected Svex<Path> eq(Svex<Path> x, Svex<Path> y)
    {
        return SvexCall.newCall(Vec4Equality.FUNCTION, x, y);
    }

    protected Svex<Path> concat(Svex<Path> n, Svex<Path> lower, Svex<Path> upper)
    {
        return SvexCall.newCall(Vec4Concat.FUNCTION, n, lower, upper);
    }

    protected Svex<Path> bitExtract(Svex<Path> index, Svex<Path> x)
    {
        return SvexCall.newCall(Vec4BitExtract.FUNCTION, index, x);
    }

    protected Svex<Path> partSelect(Svex<Path> lsb, Svex<Path> width, Svex<Path> x)
    {
        return SvexCall.newCall(Vec4PartSelect.FUNCTION, lsb, width, x);
    }

    protected Svex<Path> rsh(Svex<Path> n, Svex<Path> x)
    {
        return SvexCall.newCall(Vec4Rsh.FUNCTION, n, x);
    }

    protected Svex<Path> zext(Svex<Path> n, Svex<Path> x)
    {
        return SvexCall.newCall(Vec4ZeroExt.FUNCTION, n, x);
    }

    protected Svex<Path> q(int val)
    {
        return q(new Vec2(val));
    }

    protected Svex<Path> q(int upper, int lower)
    {
        return q(Vec4.valueOf(BigInteger.valueOf(upper), BigInteger.valueOf(lower)));
    }

    protected Svex<Path> q(Vec4 val)
    {
        return new SvexQuote<>(val);
    }

    protected Svex<Path> v(String wireName)
    {
        return v(wireName, 0);
    }

    protected Svex<Path> v(String wireName, int delay)
    {
        Name name = curBuilder.getName(wireName);
        Path path = Path.simplePath(name);
        Svar<Path> svar = curBuilder.builder.newVar(path, delay, false);
        return new SvexVar<>(svar);
    }

    protected Svex<Path> vE(String wireName)
    {
        return zext(q(1), v(wireName, 0));
    }

    /**
     * Generate an assignment of an svex expression to a local wire for current modName
     *
     * @param wireName name of a local wire
     * @param width width of a local wire
     * @param svex SVEX expression
     */
    protected void assign(String wireName, int width, Svex<Path> svex)
    {
        assign(r(wireName, width - 1, 0), svex);
    }

    /**
     * Generate an assignment of an svex expression to a local wire for current modName
     *
     * @param instName name of an instance
     * @param portName name of a port
     * @param width width of a local wire
     * @param svex SVEX expression
     */
    protected void assign(String instName, String portName, int width, Svex<Path> svex)
    {

        assign(r(instName, portName, width - 1, 0), svex);
    }

    /**
     * Generate an assignment of an svex expression to a Lhrange for current modName
     *
     * @param range Lhrange
     * @param svex SVEX expression
     */
    protected void assign(Lhrange<Path> range, Svex<Path> svex)
    {
        curBuilder.assign(range, svex);
    }

    /**
     * Generate an assignment of an svex expression to a Lhrange for current modName
     *
     * @param range1 upper Lhrange
     * @param range2 lower Lhrange
     * @param svex SVEX expression
     */
    protected void assign(Lhrange<Path> range1, Lhrange<Path> range2, Svex<Path> svex)
    {
        curBuilder.assign(new Lhs<>(Arrays.asList(range1, range2)), svex);
    }

    public void conn(Lhrange<Path> lrange, Lhrange<Path>... ranges)
    {
        curBuilder.conn(lrange, ranges);
    }

    /**
     * Generate a connection a port of current instance to a list
     * of Lhranges for current ModName.
     *
     * @param portName name of port
     * @param ranges list of Lhranges
     */
    protected void conn(String portName, Lhrange<Path>... ranges)
    {
        curBuilder.conn(curInstName, portName, ranges);
    }

    /**
     * Generate a connection of port of current instance to a local wire
     * for current ModName
     *
     * @param portName name of port
     * @param wireName name of local wire
     * @param width width of local wire.
     */
    protected void conn(String portName, String wireName, int width)
    {
        conn(portName, r(wireName, width - 1, 0));
    }

    protected Module<Path> getModule()
    {
        return curBuilder.getModule();
    }

    protected Module<Path> genModule()
    {
        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ParameterizedModule)
        {
            ParameterizedModule that = (ParameterizedModule)o;
            return this.modName.equals(that.modName);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + modName.hashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return libName.isEmpty() ? modName : libName + "." + modName;
    }

    public Builder newBuilder(Map<String, String> params)
    {
        return new Builder(params);
    }

    public class Builder
    {
        private final Map<String, String> params;
        private final Map<String, Name> names = new HashMap<>();
        private final Svar.Builder<Path> builder = new Path.SvarBuilder();
        private final List<Wire> wires = new ArrayList<>();
        private final List<ModInst> insts = new ArrayList<>();
        private final Map<Lhs<Path>, Driver<Path>> assigns = new LinkedHashMap<>();
        private final Map<Lhs<Path>, Lhs<Path>> aliaspairs = new LinkedHashMap<>();

        private Builder(Map<String, String> params)
        {
            this.params = params;
        }

        public String getParam(String paramName)
        {
            return params.get(paramName);
        }

        public int getIntParam(String paramName)
        {
            return Integer.parseInt(getParam(paramName));
        }

        private Name getName(String nameStr)
        {
            Name name = names.get(nameStr);
            if (name == null)
            {
                name = new Name(honscopy(ACL2Object.valueOf(nameStr)));
                names.put(nameStr, name);
            }
            return name;
        }

        public void wire(Name name, int width)
        {
            wires.add(new Wire(name, width));
        }

        public void wire(String wireName, int width)
        {
            wire(getName(wireName), width);
        }

        public void input(String wireName, int width)
        {
            wire(wireName, width);
        }

        public void global(String wireName, int width)
        {
            wire(wireName, width);
        }

        public void unused(String wireName, int width)
        {
            wire(wireName, width);
        }

        public void unusedGlobal(String wireName, int width)
        {
            wire(wireName, width);
        }

        public void output(String wireName, int width)
        {
            wire(wireName, width);
        }

        public void instance(ModName modName, String instName)
        {
            insts.add(new ModInst(getName(instName), modName));

        }

        public void instance(String modName, String instName)
        {
            instance(ModName.valueOf(ACL2Object.valueOf(modName)), instName);
        }

        public Lhrange<Path> r(Name wireName, int leftIndex, int rightIndex)
        {
            Path rpath = Path.simplePath(wireName);
            Svar<Path> svar = builder.newVar(rpath, 0, false);
            int width = leftIndex - rightIndex + 1;
            if (width <= 0)
            {
                throw new IllegalArgumentException();
            }
            return new Lhrange<>(width, new Lhatom.Var<>(svar, rightIndex));
        }

        public Lhrange<Path> r(String wireName, int leftIndex, int rightIndex)
        {
            return r(getName(wireName), leftIndex, rightIndex);
        }

        public Lhrange<Path> r(String instName, String portName, int leftIndex, int rightIndex)
        {
            Path rpath = Path.makePath(Arrays.asList(getName(instName)), getName(portName));
            Svar<Path> svar = builder.newVar(rpath, 0, false);
            int width = leftIndex - rightIndex + 1;
            if (width <= 0)
            {
                throw new IllegalArgumentException();
            }
            return new Lhrange<>(width, new Lhatom.Var<>(svar, rightIndex));
        }

        public void conn(Lhrange<Path> lrange, Lhrange<Path>... ranges)
        {
            Lhs<Path> lhs = new Lhs<>(Arrays.asList(lrange));
            Lhs<Path> rhs = new Lhs<>(Arrays.asList(ranges));
            Util.check(lhs.width() == rhs.width());
            aliaspairs.put(lhs, rhs);
        }

        public void conn(String instName, String portName, Lhrange<Path>... ranges)
        {
            Lhs<Path> rhs = new Lhs<>(Arrays.asList(ranges));
            Path lpath = Path.makePath(Arrays.asList(getName(instName)), getName(portName));
            Svar<Path> lvar = builder.newVar(lpath, 0, false);
            Lhrange<Path> lrange = new Lhrange<>(rhs.width(), new Lhatom.Var<>(lvar, 0));
            conn(lrange, ranges);
        }

        public void conn(String instName, String portName, String wireName, int width)
        {
            conn(instName, portName, r(wireName, width - 1, 0));
        }

        public void assign(Lhrange<Path> wire, Svex<Path> svex)
        {
            assign(new Lhs<>(Arrays.asList(wire)), svex);
        }

        public void assign(Lhs<Path> lhs, Svex<Path> svex)
        {
            Driver<Path> driver = new Driver<>(svex);
            assigns.put(lhs, driver);
        }

        public List<Wire> getWires()
        {
            return wires;
        }

        public List<ModInst> getInsts()
        {
            return insts;
        }

        public Map<Lhs<Path>, Lhs<Path>> getAliaspairs()
        {
            return aliaspairs;
        }

        protected Module<Path> getModule()
        {
            return new Module<>(wires, insts, assigns, aliaspairs);
        }
    }
}
