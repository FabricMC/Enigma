package cuchaz.enigma.mapping;

import com.google.common.base.Charsets;
import com.google.common.collect.Queues;
import cuchaz.enigma.throwables.MappingConflict;
import cuchaz.enigma.throwables.MappingParseException;

import java.io.*;
import java.util.Deque;

public class MappingsEnigmaReader
{

    public Mappings read(File file) throws IOException, MappingParseException {
        Mappings mappings;

        // Multiple file
        if (file.isDirectory())
        {
            mappings = new Mappings(Mappings.FormatType.ENIGMA_DIRECTORY);
            readDirectory(mappings, file);
        }
        else
        {
            mappings = new Mappings();
            readFile(mappings, file);
        }
        return mappings;
    }

    public void readDirectory(Mappings mappings, File directory) throws IOException, MappingParseException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(".mapping"))
                    readFile(mappings, file);
                else if (file.isDirectory())
                    readDirectory(mappings, file.getAbsoluteFile());
            }
            mappings.savePreviousState();
        }
        else
            throw new IOException("Cannot access directory" + directory.getAbsolutePath());
    }

    public Mappings readFile(Mappings mappings, File file) throws IOException, MappingParseException {

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
        Deque<Object> mappingStack = Queues.newArrayDeque();

        int lineNumber = 0;
        String line;
        while ((line = in.readLine()) != null) {
            lineNumber++;

            // strip comments
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) {
                line = line.substring(0, commentPos);
            }

            // skip blank lines
            if (line.trim().length() <= 0) {
                continue;
            }

            // get the indent of this line
            int indent = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) != '\t') {
                    break;
                }
                indent++;
            }

            // handle stack pops
            while (indent < mappingStack.size()) {
                mappingStack.pop();
            }

            String[] parts = line.trim().split("\\s");
            try {
                // read the first token
                String token = parts[0];

                if (token.equalsIgnoreCase("CLASS")) {
                    ClassMapping classMapping;
                    if (indent <= 0) {
                        // outer class
                        classMapping = readClass(parts, false);
                        mappings.addClassMapping(classMapping);
                    } else {

                        // inner class
                        if (!(mappingStack.peek() instanceof ClassMapping)) {
                            throw new MappingParseException(file, lineNumber, "Unexpected CLASS entry here!");
                        }

                        classMapping = readClass(parts, true);
                        ((ClassMapping) mappingStack.peek()).addInnerClassMapping(classMapping);
                    }
                    mappingStack.push(classMapping);
                } else if (token.equalsIgnoreCase("FIELD")) {
                    if (mappingStack.isEmpty() || !(mappingStack.peek() instanceof ClassMapping)) {
                        throw new MappingParseException(file, lineNumber, "Unexpected FIELD entry here!");
                    }
                    ((ClassMapping) mappingStack.peek()).addFieldMapping(readField(parts));
                } else if (token.equalsIgnoreCase("METHOD")) {
                    if (mappingStack.isEmpty() || !(mappingStack.peek() instanceof ClassMapping)) {
                        throw new MappingParseException(file, lineNumber, "Unexpected METHOD entry here!");
                    }
                    MethodMapping methodMapping = readMethod(parts);
                    ((ClassMapping) mappingStack.peek()).addMethodMapping(methodMapping);
                    mappingStack.push(methodMapping);
                } else if (token.equalsIgnoreCase("ARG")) {
                    if (mappingStack.isEmpty() || !(mappingStack.peek() instanceof MethodMapping)) {
                        throw new MappingParseException(file, lineNumber, "Unexpected ARG entry here!");
                    }
                    ((MethodMapping) mappingStack.peek()).addArgumentMapping(readArgument(parts));
                }
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
                throw new MappingParseException(file, lineNumber, "Malformed line:\n" + line);
            } catch (MappingConflict e) {
                throw new MappingParseException(file, lineNumber, e.getMessage());
            }
        }
        in.close();
        return mappings;
    }

    private ArgumentMapping readArgument(String[] parts) {
        return new ArgumentMapping(Integer.parseInt(parts[1]), parts[2]);
    }

    private ClassMapping readClass(String[] parts, boolean makeSimple) {
        if (parts.length == 2) {
            return new ClassMapping(parts[1]);
        } else {
            return new ClassMapping(parts[1], parts[2]);
        }
    }

    /* TEMP */
    protected FieldMapping readField(String[] parts) {
        return new FieldMapping(parts[1], new Type(parts[3]), parts[2]);
    }

    private MethodMapping readMethod(String[] parts) {
        if (parts.length == 3) {
            return new MethodMapping(parts[1], new Signature(parts[2]));
        } else {
            return new MethodMapping(parts[1], new Signature(parts[3]), parts[2]);
        }
    }
}
