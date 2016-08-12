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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import cuchaz.enigma.json.JsonClass;
import cuchaz.enigma.throwables.MappingConflict;

public class MappingsJsonReader {

    public Mappings read(File in) throws IOException {
        Mappings mappings = new Mappings(Mappings.FormatType.JSON_DIRECTORY);
        readDirectory(mappings, in);
        return mappings;
    }

    public void readDirectory(Mappings mappings, File in) throws IOException {
        File[] fList = in.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile() && Files.getFileExtension(file.getName()).equalsIgnoreCase("json")) {
                    readFile(mappings, new BufferedReader(new InputStreamReader(new FileInputStream(file),
                            Charsets.UTF_8)));
                } else if (file.isDirectory()) {
                    readDirectory(mappings, file.getAbsoluteFile());
                }
            }
        }
    }

    public void readFile(Mappings mappings, BufferedReader in) throws IOException {
        StringBuilder buf = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            buf.append(line);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonClass jsonClass = gson.fromJson(buf.toString(), JsonClass.class);
        try {
            load(null, jsonClass, mappings);
        } catch (MappingConflict e) {
            e.printStackTrace();
        }
        in.close();
    }

    public void load(ClassMapping parent, JsonClass jsonClass, Mappings mappings) throws MappingConflict {
        ClassMapping classMapping = readClass(jsonClass.getObf(), jsonClass.getName());
        if (parent != null) {
            parent.addInnerClassMapping(classMapping);
        } else {
            mappings.addClassMapping(classMapping);
        }
        jsonClass.getField().forEach(jsonField -> classMapping.addFieldMapping(readField(jsonField.getObf(), jsonField.getName(), jsonField.getType())));

        jsonClass.getConstructors().forEach(jsonConstructor -> {
            MethodMapping methodMapping = readMethod(jsonConstructor.isStatics() ? "<clinit>" : "<init>", null, jsonConstructor.getSignature());
            jsonConstructor.getArgs().forEach(jsonArgument -> {
                try {
                    methodMapping.addArgumentMapping(readArgument(jsonArgument.getIndex(), jsonArgument.getName()));
                } catch (MappingConflict e) {
                    e.printStackTrace();
                }
            });
            classMapping.addMethodMapping(methodMapping);
        });

        jsonClass.getMethod().forEach(jsonMethod -> {
            MethodMapping methodMapping = readMethod(jsonMethod.getObf(), jsonMethod.getName(), jsonMethod.getSignature());
            jsonMethod.getArgs().forEach(jsonArgument -> {
                try {
                    methodMapping.addArgumentMapping(readArgument(jsonArgument.getIndex(), jsonArgument.getName()));
                } catch (MappingConflict e) {
                    e.printStackTrace();
                }
            });
            classMapping.addMethodMapping(methodMapping);
        });

        jsonClass.getInnerClass().forEach(jsonInnerClasses -> {
            try {
                load(classMapping, jsonInnerClasses, mappings);
            } catch (MappingConflict e) {
                e.printStackTrace();
            }
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
