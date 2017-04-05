/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SvexCall.java
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
package com.sun.electric.tool.simulation.acl2.svex;

import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;

/**
 * A function applied to some expressions.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____SVEX-CALL>.
 */
public class SvexCall extends Svex
{
    public final ACL2Object fn;
    private final Svex[] args;

    SvexCall(Svar.Builder sb, ACL2Object impl)
    {
        if (!consp(impl).bool())
        {
            throw new IllegalArgumentException();
        }
        fn = car(impl);
        ACL2Object a = cdr(impl);
        if (!isFnSym(fn))
        {
            throw new IllegalArgumentException();
        }
        int n;
        switch (symbol_package_name(fn).stringValueExact())
        {
            case "SV":
                switch (symbol_name(fn).stringValueExact())
                {
                    case "ID":
                        n = 1;
                        break;
                    case "BITSEL":
                        n = 2;
                        break;
                    case "UNFLOAT":
                        n = 1;
                        break;
                    case "BITNOT":
                        n = 1;
                        break;
                    case "BITAND":
                        n = 2;
                        break;
                    case "BITOR":
                        n = 2;
                        break;
                    case "BITXOR":
                        n = 2;
                        break;
                    case "RES":
                        n = 2;
                        break;
                    case "RESAND":
                        n = 2;
                        break;
                    case "RESOR":
                        n = 2;
                        break;
                    case "OVERRIDE":
                        n = 2;
                        break;
                    case "ONP":
                        n = 1;
                        break;
                    case "OFFP":
                        n = 1;
                        break;
                    case "UAND":
                        n = 1;
                        break;
                    case "UOR":
                        n = 1;
                        break;
                    case "UXOR":
                        n = 1;
                        break;
                    case "ZEROX":
                        n = 2;
                        break;
                    case "SIGNX":
                        n = 2;
                        break;
                    case "CONCAT":
                        n = 3;
                        break;
                    case "BLKREV":
                        n = 3;
                        break;
                    case "RSH":
                        n = 2;
                        break;
                    case "LSH":
                        n = 2;
                        break;
                    case "B-":
                        n = 2;
                        break;
                    case "U-":
                        n = 1;
                        break;
                    case "/":
                        n = 2;
                        break;
                    case "%":
                        n = 2;
                        break;
                    case "XDET":
                        n = 1;
                        break;
                    case "<":
                        n = 2;
                        break;
                    case "==":
                        n = 2;
                        break;
                    case "===":
                        n = 2;
                        break;
                    case "==?":
                        n = 2;
                        break;
                    case "SAFER-==?":
                        n = 2;
                        break;
                    case "==??":
                        n = 2;
                        break;
                    case "CLOG2":
                        n = 1;
                        break;
                    case "POW":
                        n = 2;
                        break;
                    case "?":
                        n = 3;
                        break;
                    case "?*":
                        n = 3;
                        break;
                    case "BIT?":
                        n = 3;
                        break;
                    case "PARTSEL":
                        n = 3;
                        break;
                    case "PARTINST":
                        n = 4;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                break;
            case "COMMON-LISP":
                switch (symbol_name(fn).stringValueExact())
                {
                    case "+":
                        n = 2;
                        break;
                    case "*":
                        n = 2;
                        break;
                    case "<":
                        n = 2;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        args = new Svex[n];
        for (int i = 0; i < n; i++)
        {
            if (!consp(a).bool())
            {
                throw new IllegalArgumentException();
            }
            args[i] = Svex.valueOf(sb, car(a));
            a = cdr(a);
        }
        if (!NIL.equals(a))
        {
            throw new IllegalArgumentException();
        }
    }

    public Svex[] getArgs()
    {
        return args.clone();
    }

    @Override
    public ACL2Object makeACL2Object()
    {
        ACL2Object a = NIL;
        for (int i = args.length - 1; i >= 0; i++)
        {
            a = cons(args[i].makeACL2Object(), a);
        }
        return cons(fn, a);
    }

    private static boolean isFnSym(ACL2Object o)
    {
        return symbolp(o).bool() && !QUOTE.equals(o) && !keywordp(o).bool();
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data)
    {
        return visitor.visitCall(fn, args, data);
    }

}
