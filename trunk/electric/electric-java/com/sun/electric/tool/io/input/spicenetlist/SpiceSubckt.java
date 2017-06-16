/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceNetlistReader.java
 *
 * Copyright (c) 2006, Static Free Software. All rights reserved.
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
package com.sun.electric.tool.io.input.spicenetlist;

import java.util.*;
import java.io.PrintStream;

/**
 * User: gainsley
 * Date: Aug 3, 2006
 */

public class SpiceSubckt {
    public enum PortType { IN, OUT, BIDIR }

    private final String name;
    private final List<String> ports = new ArrayList<>();
    private final Map<String,String> params = new LinkedHashMap<>();
    private final Map<String,SpiceModel> localModels = new LinkedHashMap<>();
    private final Map<String,String> localParams = new LinkedHashMap<>();
    private final List<SpiceInstance> instances = new ArrayList<>();
    private final Map<String,PortType> porttypes = new HashMap<>();
    private final Map<String,SpiceSubckt> subckts = new LinkedHashMap<>();
    public SpiceSubckt(String name) {
        this.name = name;
    }
    public String getName() { return name; }
    public void addPort(String port) { ports.add(port); }
    public boolean hasPort(String portname) { return ports.contains(portname); }
    public boolean hasPortCaseInsensitive(String portname) {
        for (String port : ports) {
            if (portname.equalsIgnoreCase(port))
                return true;
        }
        return false;
    }
    public List<String> getPorts() { return ports; }
    public String getParamValue(String name) { return params.get(name); }
    public Map<String,String> getParams() { return params; }
    public Map<String,String> getLocalParams() { return localParams; }
    void addModel(SpiceModel model) { localModels.put(model.getName().toLowerCase(), model); }
    void addInstance(SpiceInstance inst) { instances.add(inst); }
    public List<SpiceInstance> getInstances() { return instances; }
    public void setPortType(String port, PortType type) {
        if (ports.contains(port) && type != null)
            porttypes.put(port, type);
    }
    public PortType getPortType(String port) { return porttypes.get(port); }
    SpiceSubckt addSubckt(SpiceSubckt subckt)
    {
        return subckts.put(subckt.name.toLowerCase(), subckt);
    }
    public SpiceSubckt findSubckt(String subcktName)
    {
        return subckts.get(subcktName.toLowerCase());
    }
    public void write(PrintStream out) {
        StringBuilder buf = new StringBuilder(".subckt ");
        buf.append(name);
        buf.append(" ");
        for (String port : ports) {
            buf.append(port);
            buf.append(" ");
        }
        for (String key : params.keySet()) {
            buf.append(key);
            buf.append("=");
            if (SpiceNetlistReader.WRITE_PARAMS_IN_QUOTES) {
                buf.append("'").append(params.get(key)).append("'");
            } else {
                buf.append(params.get(key));

            }
            buf.append(" ");
        }
        buf.append("\n");
        SpiceNetlistReader.multiLinePrint(out, false, buf.toString());
        for (String key : localParams.keySet()) {
            if (SpiceNetlistReader.WRITE_PARAMS_IN_QUOTES) {
                out.println(".param "+key+"='"+localParams.get(key) +"'");
            } else {
                out.println(".param "+key+"="+localParams.get(key));
            }
        }
        for (SpiceModel model : localModels.values()) {
            model.write(out);
        }
        for (SpiceSubckt subckt: subckts.values())
        {
            subckt.write(out);
        }
        for (SpiceInstance inst : instances) {
            inst.write(out);
        }
        out.println(".ends "+name);
    }
}
