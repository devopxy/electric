/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Design.java
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

import com.sun.electric.util.TextUtils;
import static com.sun.electric.util.acl2.ACL2.*;
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
public class Design
{
    final ACL2Object impl;

//    final Map<ModName,Module> modalist = new LinkedHashMap<>();
    public final ModName top;

    public final Map<ModName, Module> downTop = new LinkedHashMap<>();
    public final Map<ModName, Module> topDown = new LinkedHashMap<>();

    public Design(ACL2Object impl)
    {
        this.impl = impl;
        List<ACL2Object> fields = Util.getList(impl, true);
        Util.check(fields.size() == 2);
        ACL2Object pair;
        pair = fields.get(0);
        Util.check(car(pair).equals(Util.SV_MODALIST));
        Map<ModName, ACL2Object> rawMods = new LinkedHashMap<>();
        ACL2Object modalist = cdr(pair);
        while (consp(modalist).bool())
        {
            ModName modName = ModName.valueOf(car(car(modalist)));
            ACL2Object old = rawMods.put(modName, cdr(car(modalist)));
            Util.check(old == null);
            modalist = cdr(modalist);
        }
        Util.checkNil(modalist);
        pair = fields.get(1);
        Util.check(car(pair).equals(Util.SV_TOP));
        top = ModName.valueOf(cdr(pair));
//        addToDownTop(top, rawMods);

        for (ModName mn : rawMods.keySet())
        {
            addToDownTop(mn, rawMods);
        }
        Util.check(downTop.size() == rawMods.size());

        List<ModName> keys = new ArrayList<>(downTop.keySet());
        for (int i = keys.size() - 1; i >= 0; i--)
        {
            ModName key = keys.get(i);
            topDown.put(key, downTop.get(key));
        }
        Util.check(topDown.size() == rawMods.size());

        topDown.get(top).markTop();
        Map<String, Integer> globalCounts = new TreeMap<>();
        for (Module m : topDown.values())
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

    private void addToDownTop(ModName mn, Map<ModName, ACL2Object> rawMods)
    {
        if (downTop.containsKey(mn))
        {
            return;
        }
        ACL2Object rawMod = rawMods.get(mn);
        List<ACL2Object> fields = Util.getList(rawMod, true);
        Util.check(fields.size() == 4);

        ACL2Object pair = fields.get(1);
        Util.check(car(pair).equals(Util.SV_INSTS));
        for (ACL2Object o : Util.getList(cdr(pair), true))
        {
            addToDownTop(ModName.valueOf(cdr(o)), rawMods);
        }
        Module m = new Module(mn, rawMod, downTop);
        Module old = downTop.put(mn, m);
        Util.check(old == null);
    }
}
