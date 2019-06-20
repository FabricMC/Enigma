package cuchaz.enigma.command;

import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ComposeMappingsCommand extends Command {
    public ComposeMappingsCommand() {
        super("compose-mappings");
    }

    @Override
    public String getUsage() {
        return "<left-format> <left> <right-format> <right> <result-format> <result> <keep-mode>";
    }

    @Override
    public boolean isValidArgument(int length) {
        return length == 7;
    }

    @Override
    public void run(String... args) throws IOException, MappingParseException {
        EntryTree<EntryMapping> left = MappingCommandsUtil.read(args[0], Paths.get(args[1]));
        EntryTree<EntryMapping> right = MappingCommandsUtil.read(args[2], Paths.get(args[3]));
        EntryTree<EntryMapping> result = MappingCommandsUtil.compose(left, right, args[6].equals("left") || args[6].equals("both"), args[6].equals("right") || args[6].equals("both"));

        Path output = Paths.get(args[5]);
        Utils.delete(output);
        MappingCommandsUtil.write(result, args[4], output);
    }
}
