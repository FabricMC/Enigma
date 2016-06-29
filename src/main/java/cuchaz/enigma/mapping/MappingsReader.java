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
package cuchaz.enigma.mapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import cuchaz.enigma.json.JsonClass;

public class MappingsReader {

    public Mappings read(File in) throws IOException, MappingParseException {
        Mappings mappings = new Mappings();
        readDirectory(mappings, in);
        return mappings;
    }

    public void readDirectory(Mappings mappings, File in) throws IOException, MappingParseException {

        File[] fList = in.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                readFile(mappings, new BufferedReader(new FileReader(file)));
            } else if (file.isDirectory()) {
                readDirectory(mappings, file.getAbsoluteFile());
            }
        }
    }

    public void readFile(Mappings mappings, BufferedReader in) throws IOException, MappingParseException {

        String builder = "";
        String line = null;
        while ((line = in.readLine()) != null) {
            builder += line;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonClass jsonClass = gson.fromJson(builder, JsonClass.class);
        load(null, jsonClass, mappings);
    }

    public void load(ClassMapping parent, JsonClass jsonClass, Mappings mappings) {
        ClassMapping classMapping = readClass(jsonClass.getObf(), jsonClass.getName());
        if (parent != null) {
            parent.addInnerClassMapping(classMapping);
        } else {
            mappings.addClassMapping(classMapping);
        }
        jsonClass.getField().forEach(jsonField -> classMapping.addFieldMapping(readField(jsonField.getObf(), jsonField.getName(), jsonField.getType())));

        jsonClass.getMethod().forEach(jsonMethod -> {
            MethodMapping methodMapping = readMethod(jsonMethod.getObf(), jsonMethod.getName(), jsonMethod.getSignature());
            jsonMethod.getArgs().forEach(jsonArgument -> methodMapping.addArgumentMapping(readArgument(jsonArgument.getIndex(), jsonArgument.getName())));
            classMapping.addMethodMapping(methodMapping);
        });

        jsonClass.getInnerClass().forEach(jsonInnerClasses -> {
            load(classMapping, jsonInnerClasses, mappings);
        });
    }

    private ArgumentMapping readArgument(int index, String name) {
        return new ArgumentMapping(index, name);
    }

    private ClassMapping readClass(String obf, String deobf) {
        return new ClassMapping("none/" + obf, deobf);
    }

    /* TEMP */
    protected FieldMapping readField(String obf, String deobf, String sig) {
        return new FieldMapping(obf, new Type(sig), deobf);
    }

    private MethodMapping readMethod(String obf, String deobf, String sig) {
        return new MethodMapping(obf, new Signature(sig), deobf);
    }
}
