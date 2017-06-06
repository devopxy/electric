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
import java.util.LinkedList;
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

    /**
     * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODDB-MOD-NWIRES>
     *
     * @param modidx module index
     * @return number of local wires in the module
     */
    public int modNWires(int modidx)
    {
        return mods.get(modidx).wireTable.length;
    }

    /**
     * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODDB-MOD-NINSTS>
     *
     * @param modidx module index
     * @return number of local insts in the module
     */
    public int modNInsts(int modidx)
    {
        return mods.get(modidx).modInstTable.length;
    }

    /**
     * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODDB-MOD-TOTALWIRES>
     *
     * @param modidx module index
     * @return number of total wires in the module
     */
    public int modTotalWires(int modidx)
    {
        return mods.get(modidx).totalWires;
    }

    /**
     * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODDB-MOD-TOTALINSTS>
     *
     * @param modidx module index
     * @return number of total instances in the module
     */
    public int modTotalInsts(int modidx)
    {
        return mods.get(modidx).totalWires;
    }

    /**
     * Convert a wire index to a path relative to the module it’s in
     * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____MODDB-WIREIDX-_E3PATH>
     *
     * @param wireidx
     * @param modix
     * @return
     */
    public Path wireidxToPath(int wireidx, int modix)
    {
        ElabMod elabMod = mods.get(modix);
        if (wireidx < 0 || wireidx >= elabMod.totalWires)
        {
            throw new IllegalArgumentException();
        }
        List<Name> stack = new LinkedList<>();
        while (wireidx >= elabMod.wireTable.length)
        {
            int instIdx = elabMod.wireFindInst(wireidx);
            ElabModInst elabModInst = elabMod.modInstTable[instIdx];
            stack.add(elabModInst.instName);
            instIdx -= elabModInst.wireOffrset;
            elabMod = elabModInst.modidx;
        }
        return Path.makePath(stack, elabMod.wireTable[wireidx].name);
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
        final int totalWires;
        final int totalInsts;
        final Module<?> origMod;
        final int modMeas;

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
            int meas = 1;
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
                meas += 1 + elabModInst.instMeas;
            }
            totalWires = wireOfs;
            totalInsts = instOfs;
            modMeas = meas;
        }

        int wireFindInst(int wire)
        {
            if (wire < wireTable.length || wire >= totalWires)
            {
                throw new IllegalArgumentException();
            }
            int minInst = 0;
            int minOffset = wireTable.length;
            int maxInst = modInstTable.length;
            int maxOffset = totalWires;
            while (maxInst > 1 + minInst)
            {
                int guess = (maxInst - minInst) >> 1;
                int pivot = minInst + guess;
                int pivotOffset = modInstTable[pivot].wireOffrset;
                if (wire < pivotOffset)
                {
                    maxInst = pivot;
                    maxOffset = pivotOffset;
                } else
                {
                    minInst = pivot;
                    minOffset = pivotOffset;
                }

            }
            return minInst;
        }
    }

    public static class ElabModInst
    {
        final int instIndex;
        final Name instName;
        final ElabMod modidx;
        final int wireOffrset;
        final int instOffset;
        final int instMeas;

        ElabModInst(int instIndex, Name instName, ElabMod modidx, int wireOffset, int instOffset)
        {
            this.instIndex = instIndex;
            this.instName = instName;
            this.modidx = modidx;
            this.wireOffrset = wireOffset;
            this.instOffset = instOffset;
            instMeas = modidx.modMeas + 1;
        }
    }
}
