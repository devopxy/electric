/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TutorialHints.java
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
import com.sun.electric.tool.simulation.acl2.mods.IndexName;
import com.sun.electric.tool.simulation.acl2.mods.Lhs;
import com.sun.electric.tool.simulation.acl2.mods.ModName;
import com.sun.electric.tool.simulation.acl2.mods.Module;
import com.sun.electric.tool.simulation.acl2.mods.Name;
import com.sun.electric.tool.simulation.acl2.mods.Path;
import com.sun.electric.tool.simulation.acl2.svex.Svar;
import com.sun.electric.tool.simulation.acl2.svex.Svex;
import com.sun.electric.tool.simulation.acl2.svex.SvexManager;
import com.sun.electric.tool.simulation.acl2.svex.Vec2;
import com.sun.electric.tool.simulation.acl2.svex.Vec4;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Design hints for ACL2 SV tutorial.
 */
public class TutorialHints implements DesignHints
{
    public TutorialHints()
    {
    }

    @Override
    public List<ParameterizedModule> getParameterizedModules()
    {
        return Arrays.asList(
            new Flop(),
            new BoothFlop()
        );
    }

    @Override
    public String getGlobalClock()
    {
        return "clk";
    }

    @Override
    public String[] getExportNames(ModName modName)
    {
        switch (modName.toString())
        {
            case "alu16":
                return new String[]
                {
                    "out", "opcode", "abus", "bbus", "clk"
                };
            case "boothpipe":
                return new String[]
                {
                    "o", "a", "b", "en", "clk"
                };
            default:
                return null;
        }
    }

    @Override
    public String[] getPortInstancesToSplit(ModName modName)
    {
        return null;
    }

    @Override
    public int[] getDriversToSplit(ModName modName)
    {
        return null;
    }

    @Override
    public void testSvtv(ModName modName,
        Map<Svar<Address>, Svex<Address>> updates,
        Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates,
        SvexManager<Address> sm)
    {
        switch (modName.toString())
        {
            case "counter":
                testCounter(svtvOutExprs, svtvNextStates, sm);
                break;
            case "alu16":
                testAlu16(svtvOutExprs, svtvNextStates, sm);
                break;
            case "boothenc":
                testBoothenc(svtvOutExprs, svtvNextStates, sm);
                break;
            case "boothpipe":
                testBoothpipe(svtvOutExprs, svtvNextStates, sm);
                break;
            default:
                System.out.println("Can't test " + modName);
        }
    }

    private static class Flop extends ParameterizedModule
    {
        Flop()
        {
            super("tutorial", "flop");
        }

        @Override
        protected boolean hasState()
        {
            return true;
        }

        @Override
        protected Module<Address> genModule()
        {
            int width = getIntParam("width");
            output("q", width);
            input("d", width);
            global("clk", 1);

            assign("q", width,
                ite(bitand(bitnot(v("clk", 1)), concat(q(1), v("clk"), q(0))),
                    concat(q(width), v("d", 1), rsh(q(width), v("q", 1))),
                    v("q", 1)));

            return getModule();
        }

        @Override
        protected int getNumWires()
        {
            return 3;
        }

        @Override
        protected int getNumBits()
        {
            int width = getIntParam("width");
            return 2 * width + 1;
        }

        @Override
        protected void makeAliases(List<Lhs<IndexName>> portMap, List<Lhs<IndexName>> arr, SvexManager<IndexName> sm)
        {
            assert portMap.size() == 3;
            Lhs<IndexName> q = portMap.get(0);
            Lhs<IndexName> d = portMap.get(1);
            Lhs<IndexName> clk = portMap.get(2);
            makeAliases(arr, sm, q, d, clk);
        }

        static void makeAliases(List<Lhs<IndexName>> arr, SvexManager<IndexName> sm,
            Lhs<IndexName> q, Lhs<IndexName> d, Lhs<IndexName> clk)
        {
            arr.add(q);
            arr.add(d);
            arr.add(clk);
        }
    }

    private static class BoothFlop extends ParameterizedModule
    {

