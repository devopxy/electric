/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ModDb.java
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

import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SV module Database.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODDB>.
 */
public class ModDb
{
    final List<ElabMod> mods = new ArrayList<>();
    final Map<ModName, ElabMod> modnameIdxes = new HashMap<>();

    public <N extends SvarName> ModDb(ModName modName, Map<ModName, Module<N>> modalist)
    {
        moduleToDb(modName, modalist);
    }

    public int nMods()
    {
        return mods.size();
    }

    public ElabMod getMod(int modIdx)
    {
        return mods.get(modIdx);
    }

    public ElabMod topMod()
    {
        return getMod(nMods() - 1);
    }

    public ElabMod modnameGetIndex(ModName modName)
    {
        return modnameIdxes.get(modName);
    }

    public Map<ModName, Module<Address>> modalistNamedToIndex(Map<ModName, Module<Address>> modalist)
    {
        Address.SvarBuilder builder = new Address.SvarBuilder();
        Map<ModName, Module<Address>> result = new LinkedHashMap<>();
        for (Map.Entry<ModName, Module<Address>> e : modalist.entrySet())
        {
            ModName modName = e.getKey();
            Module<Address> module = e.getValue();
            ElabMod modIdx = modnameIdxes.get(modName);
            Module<Address> newModule = modIdx.moduleNamedToIndex(module, builder);
            result.put(modName, newModule);
        }
        return result;
    }

    private <N extends SvarName> void moduleToDb(ModName modName, Map<ModName, Module<N>> modalist)
    {
        ElabMod elabMod = modnameIdxes.get(modName);
        if (elabMod != null)
        {
            return;
        }
        if (modnameIdxes.containsKey(modName))
        {
            throw new IllegalArgumentException("Module loop " + modName);
        }
        modnameIdxes.put(modName, null);
        Module<N> module = modalist.get(modName);
        if (module == null)
        {
            throw new IllegalArgumentException("Module not found " + modName);
        }
        for (ModInst modInst : module.insts)
        {
            moduleToDb(modInst.modname, modalist);
        }
        elabMod = new ElabMod(modName, module, modnameIdxes);
        mods.add(elabMod);
        ElabMod old = modnameIdxes.put(modName, elabMod);
        assert old == null;
    }

    public static class FlattenResult
    {
        public final Map<Lhs<IndexName>, Lhs<IndexName>> aliaspairs = new LinkedHashMap<>();
        public final Map<Lhs<IndexName>, Driver<IndexName>> assigns = new LinkedHashMap<>();
        public final IndexName.SvarBuilder builder = new IndexName.SvarBuilder();
        public LhsArr aliases;

        public ACL2Object aliaspairsToACL2Object()
        {
            ACL2Object alist = NIL;
            for (Map.Entry<Lhs<IndexName>, Lhs<IndexName>> e : aliaspairs.entrySet())
            {
                alist = cons(cons(e.getKey().getACL2Object(), e.getValue().getACL2Object()), alist);
            }
            return Util.revList(alist);
        }

        public ACL2Object assignsToACL2Object()
        {
            ACL2Object alist = NIL;
            for (Map.Entry<Lhs<IndexName>, Driver<IndexName>> e : assigns.entrySet())
            {
                alist = cons(cons(e.getKey().getACL2Object(), e.getValue().getACL2Object()), alist);
            }
            return Util.revList(alist);
        }

        public ACL2Object aliasesToACL2Object()
        {
            return aliases.collectAliasesAsACL2Objects();
        }
    }

}
