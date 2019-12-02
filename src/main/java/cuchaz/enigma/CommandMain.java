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

package cuchaz.enigma;

import cuchaz.enigma.command.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CommandMain {

	private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

	public static void main(String... args) throws Exception {
		try {
			// process the command
			if (args.length < 1)
				throw new IllegalArgumentException("Requires a command");
			String command = args[0].toLowerCase(Locale.ROOT);

			Command cmd = COMMANDS.get(command);
			if (cmd == null)
				throw new IllegalArgumentException("Command not recognized: " + command);

			if (!cmd.isValidArgument(args.length - 1)) {
				throw new CommandHelpException(cmd);
			}

			String[] cmdArgs = new String[args.length - 1];
			System.arraycopy(args, 1, cmdArgs, 0, args.length - 1);

			try {
				cmd.run(cmdArgs);
			} catch (Exception ex) {
				throw new CommandHelpException(cmd, ex);
			}
		} catch (CommandHelpException ex) {
			System.err.println(ex.getMessage());
			System.out.println(String.format("%s - %s", Constants.NAME, Constants.VERSION));
			System.out.println("Command " + ex.command.name + " has encountered an error! Usage:");
			printHelp(ex.command);
			System.exit(1);
		} catch (IllegalArgumentException ex) {
			System.err.println(ex.getMessage());
			printHelp();
			System.exit(1);
		}
	}

	private static void printHelp() {
		System.out.println(String.format("%s - %s", Constants.NAME, Constants.VERSION));
		System.out.println("Usage:");
		System.out.println("\tjava -cp enigma.jar cuchaz.enigma.CommandMain <command>");
		System.out.println("\twhere <command> is one of:");

		for (Command command : COMMANDS.values()) {
			printHelp(command);
		}
	}

	private static void printHelp(Command command) {
		System.out.println("\t\t" + command.name + " " + command.getUsage());
	}

	private static void register(Command command) {
		Command old = COMMANDS.put(command.name, command);
		if (old != null) {
			System.err.println("Command " + old + " with name " + command.name + " has been substituted by " + command);
		}
	}

	static {
		register(new DeobfuscateCommand());
		register(new DecompileCommand());
		register(new ConvertMappingsCommand());
		register(new ComposeMappingsCommand());
		register(new InvertMappingsCommand());
		register(new CheckMappingsCommand());
		register(new MapSpecializedMethodsCommand());
	}

	private static final class CommandHelpException extends IllegalArgumentException {

		final Command command;

		CommandHelpException(Command command) {
			this.command = command;
		}

		CommandHelpException(Command command, Throwable cause) {
			super(cause);
			this.command = command;
		}
	}
}
