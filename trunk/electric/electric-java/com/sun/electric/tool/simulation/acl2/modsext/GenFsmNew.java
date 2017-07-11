/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenFsmNew.java
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

import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModInst;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.mods.Wire;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public abstract class GenFsmNew extends GenBase
{
    private final Map<ParameterizedModule, Set<String>> parModuleInstances = new HashMap<>();

    protected abstract List<ParameterizedModule> getParameterizedModules();

    public void scanLib(File saoFile) throws IOException
    {
        ACL2Reader sr = new ACL2Reader(saoFile);
        DesignExt design = new DesignExt(sr.root);
        List<ParameterizedModule> parModules = getParameterizedModules();
        for (Map.Entry<ModName, ModuleExt> e : design.downTop.entrySet())
        {
            ModName modName = e.getKey();
            ModuleExt m = e.getValue();
            boolean found = false;
            for (ParameterizedModule parModule : parModules)
            {
                Map<String, String> params = parModule.matchModName(modName);
                if (params != null)
                {
                    assert !found;
                    found = true;
                    Set<String> parInsts = parModuleInstances.get(parModule);
                    if (parInsts == null)
                    {
                        parInsts = new TreeSet<>(TextUtils.STRING_NUMBER_ORDER);
                        parModuleInstances.put(parModule, parInsts);
                    }
                    parInsts.add(modName.toString());
                    List<Wire> genWires = parModule.genWires(params);
                    if (genWires == null)
                    {
                        System.out.println("Module wires are unknown " + modName);
                    } else if (!genWires.equals(m.b.wires))
                    {
                        System.out.println("Module wires mismatch " + modName);
                    }
                    List<ModInst> genInsts = parModule.genInsts(params);
                    if (!genInsts.equals(m.b.insts))
                    {
                        System.out.println("Module insts mismatch " + modName);
                    }
                    Map<Lhs<Path>, Lhs<Path>> genAliaspairs = parModule.getAliaspairs(params);
                    if (!genAliaspairs.equals(m.b.aliaspairs))
                    {
                        System.out.println("Module aliaspairs mismatch " + modName);
                    }
                    if (parModule.setCurBuilder(modName))
                    {
                        Module<Path> genM = parModule.genModule();
                        if (genM == null)
                        {
                            System.out.println("Module specalizition is unfamiliar " + modName);
                        } else if (!genM.equals(m.b))
                        {
                            System.out.println("Module mismatch " + modName);
                        }
                    }
                }
            }
            if (!found)
            {
                System.out.println(modName);
            }
        }
    }

    public void showLibs()
    {
        System.out.println("========= Instances of libs ============");
        for (ParameterizedModule parModule : getParameterizedModules())
        {
            Set<String> parInsts = parModuleInstances.get(parModule);
            if (parInsts != null)
            {
                System.out.println(parModule);
                for (String parInst : parInsts)
                {
                    ModName modName = ModName.valueOf(ACL2Object.valueOf(parInst));
                    System.out.println("   " + parModule.matchModName(modName));
                }
            }
        }
    }

}
