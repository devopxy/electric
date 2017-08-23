/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ACL2DesignJobs.java
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

import com.sun.electric.tool.simulation.acl2.mods.Address;
import com.sun.electric.tool.simulation.acl2.mods.Aliaspair;
import com.sun.electric.tool.simulation.acl2.mods.Assign;
import com.sun.electric.tool.simulation.acl2.mods.Design;
import com.sun.electric.tool.simulation.acl2.mods.Driver;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModInst;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Wire;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import com.sun.electric.util.acl2.ACL2Object;
import com.sun.electric.util.acl2.ACL2Reader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Standalone programs to explore design
 *
 * @param <H> Type of design hints
 */
public class DesignExplore<H extends DesignHints>
{
    Class<H> cls;

    public DesignExplore(Class<H> cls)
    {
        this.cls = cls;
    }

    private static void help()
    {
        System.out.println("  showlibs <sao.file>");
        System.exit(1);
    }

    private void showLibs(String saoFileName)
    {
        ACL2DesignJobs.ShowSvexLibsJob.doItNoJob(cls, new File(saoFileName));
    }

    private void showMods(String saoFileName, String[] modNames)
    {
        try
        {
            File saoFile = new File(saoFileName);
            ACL2Object.initHonsMananger(saoFile.getName());
            ACL2Reader sr = new ACL2Reader(saoFile);
            SvarName.Builder<Address> snb = new Address.SvarNameBuilder();
            Design<Address> design = new Design<>(snb, sr.root);
            for (String modNameStr : modNames)
            {
                ModName modName = ModName.valueOf(modNameStr);
                showMod(System.out, modName, design.modalist.get(modName));
            }
        } catch (/*InstantiationException | IllegalAccessException |*/IOException e)
        {
            System.out.println(e.getMessage());
        } finally
        {
            ACL2Object.closeHonsManager();
        }
    }

    public static void showMod(PrintStream out, ModName modName, Module<Address> mod)
    {
        out.println();
        out.println("module " + modName);
        for (Wire wire : mod.wires)
        {
            out.println("  wire " + wire);
        }
        for (ModInst inst : mod.insts)
        {
            out.println("  " + inst.modname + " " + inst.instname);
        }
        for (Assign<Address> assign : mod.assigns)
        {
            Lhs<Address> lhs = assign.lhs;
            Driver<Address> drv = assign.driver;
            out.print("  assign " + lhs + " = ");
            GenFsmNew.printSvex(out, 1, drv.svex);
        }
        for (Aliaspair<Address> aliaspair : mod.aliaspairs)
        {
            Lhs<Address> lhs = aliaspair.lhs;
            Lhs<Address> rhs = aliaspair.rhs;
            out.println("  assign " + lhs + " = " + rhs);
        }
        out.println("endmodule // " + modName);
    }

    public void main(String[] args)
    {
        if (args.length == 0)
        {
            help();
        }
        String command = args[0];
        switch (command)
        {
            case "showlibs":
                if (args.length != 2)
                {
                    help();
                }
                showLibs(args[1]);
                break;
            case "showmod":
                if (args.length < 2)
                {
                    help();
                }
                showMods(args[1], Arrays.copyOfRange(args, 2, args.length));
                break;
            default:
                help();
        }
    }
}
