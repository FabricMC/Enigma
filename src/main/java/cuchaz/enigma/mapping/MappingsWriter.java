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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cuchaz.enigma.json.*;

public class MappingsWriter {

    public void write(File file, Mappings mappings) throws IOException {
        if (!file.isDirectory()) {
            return;
        }

        String[] entries = file.list();
        for (String s : entries) {
            File currentFile = new File(file.getPath(), s);
            currentFile.delete();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (ClassMapping classMapping : sorted(mappings.classes())) {
            if (classMapping.getDeobfName() != null && !classMapping.getDeobfName().equalsIgnoreCase("") && !classMapping.getDeobfName().equalsIgnoreCase("null")) {
                JsonClass jsonClass = new JsonClass(classMapping.getObfSimpleName(), classMapping.getDeobfName());
                write(jsonClass, classMapping);

                File f = new File(file, jsonClass.getName() + ".json");
                f.getParentFile().mkdirs();
                f.createNewFile();
                FileWriter writer = new FileWriter(f);
                writer.write(gson.toJson(jsonClass));
                writer.close();
            }
        }
    }

    private void write(JsonClass jsonClass, ClassMapping classMapping) throws IOException {
        for (ClassMapping innerClassMapping : sorted(classMapping.innerClasses())) {
            JsonClass innerClass = new JsonClass(classMapping.getObfSimpleName() + "$" + innerClassMapping.getObfSimpleName().replace("nome/", ""), innerClassMapping.getDeobfName());
            write(innerClass, innerClassMapping);
            jsonClass.addInnerClass(innerClass);
        }

        for (FieldMapping fieldMapping : sorted(classMapping.fields())) {
            jsonClass.addField(new JsonField(fieldMapping.getObfName(), fieldMapping.getDeobfName(), fieldMapping.getObfType().toString()));
        }

        for (MethodMapping methodMapping : sorted(classMapping.methods())) {
            List<JsonArgument> args = new ArrayList<>();
            for (ArgumentMapping argumentMapping : sorted(methodMapping.arguments())) {
                args.add(new JsonArgument(argumentMapping.getIndex(), argumentMapping.getName()));
            }
            if (methodMapping.getObfName().contains("<init>") || methodMapping.getObfName().contains("<clinit>")) {
                jsonClass.addConstructor(new JsonConstructor(methodMapping.getObfSignature().toString(), args, methodMapping.getObfName().contains("<clinit>")));
            } else {
                jsonClass.addMethod(new JsonMethod(methodMapping.getObfName(), methodMapping.getDeobfName(), methodMapping.getObfSignature().toString(), args));
            }
        }
    }

    private <T extends Comparable<T>> List<T> sorted(Iterable<T> classes) {
        List<T> out = new ArrayList<>();
        for (T t : classes) {
            out.add(t);
        }
        Collections.sort(out);
        return out;
    }
}
