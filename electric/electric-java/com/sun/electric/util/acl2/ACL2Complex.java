/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Complex.java
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
package com.sun.electric.util.acl2;

import java.math.BigInteger;
import java.util.Objects;

/**
 * ACL2 complex-rational number.
 * Its value is a complex number with integer numerator and positive integer denominator.
 */
public class ACL2Complex extends ACL2Number {

        public final BigInteger nr;
        public final BigInteger dr;
        public final BigInteger ni;
        public final BigInteger di;

        ACL2Complex(int id, BigInteger nr, BigInteger dr, BigInteger ni, BigInteger di) {
            super(id);
            if (dr.signum() <= 0 || di.signum() <= 0 || nr.signum() == 0) {
                throw new IllegalArgumentException();
            }
            if (!nr.gcd(dr).equals(BigInteger.ONE)
                || !ni.gcd(di).equals(BigInteger.ONE)) {
                throw new IllegalArgumentException();
            }
            this.nr = nr;
            this.dr = dr;
            this.ni = ni;
            this.di = di;
        }

        @Override
        public String rep() {
            return "#c(" + nr.toString() + "/" + dr.toString() + "," + ni.toString() + "/" + di.toString();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ACL2Complex
                    && nr.equals(((ACL2Complex) o).nr)
                    && dr.equals(((ACL2Complex) o).dr)
                    && ni.equals(((ACL2Complex) o).ni)
                    && di.equals(((ACL2Complex) o).di);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Objects.hashCode(this.nr);
            hash = 59 * hash + Objects.hashCode(this.dr);
            hash = 59 * hash + Objects.hashCode(this.ni);
            hash = 59 * hash + Objects.hashCode(this.di);
            return hash;
        }
}
