/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.convert;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;

import java.util.Map;

import cuchaz.enigma.mapping.ClassEntry;

public class ClassNamer {

    public interface SidedClassNamer {
        String getName(String name);
    }

    private Map<String, String> sourceNames;
    private Map<String, String> destNames;

    public ClassNamer(BiMap<ClassEntry, ClassEntry> mappings) {
        // convert the identity mappings to name maps
        this.sourceNames = Maps.newHashMap();
        this.destNames = Maps.newHashMap();
        int i = 0;
        for (Map.Entry<ClassEntry, ClassEntry> entry : mappings.entrySet()) {
            String name = String.format("M%04d", i++);
            this.sourceNames.put(entry.getKey().getName(), name);
            this.destNames.put(entry.getValue().getName(), name);
        }
    }

    public String getSourceName(String name) {
        return this.sourceNames.get(name);
    }

    public String getDestName(String name) {
        return this.destNames.get(name);
    }

    public SidedClassNamer getSourceNamer() {
        return this::getSourceName;
    }

    public SidedClassNamer getDestNamer() {
        return this::getDestName;
    }
}
