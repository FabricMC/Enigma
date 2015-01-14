package cuchaz.enigma;

public class CommandMain {
	
	public static void main(String[] args) {
		
		// parse the args
		if (args.length < 1) {
			printHelp();
			return;
		}
		
		// process the command
		String command = args[0];
		if (command.equalsIgnoreCase("deobfuscate")) {
			deobfuscate(args);
		} else if(command.equalsIgnoreCase("decompile")) {
			decompile(args);
		} else {
			System.out.println("Command not recognized: " + args[0]);
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println(String.format("%s - %s", Constants.Name, Constants.Version));
		System.out.println("Usage:");
		System.out.println("\tjava -jar enigma.jar cuchaz.enigma.CommandMain <command>");
		System.out.println("\twhere <command> is one of:");
		System.out.println("\t\tdeobfuscate <mappings file> <in jar> <out jar>");
		System.out.println("\t\tdecompile <mappings file> <in jar> <out jar>");
	}
	
	private static void decompile(String[] args) {
		// TODO
		throw new Error("Not implemented yet");
	}

	private static void deobfuscate(String[] args) {
		// TODO
		throw new Error("Not implemented yet");
	}
}
