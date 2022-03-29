package cuchaz.enigma.command;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.ProposingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;

import javax.annotation.Nullable;
import java.nio.file.Path;

public class InsertProposedMappingsCommand extends Command {
    public InsertProposedMappingsCommand() {
        super("invert-proposed-mappings");
    }

    @Override
    public String getUsage() {
        return "<in jar> <source> <result> <result-format> [<profile>]";
    }

    @Override
    public boolean isValidArgument(int length) {
        return length == 4 || length == 5;
    }

    @Override
    public void run(String... args) throws Exception {
        Path inJar = getReadablePath(getArg(args, 0, "in jar", true));
        Path source = getReadablePath(getArg(args, 1, "source", true));
        //noinspection ConstantConditions
        Path output = Path.of(getArg(args, 2, "result", true));
        String resultFormat = getArg(args, 3, "result-format", true);
        Path profilePath = getReadablePath(getArg(args, 4, "path", false));

        run(inJar, source, output, resultFormat, profilePath, null);
    }

    public void run(Path inJar, Path source, Path output, String resultFormat, @Nullable Path profilePath, @Nullable Iterable<EnigmaPlugin> plugins) throws Exception {
        EnigmaProfile profile = EnigmaProfile.read(profilePath);
        Enigma.Builder builder = Enigma.builder().setProfile(profile);

        if (plugins != null) {
            builder.setPlugins(plugins);
        }

        Enigma enigma = builder.build();

        NameProposalService[] nameProposalServices = enigma.getServices().get(NameProposalService.TYPE).toArray(new NameProposalService[0]);
        if (nameProposalServices.length == 0) {
            System.err.println("No name proposal service found");
            return;
        }

        System.out.println("Reading JAR...");

        EnigmaProject project = enigma.openJar(inJar, new ClasspathClassProvider(), ProgressListener.none());

        System.out.println("Reading mappings...");

        MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

        EntryTree<EntryMapping> mappings = readMappings(source, ProgressListener.none(), saveParameters);
        project.setMappings(mappings);

        EntryRemapper mapper = project.getMapper();
        Translator translator = new ProposingTranslator(mapper, nameProposalServices);
        EntryIndex index = project.getJarIndex().getEntryIndex();

        System.out.println("Proposing class names...");
        int classes = 0;
        for (ClassEntry clazz : index.getClasses()) {
            ClassEntry deobf = mapper.extendedDeobfuscate(clazz).getValue();
            String name = translator.extendedTranslate(clazz).getValue().getName();
            if (!deobf.getName().equals(name) && !clazz.getName().equals(name)) {
                String javadoc = deobf.getJavadocs();
                EntryMapping mapping = javadoc != null && !javadoc.isEmpty() ? new EntryMapping(name, javadoc) : new EntryMapping(name);
                mappings.insert(clazz, mapping);
                classes++;
            }
        }

        System.out.println("Proposing field names...");
        int fields = 0;
        for (FieldEntry field : index.getFields()) {
            FieldEntry deobf = mapper.extendedDeobfuscate(field).getValue();
            String name = translator.extendedTranslate(field).getValue().getName();
            if (!deobf.getName().equals(name) && !field.getName().equals(name)) {
                String javadoc = deobf.getJavadocs();
                EntryMapping mapping = javadoc != null && !javadoc.isEmpty() ? new EntryMapping(name, javadoc) : new EntryMapping(name);
                mappings.insert(field, mapping);
                fields++;
            }
        }

        System.out.println("Proposing method names...");
        int methods = 0;
        for (MethodEntry method : index.getMethods()) {
            MethodEntry deobf = mapper.extendedDeobfuscate(method).getValue();
            String name = translator.extendedTranslate(method).getValue().getName();
            if (!deobf.getName().equals(name) && !method.getName().equals(name)) {
                String javadoc = deobf.getJavadocs();
                EntryMapping mapping = javadoc != null && !javadoc.isEmpty() ? new EntryMapping(name, javadoc) : new EntryMapping(name);
                mappings.insert(method, mapping);
                methods++;
            }
        }

        System.out.println("Proposed names for " + classes + " classes, " + fields + " fields, " + methods + " methods");

        Utils.delete(output);
        MappingCommandsUtil.write(mappings, resultFormat, output, saveParameters);
    }
}
