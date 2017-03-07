package cuchaz.enigma.mapping;

import com.google.common.base.Charsets;
import cuchaz.enigma.analysis.TranslationIndex;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 11/08/2016.
 */
public class MappingsSRGWriter {

    public void write(File file, Mappings mappings) throws IOException {
        if(file.exists()){
           file.delete();
        }
        file.createNewFile();

        TranslationIndex index = new TranslationIndex();

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8));
        List<String> fieldMappings = new ArrayList<>();
        List<String> methodMappings = new ArrayList<>();
        for (ClassMapping classMapping : sorted(mappings.classes())) {
            if(classMapping.getDeobfName() == null || classMapping.getObfSimpleName() == null || classMapping.getDeobfName() == null){
                continue;
            }
            writer.write("CL: " + classMapping.getObfSimpleName() + " " + classMapping.getDeobfName());
            writer.write(System.lineSeparator());
            for (ClassMapping innerClassMapping : sorted(classMapping.innerClasses())) {
                if(innerClassMapping.getDeobfName() == null || innerClassMapping.getObfSimpleName() == null || innerClassMapping.getDeobfName() == null){
                    continue;
                }
                String innerClassName = classMapping.getObfSimpleName() + "$" + innerClassMapping.getObfSimpleName();
                String innerDeobfClassName = classMapping.getDeobfName() + "$" + innerClassMapping.getDeobfName();
                writer.write("CL: " + innerClassName + " " + classMapping.getDeobfName() + "$" + innerClassMapping.getDeobfName());
                writer.write(System.lineSeparator());
                for (FieldMapping fieldMapping : sorted(innerClassMapping.fields())) {
                    fieldMappings.add("FD: " + innerClassName + "/" + fieldMapping.getObfName() + " " + innerDeobfClassName + "/" + fieldMapping.getDeobfName());
                }

                for (MethodMapping methodMapping : sorted(innerClassMapping.methods())) {
                    methodMappings.add("MD: " + innerClassName + "/" + methodMapping.getObfName() + " " + methodMapping.getObfSignature().toString() + " " + innerDeobfClassName + "/" + methodMapping.getDeobfName() + " " + mappings.getTranslator(TranslationDirection.Deobfuscating, index).translateSignature(methodMapping.getObfSignature()));
                }
            }

            for (FieldMapping fieldMapping : sorted(classMapping.fields())) {
                fieldMappings.add("FD: " + classMapping.getObfFullName() + "/" + fieldMapping.getObfName() + " " + classMapping.getDeobfName() + "/" + fieldMapping.getDeobfName());
            }

            for (MethodMapping methodMapping : sorted(classMapping.methods())) {
                methodMappings.add("MD: " + classMapping.getObfFullName() + "/" + methodMapping.getObfName() + " " + methodMapping.getObfSignature().toString() + " " + classMapping.getDeobfName() + "/" + methodMapping.getDeobfName() + " " + mappings.getTranslator(TranslationDirection.Deobfuscating, index).translateSignature(methodMapping.getObfSignature()));
            }
        }
        for(String fd : fieldMappings){
            writer.write(fd);
            writer.write(System.lineSeparator());
        }

        for(String md : methodMappings){
            writer.write(md);
            writer.write(System.lineSeparator());
        }


        writer.close();
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
