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
package com.sun.electric.tool.simulation.acl2.mods;

import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.SvarName;
import static com.sun.electric.util.acl2.ACL2.car;
import static com.sun.electric.util.acl2.ACL2.cdr;
import static com.sun.electric.util.acl2.ACL2.consp;
import com.sun.electric.util.acl2.ACL2Object;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SVEX design.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____DESIGN>.
 *
 * @param <N> Type of names in Svex variables
 */
public class Design<N extends SvarName>
{
    final Map<ModName, Module<N>> modalist = new LinkedHashMap<>();
    public final ModName top;

    public Design(Svar.Builder<N> builder, ACL2Object impl)
    {
        List<ACL2Object> fields = Util.getList(impl, true);
        Util.check(fields.size() == 2);
        ACL2Object pair;
        pair = fields.get(0);
        Util.check(car(pair).equals(Util.SV_MODALIST));
        ACL2Object rawModalist = cdr(pair);
        while (consp(rawModalist).bool())
        {
            ModName modName = ModName.valueOf(car(car(rawModalist)));
            Module<N> module = new Module<>(builder, cdr(car(rawModalist)));
            Module old = modalist.put(modName, module);
            Util.check(old == null);
            rawModalist = cdr(rawModalist);
        }
        Util.checkNil(rawModalist);
        pair = fields.get(1);
        Util.check(car(pair).equals(Util.SV_TOP));
        top = ModName.valueOf(cdr(pair));
    }
}