        BoothFlop()
        {
            super("tutorial", "boothflop");
        }

        @Override
        protected boolean hasState()
        {
            return true;
        }

        @Override
        protected Module<Address> genModule()
        {
            int width = getIntParam("width");
            output("q", width);
            input("d", width);
            input("clk", 1);

            assign("q", width,
                ite(bitand(bitnot(v("clk", 1)), concat(q(1), v("clk"), q(0))),
                    concat(q(width), v("d", 1), rsh(q(width), v("q", 1))),
                    v("q", 1)));

            return getModule();
        }

        @Override
        protected int getNumWires()
        {
            return 3;
        }

        @Override
        protected int getNumBits()
        {
            int width = getIntParam("width");
            return 2 * width + 1;
        }

        @Override
        protected void makeAliases(List<Lhs<IndexName>> portMap, List<Lhs<IndexName>> arr, SvexManager<IndexName> sm)
        {
            assert portMap.size() == 3;
            Lhs<IndexName> q = portMap.get(0);
            Lhs<IndexName> d = portMap.get(1);
            Lhs<IndexName> clk = portMap.get(2);
            makeAliases(arr, sm, q, d, clk);
        }

        static void makeAliases(List<Lhs<IndexName>> arr, SvexManager<IndexName> sm,
            Lhs<IndexName> q, Lhs<IndexName> d, Lhs<IndexName> clk)
        {
            arr.add(q);
            arr.add(d);
            arr.add(clk);
        }
    };

    private static void check(boolean p)
    {
        if (!p)
        {
            throw new AssertionError();
        }
    }

    public static class CounterState
    {
        byte count;
        boolean countValid;

        void setStage1(int count)
        {
            assert (count >>> 4) == 0;
            this.count = (byte)count;
            countValid = true;
        }

        void invalidate()
        {
            countValid = false;
        }

        void check()
        {
            if (countValid)
            {
                assert (count >>> 4) == 0;
            }
        }
    }

    public static int counter$count(CounterState st)
    {
        check(st.countValid);
        return st.count;
    }

    public static void counter$nextState(CounterState prev, CounterState next,
        int reset, boolean resetValid,
        int incr, boolean incrValid)
    {
        next.invalidate();
        if (resetValid)
        {
            assert (reset >>> 1) == 0;
            if (reset != 0)
            {
                next.setStage1(0);
            } else
            {
                if (prev.countValid && incrValid)
                {
                    assert (prev.count >>> 4) == 0;
                    assert (incr >>> 1) == 0;
                    int tmpcount = (prev.count + incr) & 0xF;
                    next.setStage1(tmpcount == 10 ? 0 : tmpcount);
                }
            }
        }
    }

    public void testCounter(Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates, SvexManager<Address> sm)
    {
        CounterState state = new CounterState();
        for (int i = 0; i < 16; i++)
        {
            state.setStage1(i);
            testCounter(sm, svtvOutExprs, svtvNextStates, state, 0, 0);
            testCounter(sm, svtvOutExprs, svtvNextStates, state, 0, 1);
            testCounter(sm, svtvOutExprs, svtvNextStates, state, 1, 0);
            testCounter(sm, svtvOutExprs, svtvNextStates, state, 1, 1);
        }
    }

