/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DesignExt.java
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

import com.sun.electric.tool.simulation.acl2.mods.Design;
import com.sun.electric.tool.simulation.acl2.mods.ModInst;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.mods.Util;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SVEX design.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____DESIGN>.
 */
public class DesignExt
{
    public final Design<? extends SvarName> b;

    public final Map<ModName, ModuleExt> downTop = new LinkedHashMap<>();
    public final Map<ModName, ModuleExt> topDown = new LinkedHashMap<>();

    public DesignExt(ACL2Object impl)
    {
        this(new Design<>(new Path.SvarBuilder(), impl));
    }

    public <N extends SvarName> DesignExt(Design<N> b)
    {
        this.b = b;

        for (ModName mn : b.modalist.keySet())
        {
            addToDownTop(mn);
        }
        Util.check(downTop.size() == b.modalist.size());

        List<ModName> keys = new ArrayList<>(downTop.keySet());
        for (int i = keys.size() - 1; i >= 0; i--)
        {
            ModName key = keys.get(i);
            topDown.put(key, downTop.get(key));
        }
        Util.check(topDown.size() == b.modalist.size());

        topDown.get(b.top).markTop();
        Map<String, Integer> globalCounts = new TreeMap<>();
        for (ModuleExt m : topDown.values())
        {
            m.markDown(globalCounts);
        }

        List<Map.Entry<String, Integer>> filteredGlobalCounts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : globalCounts.entrySet())
        {
            String canonicGlobal = TextUtils.canonicString(e.getKey());
            if (canonicGlobal.contains("clk") || canonicGlobal.contains("clock"))
            {
                filteredGlobalCounts.add(e);
            }
        }
        if (!globalCounts.isEmpty())
        {
            Collections.sort(filteredGlobalCounts, new Comparator<Map.Entry<String, Integer>>()
            {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2)
                {
                    return Integer.compare(o2.getValue(), o1.getValue());
                }

            });
            System.out.print("Probable clocks:");
            int n = 0;
            for (Map.Entry<String, Integer> e : filteredGlobalCounts)
            {
                String global = e.getKey();
                Integer count = e.getValue();
                if (n++ >= 5)
                {
                    break;
                }
                System.out.print(" " + global + "(" + count + ")");
            }
            System.out.println();
        }
    }

    private void addToDownTop(ModName mn)
    {
        if (downTop.containsKey(mn))
        {
            return;
        }
        Module<? extends SvarName> module = b.modalist.get(mn);
        for (ModInst modInst : module.insts)
        {
            addToDownTop(modInst.modname);
        }
        ModuleExt m = new ModuleExt(mn, module, downTop);
        ModuleExt old = downTop.put(mn, m);
        Util.check(old == null);
    }

    public ModName getTop()
    {
        return b.top;
    }

    public void computeCombinationalInputs(String clockName)
    {
        for (ModuleExt m : downTop.values())
        {
            m.computeCombinationalInputs(clockName);
        }
    }
}
