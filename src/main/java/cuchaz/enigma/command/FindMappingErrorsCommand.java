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

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.analysis.index.ReferenceIndex;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.File;
import java.util.*;
import java.util.jar.JarFile;

public class FindMappingErrorsCommand extends Command {
	public FindMappingErrorsCommand() {
		super("findMappingErrors");
	}

	@Override
	public String getUsage() {
		return "<obf jar> <mappings>";
	}

	@Override
	public boolean isValidArgument(int count) {
		return count == 2;
	}

	private void addError(SortedMap<String, Set<String>> errorStrings, String error, String cause) {
		if (!errorStrings.containsKey(error)) {
			errorStrings.put(error, new HashSet<>());
		}
		errorStrings.get(error).add(cause);
	}

	private boolean isRefValid(AccessFlags entryAcc, EntryReference ref, Deobfuscator deobfuscator) {
		EntryReference refDeobf = deobfuscator.getMapper().deobfuscate(ref);
		String packageCtx = refDeobf.context.getContainingClass().getPackageName();
		String packageEntry = refDeobf.entry.getContainingClass().getPackageName();
		boolean samePackage = (packageCtx == null && packageEntry == null) || (packageCtx != null && packageCtx.equals(packageEntry));
		if (samePackage) {
			return true;
		} else if (entryAcc.isProtected()) {
			// TODO: Is this valid?
			InheritanceIndex inheritanceIndex = deobfuscator.getJarIndex().getInheritanceIndex();

			for (ClassEntry outerClass : getOuterClasses(ref.context.getContainingClass())) {
				Set<ClassEntry> callerAncestors = inheritanceIndex.getAncestors(outerClass);
				if (callerAncestors.contains(ref.entry.getContainingClass())) {
					return true;
				}
			}
		}

		return false;
	}

	private Collection<ClassEntry> getOuterClasses(ClassEntry entry) {
		Collection<ClassEntry> outerClasses = new ArrayList<>();

		ClassEntry currentEntry = entry;
		while (currentEntry != null) {
			outerClasses.add(currentEntry);
			currentEntry = currentEntry.getOuterClass();
		}

		return outerClasses;
	}

	@Override
	public void run(String[] args) throws Exception {
		File fileJarIn = new File(args[0]);
		File fileMappings = new File(args[1]);

		System.out.println("Reading JAR...");
		Deobfuscator deobfuscator = new Deobfuscator(new JarFile(fileJarIn));
		System.out.println("Reading mappings...");

		MappingFormat format = fileMappings.isDirectory() ? MappingFormat.ENIGMA_DIRECTORY : MappingFormat.ENIGMA_FILE;
		EntryTree<EntryMapping> mappings = format.read(fileMappings.toPath(), ProgressListener.VOID);
		deobfuscator.setMappings(mappings);

		JarIndex idx = deobfuscator.getJarIndex();
		EntryIndex entryIndex = idx.getEntryIndex();
		ReferenceIndex referenceIndex = idx.getReferenceIndex();

		EntryRemapper mapper = deobfuscator.getMapper();

		SortedMap<String, Set<String>> errorStrings = new TreeMap<>();

		for (FieldEntry entry : entryIndex.getFields()) {
			AccessFlags entryAcc = entryIndex.getFieldAccess(entry);
			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<FieldEntry, MethodDefEntry> ref : referenceIndex.getReferencesToField(entry)) {
					boolean valid = isRefValid(entryAcc, ref, deobfuscator);

					if (!valid) {
						EntryReference<FieldEntry, MethodDefEntry> refDeobf = mapper.deobfuscate(ref);
						addError(errorStrings, "ERROR: Must be in one package: " + refDeobf.context.getContainingClass() + " and " + refDeobf.entry.getContainingClass(), "field " + refDeobf.entry.getName());
					}
				}
			}
		}

		for (MethodEntry entry : entryIndex.getMethods()) {
			AccessFlags entryAcc = entryIndex.getMethodAccess(entry);
			if (!entryAcc.isPublic() && !entryAcc.isPrivate()) {
				for (EntryReference<MethodEntry, MethodDefEntry> ref : referenceIndex.getReferencesToMethod(entry)) {
					boolean valid = isRefValid(entryAcc, ref, deobfuscator);

					if (!valid) {
						EntryReference<MethodEntry, MethodDefEntry> refDeobf = mapper.deobfuscate(ref);
						addError(errorStrings, "ERROR: Must be in one package: " + refDeobf.context.getContainingClass() + " and " + refDeobf.entry.getContainingClass(), "method " + refDeobf.entry.getName());
					}
				}
			}
		}

		for (String s : errorStrings.keySet()) {
			System.out.println(s + " (" + String.join(", ", errorStrings.get(s)) + ")");
		}
	}
}
