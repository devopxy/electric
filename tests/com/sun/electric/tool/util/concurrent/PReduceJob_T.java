/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PReduceJob.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.util.concurrent;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

/**
 * 
 * @author fs239085
 */
public class PReduceJob_T {

	@Test
	public void testPReduce() throws PoolExistsException, InterruptedException,
			CloneNotSupportedException {
		ThreadPool pool = ThreadPool.initialize();
		int stepW = 1000000;
		double step = 1.0 / stepW;
		PReduceJob<Double> pReduceJob = new PReduceJob<Double>(new BlockedRange1D(0, stepW, 100),
				new PITask(step));
		pReduceJob.execute();

		System.out.println("calc pi = " + pReduceJob.getResult());
		System.out.println("math pi = " + Math.PI);

		pool.shutdown();
		
		Assert.assertEquals(Math.PI, pReduceJob.getResult(), 0.00001);
	}

	public static class PITask extends PReduceTask<Double> {

		private double pi;
		private double step;

		public PITask(double step) {
			this.step = step;
		}

		@Override
		public Double reduce(PReduceTask<Double> other) {
			PITask task = (PITask) other;
			this.pi += task.pi;
			return this.pi * step;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForTask#execute(com
		 * .sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
		 */
		@Override
		public void execute(BlockedRange range) {
			BlockedRange1D tmpRange = (BlockedRange1D) range;
			this.pi = 0.0;

			for (int i = tmpRange.getStart(); i < tmpRange.getEnd(); i++) {
				double x = step * ((double) i - 0.5);
				this.pi += 4.0 / (1.0 + x * x);
			}

		}
	}
}
