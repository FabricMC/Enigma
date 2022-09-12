/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation.representation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.collect.Lists;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class MethodDescriptor implements Translatable {
	private List<TypeDescriptor> argumentDescs;
	private TypeDescriptor returnDesc;

	public MethodDescriptor(String desc) {
		try {
			this.argumentDescs = Lists.newArrayList();
			int i = 0;

			while (i < desc.length()) {
				char c = desc.charAt(i);

				if (c == '(') {
					assert (this.argumentDescs.isEmpty());
					assert (this.returnDesc == null);
					i++;
				} else if (c == ')') {
					i++;
					break;
				} else {
					String type = TypeDescriptor.parseFirst(desc.substring(i));
					this.argumentDescs.add(new TypeDescriptor(type));
					i += type.length();
				}
			}

			this.returnDesc = new TypeDescriptor(TypeDescriptor.parseFirst(desc.substring(i)));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to parse method descriptor: " + desc, ex);
		}
	}

	public MethodDescriptor(List<TypeDescriptor> argumentDescs, TypeDescriptor returnDesc) {
		this.argumentDescs = argumentDescs;
		this.returnDesc = returnDesc;
	}

	public List<TypeDescriptor> getArgumentDescs() {
		return this.argumentDescs;
	}

	public TypeDescriptor getReturnDesc() {
		return this.returnDesc;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("(");

		for (TypeDescriptor desc : this.argumentDescs) {
			buf.append(desc);
		}

		buf.append(")");
		buf.append(this.returnDesc);
		return buf.toString();
	}

	public Iterable<TypeDescriptor> types() {
		List<TypeDescriptor> descs = Lists.newArrayList();
		descs.addAll(this.argumentDescs);
		descs.add(this.returnDesc);
		return descs;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodDescriptor && equals((MethodDescriptor) other);
	}

	public boolean equals(MethodDescriptor other) {
		return this.argumentDescs.equals(other.argumentDescs) && this.returnDesc.equals(other.returnDesc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.argumentDescs.hashCode(), this.returnDesc.hashCode());
	}

	public boolean hasClass(ClassEntry classEntry) {
		for (TypeDescriptor desc : types()) {
			if (desc.containsType() && desc.getTypeEntry().equals(classEntry)) {
				return true;
			}
		}

		return false;
	}

	public MethodDescriptor remap(Function<String, String> remapper) {
		List<TypeDescriptor> argumentDescs = new ArrayList<>(this.argumentDescs.size());

		for (TypeDescriptor desc : this.argumentDescs) {
			argumentDescs.add(desc.remap(remapper));
		}

		return new MethodDescriptor(argumentDescs, returnDesc.remap(remapper));
	}

	@Override
	public TranslateResult<MethodDescriptor> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		List<TypeDescriptor> translatedArguments = new ArrayList<>(argumentDescs.size());

		for (TypeDescriptor argument : argumentDescs) {
			translatedArguments.add(translator.translate(argument));
		}

		return TranslateResult.ungrouped(new MethodDescriptor(translatedArguments, translator.translate(returnDesc)));
	}

	public boolean canConflictWith(MethodDescriptor descriptor) {
		return descriptor.argumentDescs.equals(argumentDescs);
	}
}
