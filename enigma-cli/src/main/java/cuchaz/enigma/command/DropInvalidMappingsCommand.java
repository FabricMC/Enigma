package cuchaz.enigma.command;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DropInvalidMappingsCommand extends Command {
    public DropInvalidMappingsCommand() {
        super("dropinvalidmappings");
    }

    @Override
    public String getUsage() {
        return "<in jar> <mappings in> [<mappings out>]";
    }

    @Override
    public boolean isValidArgument(int length) {
        return length==3;
    }

    @Override
    public void run(String... args) throws Exception {
        Path fileJarIn = getReadableFile(getArg(args, 0, "in jar", true)).toPath();
        Path fileMappingsIn = getReadablePath(getArg(args, 1, "mappings in", true));
        if (fileMappingsIn == null) {
            System.out.println("No mappings input specified, skipping.");
            return;
        }

        String mappingsOut = getArg(args, 2, "mappings out", false);
        Path fileMappingsOut = mappingsOut != null && !mappingsOut.isEmpty() ? getReadablePath(mappingsOut) : fileMappingsIn;

        Enigma enigma = Enigma.create();

        System.out.println("Reading JAR...");

        EnigmaProject project = enigma.openJar(fileJarIn, new ClasspathClassProvider(), ProgressListener.none());

        System.out.println("Reading mappings...");

        MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

        EntryTree<EntryMapping> mappings = readMappings(fileMappingsIn, ProgressListener.none(), saveParameters);
        project.setMappings(mappings);

        System.out.println("Dropping invalid mappings...");

        project.dropMappings(ProgressListener.none());

        System.out.println("Writing mappings...");

        if (fileMappingsOut == fileMappingsIn) {
            System.out.println("Overwriting input mappings");
            Files.walkFileTree(fileMappingsIn, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult postVisitDirectory(
                        Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });

            Files.deleteIfExists(fileMappingsIn);
        }

        writeMappings(project.getMapper().getObfToDeobf(), fileMappingsOut, ProgressListener.none(), saveParameters);
    }
}
