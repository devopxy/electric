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

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SVEX design.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____DESIGN>.
 */
public class Design {
    final ACL2Object impl;

    final Map<ModName,Module> modalist = new LinkedHashMap<>();
    public final ModName top;

    public final Map<ModName,Module> downTop = new LinkedHashMap<>();

    public Design(ACL2Object impl) {
        this.impl = impl;
        List<ACL2Object> fields = Util.getList(impl, true);
        Util.check(fields.size() == 2);
        ACL2Object pair;
        pair = fields.get(0);
        Util.check(car(pair).equals(Util.SV_MODALIST));
        List<ACL2Object> modules = Util.getList(cdr(pair), true);
        for (ACL2Object so: modules) {
            Module old = modalist.put(ModName.valueOf(car(so)), new Module(cdr(so)));
            Util.check(old == null);
        }

        pair = fields.get(1);
        Util.check(car(pair).equals(Util.SV_TOP));
        top = ModName.valueOf(cdr(pair));
        Util.check(modalist.containsKey(top));
        addToDownTop(top);
        Util.check(downTop.size() == modalist.size());
        for (Module m: downTop.values()) {
            m.check(modalist);
        }
    }

    private void addToDownTop(ModName mn) {
        if (downTop.containsKey(mn)) {
            return;
        }
        Module m = modalist.get(mn);
        for (ModInst mi: m.insts) {
            addToDownTop(mi.modname);
        }
        Module old = downTop.put(mn, m);
        Util.check(old == null);
    }
}
