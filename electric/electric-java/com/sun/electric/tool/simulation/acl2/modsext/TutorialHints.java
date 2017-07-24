/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TutorialHints.java
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

import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Design hints for ACL2 SV tutorial.
 */
public class TutorialHints implements DesignHints
{
    public TutorialHints()
    {
    }

    @Override
    public List<ParameterizedModule> getParameterizedModules()
    {
        return Arrays.asList(aluFlop, boothFlop);
    }

    @Override
    public String getGlobalClock()
    {
        return "clk";
    }

    @Override
    public String[] getExportNames(ModName modName)
    {
        switch (modName.toString())
        {
            case "alu16":
                return new String[]
                {
                    "out", "opcode", "abus", "bbus", "clk"
                };
            case "boothpipe":
                return new String[]
                {
                    "o", "a", "b", "en", "clk"
                };
            default:
                return null;
        }
    }

    @Override
    public String[] getPortInstancesToSplit(ModName modName)
    {
        return null;
    }

    @Override
    public int[] getDriversToSplit(ModName modName)
    {
        return null;
    }

    private static final ParameterizedModule aluFlop = new ParameterizedModule("tutorial", "flop")
    {
        @Override
        protected boolean hasState()
        {
            return true;
        }

        @Override
        protected Module<Path> genModule()
        {
            int width = getIntParam("width");
            output("q", width);
            input("d", width);
            global("clk", 1);

            assign("q", width,
                ite(bitand(bitnot(v("clk", 1)), concat(q(1), v("clk"), q(0))),
                    concat(q(width), v("d", 1), rsh(q(width), v("q", 1))),
                    v("q", 1)));

            return getModule();
        }
    };

    private static final ParameterizedModule boothFlop = new ParameterizedModule("tutorial", "boothflop")
    {
        @Override
        protected boolean hasState()
        {
            return true;
        }

        @Override
        protected Module<Path> genModule()
        {
            int width = getIntParam("width");
            output("q", width);
            input("d", width);
            input("clk", 1);

            assign("q", width,
                ite(bitand(bitnot(v("clk", 1)), concat(q(1), v("clk"), q(0))),
                    concat(q(width), v("d", 1), rsh(q(width), v("q", 1))),
                    v("q", 1)));

            return getModule();
        }
    };
}
