/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceModel.java
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
package com.sun.electric.tool.io.input.spicenetlist;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class SpiceModel
{
    private final String name;
    private final String flag;
    private final Map<String, String> params = new LinkedHashMap<>();

    SpiceModel(String name, String flag)
    {
        this.name = name;
        this.flag = flag;
    }

    public String getName()
    {
        return name;
    }

    public Map<String, String> getParams()
    {
        return params;
    }

    public void write(PrintStream out)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(".model ").append(name).append(" ").append(flag).append(" ");
        for (String key : params.keySet())
        {
            buf.append(key);
            String value = params.get(key);
            if (value != null)
            {
                buf.append("=");
                if (SpiceNetlistReader.WRITE_PARAMS_IN_QUOTES)
                {
                    buf.append("'").append(value).append("'");
                } else
                {
                    buf.append(value);
                }
            }
            buf.append(" ");
        }
        buf.append("\n");
        SpiceNetlistReader.multiLinePrint(out, false, buf.toString());
    }
}
