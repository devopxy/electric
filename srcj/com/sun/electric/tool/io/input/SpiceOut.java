/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceOut.java
 * Input/output tool: reader for basic Spice2 or GNUCap output (.spo)
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogAnalysis;
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for reading and displaying waveforms from Spice2 or GNUCap output.
 * These are contained in .spo files.
 */
public class SpiceOut extends Simulate
{
	SpiceOut() {}

	/**
	 * Method to read an Spice output file.
	 */
	protected void readSimulationOutput(Stimuli sd, URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return;

		// show progress reading .spo file
		startProgressDialog("Spice output", fileURL.getFile());

		// read the actual signal data from the .spo file
		readSpiceFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();
	}

	private final static String CELLNAME_HEADER = "*** SPICE deck for cell ";

	private void readSpiceFile(Cell cell, Stimuli sd)
		throws IOException
	{
		boolean dataMode = false;
		boolean first = true;
		boolean pastEnd = false;
		String cellName = null;
		int mostSignals = 0;
		List<List<Double>> allNumbers = new ArrayList<List<Double>>();
		for(;;)
		{
			String line = getLine();
			if (line == null) break;

			// make sure this isn't an HSPICE deck (check first line)
			if (first)
			{
				first = false;
				if (line.length() >= 20)
				{
					String hsFormat = line.substring(16, 20);
					if (hsFormat.equals("9007") || hsFormat.equals("9601"))
					{
						System.out.println("This is an HSPICE file, not a SPICE2 file");
						System.out.println("Change the SPICE format (in Preferences) and reread");
						return;
					}
				}
			}
			int len = line.length();
			if (len < 2) continue;

			// look for a cell name
			if (cellName == null && line.startsWith(CELLNAME_HEADER))
				cellName = line.substring(CELLNAME_HEADER.length());
			if (line.startsWith(".END") && !line.startsWith(".ENDS"))
			{
				pastEnd = true;
				continue;
			}
			if (line.startsWith("#Time"))
			{
//				sim_spice_signals = 0;
//				ptr = line + 5;
//				for(;;)
//				{
//					while (isWhitespace(*ptr)) ptr++;
//					if (*ptr == 0) break;
//					CHAR *pt = ptr;
//					while (!isWhitespace(*pt)) pt++;
//					CHAR save = *pt;
//					*pt = 0;
//					CHAR **newsignames = (CHAR **)emalloc((sim_spice_signals + 1) * (sizeof (CHAR *)), sim_tool->cluster);
//					for (i = 0; i < sim_spice_signals; i++) newsignames[i] = sim_spice_signames[i];
//					(void)allocstring(&newsignames[sim_spice_signals], ptr, sim_tool->cluster);
//					if (sim_spice_signames != 0) efree((CHAR*)sim_spice_signames);
//					sim_spice_signames = newsignames;
//					sim_spice_signals++;
//					*pt = save;
//					ptr = pt;
//					while (*ptr != ' ' && *ptr != 0) ptr++;
//				}
//				if (sim_spice_signals == sim_spice_printlistlen)
//				{
//					for (i = 0; i < sim_spice_signals; i++)
//					{
//						efree(sim_spice_signames[i]);
//						(void)allocstring(&sim_spice_signames[i], sim_spice_printlist[i], sim_tool->cluster);
//					}
//				}
				pastEnd = true;
				continue;
			}
			if (pastEnd && !dataMode)
			{
				if ((Character.isWhitespace(line.charAt(0)) || line.charAt(0) == '-') && TextUtils.isDigit(line.charAt(1)))
					dataMode = true;
			}
			if (pastEnd && dataMode)
			{
				if (!((Character.isWhitespace(line.charAt(0)) || line.charAt(0) == '-') && TextUtils.isDigit(line.charAt(1))))
				{
					dataMode = false;
					pastEnd = false;
				}
			}
			if (dataMode)
			{
				List<Double> numbers = new ArrayList<Double>();
				int ptr = 0;
				while (ptr < len)
				{
					while (ptr < len && Character.isWhitespace(line.charAt(ptr))) ptr++;
					int start = ptr;
					while (ptr < len && !Character.isWhitespace(line.charAt(ptr))) ptr++;
					numbers.add(new Double(TextUtils.atof(line.substring(start, ptr))));
					ptr++;
				}
				if (numbers.size() > mostSignals) mostSignals = numbers.size();
				allNumbers.add(numbers);
			}
		}

		// generate dummy names
		mostSignals--;
		if (mostSignals <= 0)
		{
			System.out.println("No data found in the file");
			return;
		}

		AnalogAnalysis an = new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_SIGNALS, false);
		sd.setCell(cell);

		// convert lists to arrays
		int numEvents = allNumbers.size();
		an.buildCommonTime(numEvents);
		for(int i=0; i<numEvents; i++)
		{
			List<Double> row = allNumbers.get(i);
			an.setCommonTime(i, row.get(0).doubleValue());
		}
		for(int j=0; j<mostSignals; j++)
		{
			double[] values = new double[numEvents];
			for(int i=0; i<numEvents; i++)
			{
				List<Double> row = allNumbers.get(i);
				values[i] = row.get(j+1).doubleValue();
			}
			an.addSignal("Signal " + (j+1), null, values);
		}
	}

}
