/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cuchaz.enigma.command;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.MappingsChecker;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.serde.TinyMappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.io.*;
import java.util.jar.JarFile;

public class TinyifyCommand extends Command {
	public TinyifyCommand() {
		super("tinyify");
	}

	@Override
	public String getUsage() {
		return "<input-jar> <enigma-mappings> <output-tiny> [name-obf] [name-deobf]";
	}

	@Override
	public boolean isValidArgument(int count) {
		return count >= 3 && count <= 5;
	}

	@Override
	public void run(String[] args) throws Exception {
		File injf = new File(args[0]);
		File inf = new File(args[1]);
		File outf = new File(args[2]);
		String nameObf = args.length > 3 ? args[3] : "official";
		String nameDeobf = args.length > 4 ? args[4] : "named";

		if (!injf.exists() || !injf.isFile()) {
			throw new FileNotFoundException("Input JAR could not be found!");
		}

		if (!inf.exists()) {
			throw new FileNotFoundException("Enigma mappings could not be found!");
		}

		System.out.println("Reading JAR file...");

		JarIndex index = JarIndex.empty();
		index.indexJar(new ParsedJar(new JarFile(injf)), s -> {
		});

		System.out.println("Reading Enigma mappings...");
		MappingFormat format = inf.isDirectory() ? MappingFormat.ENIGMA_DIRECTORY : MappingFormat.ENIGMA_FILE;
		EntryTree<EntryMapping> mappings = format.read(inf.toPath(), ProgressListener.VOID);

		MappingsChecker checker = new MappingsChecker(index, mappings);
		checker.dropBrokenMappings(ProgressListener.VOID);

		System.out.println("Writing Tiny mappings...");

		MappingsWriter writer = new TinyMappingsWriter(nameObf, nameDeobf);
		writer.write(mappings, MappingDelta.added(mappings), outf.toPath(), ProgressListener.VOID);
	}

}
