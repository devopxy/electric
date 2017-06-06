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

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import java.util.ArrayList;
import java.util.HashMap;
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

    <V extends Svar> ModDb(ModName modName, Map<ModName, Module<V>> modalist)
    {
        moduleToDb(modName, modalist);
    }

    private <V extends Svar> void moduleToDb(ModName modName, Map<ModName, Module<V>> modalist)
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
        Module<V> module = modalist.get(modName);
        if (module == null)
        {
            throw new IllegalArgumentException("Module not found " + modName);
        }
        for (ModInst modInst : module.insts)
        {
            moduleToDb(modInst.modname, modalist);
        }
        elabMod = new ElabMod(mods.size(), modName, module);
        mods.add(elabMod);
        ElabMod old = modnameIdxes.put(modName, elabMod);
        assert old == null;
    }

    public class ElabMod
    {
        final int index;
        final ModName modName;
        final Wire[] wireTable;
        final Map<Name, Integer> wireNameIdxes = new HashMap<>();
        final ElabModInst[] modInstTable;
        final Map<Name, ElabModInst> modInstNameIdxes = new HashMap<>();
        int totalWires;
        int totalInsts;
        Module<?> origMod;

        ElabMod(int index, ModName modName, Module<?> origMod)
        {
            this.index = index;
            this.modName = modName;
            this.origMod = origMod;
            wireTable = origMod.wires.toArray(new Wire[origMod.wires.size()]);
            for (int i = 0; i < wireTable.length; i++)
            {
                Wire wire = wireTable[i];
                Integer old = wireNameIdxes.put(wire.name, i);
                assert old == null;
            }
            modInstTable = new ElabModInst[origMod.insts.size()];
            int wireOfs = wireTable.length;
            int instOfs = modInstTable.length;
            for (int i = 0; i < origMod.insts.size(); i++)
            {
                ModInst modInst = origMod.insts.get(i);
                ElabMod modidx = modnameIdxes.get(modInst.modname);
                if (modidx == null)
                {
                    throw new IllegalArgumentException();
                }
                ElabModInst elabModInst = new ElabModInst(i, modInst.instname, modidx, wireOfs, instOfs);
                modInstTable[i] = elabModInst;
                ElabModInst old = modInstNameIdxes.put(modInst.instname, elabModInst);
                assert old == null;
                wireOfs += modidx.totalWires;
                instOfs += modidx.totalInsts;
            }
            totalWires = wireOfs;
            totalInsts = instOfs;
        }
    }

    public static class ElabModInst
    {
        final int instIndex;
        final Name instName;
        final ElabMod modidx;
        final int wireOffrset;
        final int instOffset;

        ElabModInst(int instIndex, Name instName, ElabMod modidx, int wireOffset, int instOffset)
        {
            this.instIndex = instIndex;
            this.instName = instName;
            this.modidx = modidx;
            this.wireOffrset = wireOffset;
            this.instOffset = instOffset;
        }
    }
}