    private void testCounter(SvexManager<Address> sm,
        Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates,
        CounterState state, int resetVal, int incrVal)
    {
        Svar<Address> reset = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("reset"))));
        Svar<Address> incr = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("incr"))));
        Svar<Address> count = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("count"))));
        Svar<Address> countDelayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("count"))), 1, false);

        Map<Svar<Address>, Vec4> env = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedOut = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedState = new HashMap<>();

        if (state.countValid)
        {
            env.put(countDelayed, Vec2.valueOf(state.count));
        }
        env.put(reset, Vec2.valueOf(resetVal));
        env.put(incr, Vec2.valueOf(incrVal));
        expectedOut.put(count, Vec2.valueOf(counter$count(state)));

        CounterState nextState = new CounterState();
        counter$nextState(state, nextState, resetVal, true, incrVal, true);
        if (nextState.countValid)
        {
            expectedState.put(countDelayed, DesignHints.makeZ(nextState.count, 4));
        }
        DesignHints.test(sm, "clk", svtvOutExprs, svtvNextStates, env, expectedOut, expectedState);
    }

    public static class Alu16State
    {
        short abus1;
        short bbus1;
        boolean validStage1;

        short out;
        boolean validStage2;

        void setStage1(int abus1, int bbus1)
        {
            assert (abus1 >>> 16) == 0;
            assert (bbus1 >>> 16) == 0;
            this.abus1 = (short)abus1;
            this.bbus1 = (short)bbus1;
            validStage1 = true;
        }

        void setStage2(int out)
        {
            assert (out >>> 16) == 0;
            this.out = (short)out;
            validStage2 = true;
        }

        void invalidate()
        {
            validStage1 = validStage2 = false;
        }

        void check()
        {
            if (validStage1)
            {
                assert (abus1 >>> 16) == 0;
                assert (bbus1 >>> 16) == 0;
            }
            if (validStage2)
            {
                assert (out >>> 16) == 0;
            }
        }
    }

    public static int alu16$out(Alu16State st)
    {
        check(st.validStage2);
        return st.out & 0xFFFF;
    }

    public static void alu16$nextState(Alu16State prev, Alu16State next,
        int opcode, boolean opcodeValid,
        int abus, boolean abusValid,
        int bbus, boolean bbusValid)
    {
        next.invalidate();
        if (abusValid && bbusValid)
        {
            assert (abus >>> 16) == 0;
            assert (bbus >>> 16) == 0;
            next.setStage1(abus, bbus);
        }

        if (prev.validStage1 && opcodeValid)
        {
            int abus1 = prev.abus1 & 0xFFFF;
            int bbus1 = prev.bbus1 & 0xFFFF;
            int ans;
            switch (opcode)
            {
                case 0:
                    int ans_plus = (abus1 + bbus1) & 0xFFFF;
                    ans = ans_plus;
                    break;
                case 1:
                    int ans_minus = (abus1 - bbus1) & 0xFFFF;
                    ans = ans_minus;
                    break;
                case 2:
                    int ans_bitand = abus1 & bbus1;
                    ans = ans_bitand;
                    break;
                case 3:
                    int ans_bitor = abus1 | bbus1;
                    ans = ans_bitor;
                    break;
                case 4:
                    int ans_bitxor = abus1 ^ bbus1;
                    ans = ans_bitxor;
                    break;
                case 5:
                    int ans_min = Math.min(abus1, bbus1);
                    ans = ans_min;
                    break;
                case 6:
                    int ans_count = Integer.bitCount(abus1 & 0xFF7F) + Integer.bitCount(abus1 & 0x0008);
                    ans = ans_count;
                    break;
                case 7:
                    int ans_mult = (abus1 * bbus1) & 0xFFFF;
                    ans = ans_mult;
                    break;
                default:
                    throw new AssertionError();
            }
            next.setStage2(ans);
        }
    }

    public void testAlu16(Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates, SvexManager<Address> sm)
    {
        Alu16State state0 = new Alu16State();
        Alu16State state1 = new Alu16State();
        Alu16State state2 = new Alu16State();
        int[] testVals =
        {
            0, 1, 0xFFFF, 5, 0x80, 0x08, 0x88
        };
        for (int abusVal : testVals)
        {
            for (int bbusVal : testVals)
            {
                state0.invalidate();
                state0.setStage1(abusVal, bbusVal);
                for (int opcodeVal = 0; opcodeVal < 8; opcodeVal++)
                {
                    testAlu16(sm, svtvOutExprs, svtvNextStates, state0, 7 - opcodeVal, abusVal, bbusVal);
                    alu16$nextState(state0, state1, 7 - opcodeVal, true, abusVal, true, bbusVal, true);
                    testAlu16(sm, svtvOutExprs, svtvNextStates, state1, opcodeVal, bbusVal, abusVal);
                    alu16$nextState(state1, state2, 7 - opcodeVal, true, abusVal, true, bbusVal, true);
                    testAlu16(sm, svtvOutExprs, svtvNextStates, state2, 0, 0, 0);
                }
            }
        }
    }

    private void testAlu16(SvexManager<Address> sm,
        Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates,
        Alu16State state, int opcodeVal, int abusVal, int bbusVal)
    {
        Svar<Address> out = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("out"))));
        Svar<Address> opcode = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("opcode"))));
        Svar<Address> abus = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("abus"))));
        Svar<Address> bbus = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("bbus"))));
        Svar<Address> abus1Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("abus1"))), 1, false);
        Svar<Address> bbus1Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("bbus1"))), 1, false);
        Svar<Address> outDelayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("out"))), 1, false);

        Map<Svar<Address>, Vec4> env = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedOut = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedState = new HashMap<>();

        if (state.validStage1)
        {
            env.put(abus1Delayed, Vec2.valueOf(state.abus1 & 0xFFFF));
            env.put(bbus1Delayed, Vec2.valueOf(state.bbus1 & 0xFFFF));
        }
        if (state.validStage2)
        {
            env.put(outDelayed, Vec2.valueOf(state.out & 0xFFFF));
        }
        env.put(opcode, Vec2.valueOf(opcodeVal));
        env.put(abus, Vec2.valueOf(abusVal));
        env.put(bbus, Vec2.valueOf(bbusVal));
        if (state.validStage2)
        {
            expectedOut.put(out, Vec2.valueOf(alu16$out(state)));
        }

        Alu16State nextState = new Alu16State();
        alu16$nextState(state, nextState, opcodeVal, true, abusVal, true, bbusVal, true);
        if (nextState.validStage1)
        {
            expectedState.put(abus1Delayed, DesignHints.makeZ(nextState.abus1 & 0xFFFF, 16));
            expectedState.put(bbus1Delayed, DesignHints.makeZ(nextState.bbus1 & 0xFFFF, 16));
        }
        if (nextState.validStage2)
        {
            expectedState.put(outDelayed, DesignHints.makeZ(nextState.out & 0xFFFF, 16));
        }
        DesignHints.test(sm, "clk", svtvOutExprs, svtvNextStates, env, expectedOut, expectedState);
    }

    int boothenc$ppImpl(int abits, int b, int minusb)
    {
        assert (abits >>> 3) == 0;
        assert (b >>> 16) == 0;
        assert (minusb >>> 17) == 0;

        int abit0 = abits & 1;
        int abit1 = (abits >> 1) & 1;
        int abit2 = (abits >> 2) & 1;

        int bsign = abit2 != 0 ? minusb : ((b << 16) >> 16) & 0x1FFFF;
        int shft = ~(abit0 ^ abit1) & 1;
        int zro = shft & ~(abit2 ^ abit1);
        int res1 = zro != 0 ? 0 : bsign;
        return shft != 0 ? res1 << 1 : ((res1 << 15) >> 15) & 0x3FFFF;
    }

    public static int boothenc$pp(int abits, int b, int minusb)
    {
        assert (abits >>> 3) == 0;
        assert (b >>> 16) == 0;
        assert (minusb >>> 17) == 0;
        // Precondition
        b = (b << 16) >> 16;
        assert minusb == ((-b) & 0x1FFFF);

        int abit0 = abits & 1;
        int abit1 = (abits >> 1) & 1;
        int abit2 = (abits >> 2) & 1;
        int enc = abit0 + abit1 - 2 * abit2;

        return (enc * b) & 0x3FFFF;
    }

    public void testBoothenc(Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates, SvexManager<Address> sm)
    {
        int[] bVals =
        {
            0, 1, 0xFFFF, 5, 0x80, 0x08, 0x88
        };
        for (int bVal : bVals)
        {
            int minusbVal = (-((bVal << 16) >> 16)) & 0x1FFFF;
            for (int abitsVal = 0; abitsVal < 8; abitsVal++)
            {
                testBoothenc(sm, svtvOutExprs, svtvNextStates, abitsVal, bVal, minusbVal);
            }
        }
    }

    private void testBoothenc(SvexManager<Address> sm,
        Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates,
        int abitsVal, int bVal, int minusbVal)
    {
        Svar<Address> pp = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("pp"))));
        Svar<Address> abits = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("abits"))));
        Svar<Address> b = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("b"))));
        Svar<Address> minusb = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("minusb"))));

        Map<Svar<Address>, Vec4> env = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedOut = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedState = new HashMap<>();

        env.put(abits, Vec2.valueOf(abitsVal));
        env.put(b, Vec2.valueOf(bVal));
        env.put(minusb, Vec2.valueOf(minusbVal));
        expectedOut.put(pp, Vec2.valueOf(boothenc$pp(abitsVal, bVal, minusbVal)));

        DesignHints.test(sm, "clk", svtvOutExprs, svtvNextStates, env, expectedOut, expectedState);
    }

    public static class BoothpipeState
    {
        short a_c1;
        short b_c1;
        boolean validStage1;

        long pp01_c2;
        long pp23_c2;
        long pp45_c2;
        long pp67_c2;
        boolean validStage2;

        int o;
        boolean validStage3;

        void setStage1(int a_c1, int b_c1)
        {
            assert (a_c1 >>> 16) == 0;
            assert (b_c1 >>> 16) == 0;
            this.a_c1 = (short)a_c1;
            this.b_c1 = (short)b_c1;
            validStage1 = true;
        }

        void setStage2(long pp01_c2, long pp23_c2, long pp45_c2, long pp67_c2)
        {
            assert (pp01_c2 >> 36) == 0;
            assert (pp23_c2 >> 36) == 0;
            assert (pp45_c2 >> 36) == 0;
            assert (pp67_c2 >> 36) == 0;
            this.pp01_c2 = pp01_c2;
            this.pp23_c2 = pp23_c2;
            this.pp45_c2 = pp45_c2;
            this.pp67_c2 = pp67_c2;
            validStage2 = true;
        }

        void setStage3(int o)
        {
            this.o = o;
            validStage3 = true;
        }

        void invalidate()
        {
            validStage1 = validStage2 = validStage3 = false;
        }

        void check()
        {
            if (validStage1)
            {
                assert (a_c1 >>> 16) == 0;
                assert (b_c1 >>> 16) == 0;
            }
            if (validStage2)
            {
                assert (pp01_c2 >>> 36) == 0;
                assert (pp23_c2 >>> 36) == 0;
                assert (pp45_c2 >>> 36) == 0;
                assert (pp67_c2 >>> 36) == 0;
            }
            if (validStage3)
            {
                // assert (o >>> 32) == 0;
            }
        }
    }

    public static int boothpipe$o(BoothpipeState st)
    {
        check(st.validStage3);
        return st.o;
    }

    public static void boothpipe$nextState(BoothpipeState prev, BoothpipeState next,
        int a, boolean aValid,
        int b, boolean bValid,
        int en, boolean enValid)
    {
        next.invalidate();
        if (!enValid)
        {
            return;
        }
        assert (en >>> 1) == 0;
        if (en == 0)
        {
            if (prev.validStage1)
            {
                next.setStage1(prev.a_c1 & 0xFFFF, prev.b_c1 & 0xFFFF);
            }
            if (prev.validStage2)
            {
                next.setStage2(prev.pp01_c2, prev.pp23_c2, prev.pp45_c2, prev.pp67_c2);
            }
            if (prev.validStage3)
            {
                next.setStage3(prev.o);
            }
            return;
        }

        if (aValid && bValid)
        {
            assert (a >>> 16) == 0;
            assert (b >>> 16) == 0;
            next.setStage1(a, b);
        }

        if (prev.validStage1)
        {

            int a_c1 = prev.a_c1 & 0xFFFF;
            int b_c1 = prev.b_c1 & 0xFFFF;
            int minusb = (-((b_c1 << 16) >> 16)) & 0x1FFFF;
            int pp0 = boothenc$pp((a_c1 << 1) & 7, b_c1, minusb);
            int pp1 = boothenc$pp((a_c1 >> 1) & 7, b_c1, minusb);
            int pp2 = boothenc$pp((a_c1 >> 3) & 7, b_c1, minusb);
            int pp3 = boothenc$pp((a_c1 >> 5) & 7, b_c1, minusb);
            int pp4 = boothenc$pp((a_c1 >> 7) & 7, b_c1, minusb);
            int pp5 = boothenc$pp((a_c1 >> 9) & 7, b_c1, minusb);
            int pp6 = boothenc$pp((a_c1 >> 11) & 7, b_c1, minusb);
            int pp7 = boothenc$pp((a_c1 >> 13) & 7, b_c1, minusb);

            long pp01_c2 = ((long)pp0 << 18) | pp1;
            long pp23_c2 = ((long)pp2 << 18) | pp3;
            long pp45_c2 = ((long)pp4 << 18) | pp5;
            long pp67_c2 = ((long)pp6 << 18) | pp7;

            next.setStage2(pp01_c2, pp23_c2, pp45_c2, pp67_c2);
        }

        if (prev.validStage2)
        {
            long pp01_c2b = ~prev.pp01_c2 & 0xFFFFFFFFFL;
            long pp23_c2b = ~prev.pp23_c2 & 0xFFFFFFFFFL;
            long pp45_c2b = ~prev.pp45_c2 & 0xFFFFFFFFFL;
            long pp67_c2b = ~prev.pp67_c2 & 0xFFFFFFFFFL;

            int pp0_c2 = ~((int)(pp01_c2b >> 18)) & 0x3FFFF;
            int pp1_c2 = ~((int)pp01_c2b) & 0x3FFFF;
            int pp2_c2 = ~((int)(pp23_c2b >> 18)) & 0x3FFFF;
            int pp3_c2 = ~((int)pp23_c2b) & 0x3FFFF;
            int pp4_c2 = ~((int)(pp45_c2b >> 18)) & 0x3FFFF;
            int pp5_c2 = ~((int)pp45_c2b) & 0x3FFFF;
            int pp6_c2 = ~((int)(pp67_c2b >> 18)) & 0x3FFFF;
            int pp7_c2 = ~((int)pp67_c2b) & 0x3FFFF;

            int s0 = (pp0_c2 << 14) >> 14;
            int s1 = s0 + ((pp1_c2 << 14) >> 12);
            int s2 = s1 + ((pp2_c2 << 14) >> 10);
            int s3 = s2 + ((pp3_c2 << 14) >> 8);
            int s4 = s3 + ((pp4_c2 << 14) >> 6);
            int s5 = s4 + ((pp5_c2 << 14) >> 4);
            int s6 = s5 + ((pp6_c2 << 14) >> 2);
            int s7 = s6 + (pp7_c2 << 14);

            int o = s7;
            next.setStage3(o);
        } else
        {
            next.validStage3 = false;
        }
    }

    public void testBoothpipe(Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates, SvexManager<Address> sm)
    {
        BoothpipeState state0 = new BoothpipeState();
        BoothpipeState state1 = new BoothpipeState();
        BoothpipeState state2 = new BoothpipeState();
        BoothpipeState state3 = new BoothpipeState();
        BoothpipeState state4 = new BoothpipeState();
        int[] testVals =
        {
            0, 1, 5100, 0x80, 0x08, 0x88, 0x8000, 0xC050, 0xFFFF
        };
        for (int aVal : testVals)
        {
            for (int bVal : testVals)
            {
                state0.invalidate();
                state0.setStage1(aVal, bVal);
                testBoothpipe(sm, svtvOutExprs, svtvNextStates, state0, aVal, bVal, 1);
                boothpipe$nextState(state0, state1, aVal, true, bVal, true, 1, true);
                testBoothpipe(sm, svtvOutExprs, svtvNextStates, state1, 0, 0, 1);
                boothpipe$nextState(state1, state2, 0, true, 0, true, 1, true);
                testBoothpipe(sm, svtvOutExprs, svtvNextStates, state2, 0, 0, 1);
                boothpipe$nextState(state2, state3, 0, true, 0, true, 1, true);
                testBoothpipe(sm, svtvOutExprs, svtvNextStates, state3, 0, 0, 1);
                boothpipe$nextState(state3, state4, 0, true, 0, true, 0, true);
                testBoothpipe(sm, svtvOutExprs, svtvNextStates, state4, 0, 0, 0);
            }
        }
    }

    private void testBoothpipe(SvexManager<Address> sm,
        Map<Svar<Address>, Svex<Address>> svtvOutExprs,
        Map<Svar<Address>, Svex<Address>> svtvNextStates,
        BoothpipeState state, int aVal, int bVal, int enVal)
    {
        Svar<Address> o = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("o"))));
        Svar<Address> a = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("a"))));
        Svar<Address> b = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("b"))));
        Svar<Address> en = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("en"))));
        Svar<Address> a_c1Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("a_c1"))), 1, false);
        Svar<Address> b_c1Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("b_c1"))), 1, false);
        Svar<Address> pp01_c2Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("pp01_c2"))), 1, false);
        Svar<Address> pp23_c2Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("pp23_c2"))), 1, false);
        Svar<Address> pp45_c2Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("pp45_c2"))), 1, false);
        Svar<Address> pp67_c2Delayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("pp67_c2"))), 1, false);
        Svar<Address> oDelayed = sm.getVar(Address.valueOf(Path.simplePath(Name.valueOf("o"))), 1, false);

        Map<Svar<Address>, Vec4> env0 = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedOut = new HashMap<>();
        Map<Svar<Address>, Vec4> expectedState = new HashMap<>();

        if (state.validStage1)
        {
            env0.put(a_c1Delayed, Vec2.valueOf(state.a_c1 & 0xFFFF));
            env0.put(b_c1Delayed, Vec2.valueOf(state.b_c1 & 0xFFFF));
        }
        if (state.validStage2)
        {
            env0.put(pp01_c2Delayed, Vec2.valueOf(state.pp01_c2));
            env0.put(pp23_c2Delayed, Vec2.valueOf(state.pp23_c2));
            env0.put(pp45_c2Delayed, Vec2.valueOf(state.pp45_c2));
            env0.put(pp67_c2Delayed, Vec2.valueOf(state.pp67_c2));
        }
        if (state.validStage3)
        {
            env0.put(oDelayed, Vec2.valueOf(state.o & 0xFFFFFFFFL));
        }
        env0.put(a, Vec2.valueOf(aVal));
        env0.put(b, Vec2.valueOf(bVal));
        env0.put(en, Vec2.valueOf(enVal));
        if (state.validStage3)
        {
            expectedOut.put(o, Vec2.valueOf(boothpipe$o(state) & 0xFFFFFFFFL));
        }

        BoothpipeState nextState = new BoothpipeState();
        boothpipe$nextState(state, nextState, aVal, true, bVal, true, enVal, true);
        if (nextState.validStage1)
        {
            expectedState.put(a_c1Delayed, DesignHints.makeZ(nextState.a_c1 & 0xFFFF, 16));
            expectedState.put(b_c1Delayed, DesignHints.makeZ(nextState.b_c1 & 0xFFFF, 16));
        }
        if (nextState.validStage2)
        {
            expectedState.put(pp01_c2Delayed, DesignHints.makeZ(nextState.pp01_c2, 36));
            expectedState.put(pp23_c2Delayed, DesignHints.makeZ(nextState.pp23_c2, 36));
            expectedState.put(pp45_c2Delayed, DesignHints.makeZ(nextState.pp45_c2, 36));
            expectedState.put(pp67_c2Delayed, DesignHints.makeZ(nextState.pp67_c2, 36));
        }
        if (nextState.validStage3)
        {
            expectedState.put(oDelayed, DesignHints.makeZ(nextState.o & 0xFFFFFFFFL, 32));
        }
        DesignHints.test(sm, "clk", svtvOutExprs, svtvNextStates, env0, expectedOut, expectedState);
    }

}
