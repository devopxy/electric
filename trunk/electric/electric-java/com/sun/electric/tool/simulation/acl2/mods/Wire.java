/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Wire.java
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

import com.sun.electric.tool.simulation.acl2.svex.BigIntegerUtil;
import static com.sun.electric.util.acl2.ACL2.*;
import com.sun.electric.util.acl2.ACL2Object;
import java.math.BigInteger;

/**
 * Wire info as stored in an svex module.
 * See <http://www.cs.utexas.edu/users/moore/acl2/manuals/current/manual/?topic=SV____WIRE>.
 */
public class Wire
{
    public final Name name;
    public final int width;
    final int low_idx;
    final int delay;
    final boolean revp;
    final ACL2Object wiretype;

    final Module parent;

    public boolean used, exported;
    BigInteger assignedBits;
    String global;

    Wire(Module parent, ACL2Object impl)
    {
        this.parent = parent;
        ACL2Object cons00 = car(impl);
        name = new Name(car(cons00));
        if (!stringp(name.impl).bool())
        {
            Util.check(integerp(name.impl).bool() || Util.KEYWORD_SELF.equals(name.impl));
            Util.check(parent.modName.isCoretype);
        }
        ACL2Object cons001 = cdr(cons00);
        width = car(cons001).intValueExact();
        Util.check(width >= 1);
        low_idx = cdr(cons001).intValueExact();
        if (consp(cdr(impl)).bool())
        {
            ACL2Object cons01 = cdr(impl);
            if (symbolp(car(cons01)).bool())
            {
                Util.checkNil(car(cons01));
                delay = 0;
            } else
            {
                delay = car(cons01).intValueExact();
            }
            if (consp(cdr(cons01)).bool())
            {
                ACL2Object cons011 = cdr(cons01);
                if (NIL.equals(car(cons011)))
                {
                    Util.checkNil(car(cons011));
                    revp = false;
                } else
                {
                    Util.check(car(cons011).equals(T));
                    revp = true;
                }
                wiretype = cdr(cons011);
            } else
            {
                Util.checkNil(cdr(cons01));
                revp = false;
                wiretype = cdr(cons01);
            }
        } else
        {
            Util.checkNil(cdr(impl));
            delay = 0;
            revp = false;
            wiretype = cdr(impl);
        }
        Util.check(delay == 0);
    }

    public int getFirstIndex()
    {
        if (revp)
        {
            throw new UnsupportedOperationException();
        }
        return low_idx + width - 1;
    }

    public int getSecondIndex()
    {
        if (revp)
        {
            throw new UnsupportedOperationException();
        }
        return low_idx;
    }

    public String toString(int width, int rsh)
    {
        if (revp)
        {
            throw new UnsupportedOperationException();
        }
        return name
            + "[" + (width == 1 ? "" : (low_idx + rsh + width - 1) + ":")
            + (low_idx + rsh) + "]";
    }

    public String toString(BigInteger mask)
    {
        String s = name.toString();
        if (mask == null)
        {
            mask = BigInteger.ZERO;
        }
        BigInteger maskH = mask.shiftRight(width);
        mask = BigIntegerUtil.loghead(width, mask);
        String indices = "";
        int ind = 0;
        boolean first = true;
        for (;;)
        {
            while (mask.signum() != 0)
            {
                int n = mask.getLowestSetBit();
                assert n >= 0;
                ind += n;
                mask = mask.shiftRight(n);
                int indL = ind;
                if (BigIntegerUtil.MINUS_ONE.equals(mask))
                {
                    if (!first)
                    {
                        indices = "," + indices;
                    }
                    indices = ":" + Integer.toString(indL) + indices;
                    break;
                }
                n = mask.not().getLowestSetBit();
                assert n >= 0;
                ind += n;
                mask = mask.shiftRight(n);
                if (indL == 0 && ind == width && maskH.signum() == 0)
                {
                    Util.check(mask.signum() == 0);
                    Util.check(indices.isEmpty());
                    return s;
                }
                Util.check(!revp);
                if (first)
                {
                    first = false;
                } else
                {
                    indices = "," + indices;
                }
                indices = Integer.toString(ind - 1) + ":" + Integer.toString(indL) + indices;
            }
            if (maskH.signum() == 0)
                break;
            indices = "/*?*/" + indices;
            mask = maskH;
            maskH = BigInteger.ZERO;
        }
        return s + "[" + indices + "]";
    }

    public String toLispString(int width, int rsh)
    {
        return toString(BigIntegerUtil.logheadMask(width).shiftLeft(rsh));
    }

    @Override
    public String toString()
    {
        Util.checkNil(wiretype);
        Util.check(delay == 0);
        String s = name.toString();
        if (width != 1)
        {
            Util.check(!revp);
            s += "[" + (low_idx + width - 1) + ":" + low_idx + "]";
        } else if (low_idx != 0)
        {
            s += "[" + low_idx + "]";
        }
        return s;
    }

    public void markAssigned(BigInteger assignedBits)
    {
        if (assignedBits.signum() == 0)
        {
            return;
        }
        Util.check(assignedBits.signum() >= 0 && assignedBits.bitLength() <= width);
        if (this.assignedBits == null)
        {
            this.assignedBits = BigInteger.ZERO;
        }
        if (assignedBits.and(this.assignedBits).signum() != 0)
        {
            System.out.println(this + " has multiple assignement");
        }
        this.assignedBits = this.assignedBits.or(assignedBits);
    }

    public boolean isAssigned()
    {
        return assignedBits != null;
    }

    public BigInteger getAssignedBits()
    {
        return assignedBits != null ? assignedBits : BigInteger.ZERO;
    }

    public void markGlobal(String name)
    {
        if (global == null)
        {
            global = name;
        } else if (!global.equals(name))
        {
            global = "";
        }
    }

    public boolean isGlobal()
    {
        return global != null && !global.isEmpty();
    }

}
