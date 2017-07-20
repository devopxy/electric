/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DesignHints.java
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

import java.util.Collections;
import java.util.List;

/**
 * Hints to generate FSM for particular design
 */
public interface DesignHints
{
    List<ParameterizedModule> getParameterizedModules();

    String getGlobalClock();

    List<String> getSpecialOutputs();

    public static class Dummy implements DesignHints
    {
        @Override
        public List<ParameterizedModule> getParameterizedModules()
        {
            return Collections.emptyList();
        }

        @Override
        public String getGlobalClock()
        {
            return null;
        }

        @Override
        public List<String> getSpecialOutputs()
        {
            return Collections.emptyList();
        }

    }
}
