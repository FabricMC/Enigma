/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Type;

public class EntryRenamer {
	
	public static <T> void renameClassesInSet(Map<String,String> renames, Set<T> set) {
		List<T> entries = Lists.newArrayList();
		for (T val : set) {
			entries.add(renameClassesInThing(renames, val));
		}
		set.clear();
		set.addAll(entries);
	}
	
	public static <Key,Val> void renameClassesInMap(Map<String,String> renames, Map<Key,Val> map) {
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		for (Map.Entry<Key,Val> entry : map.entrySet()) {
			entriesToAdd.add(new AbstractMap.SimpleEntry<Key,Val>(
				renameClassesInThing(renames, entry.getKey()),
				renameClassesInThing(renames, entry.getValue())
			));
		}
		map.clear();
		for (Map.Entry<Key,Val> entry : entriesToAdd) {
			map.put(entry.getKey(), entry.getValue());
		}
	}
	
	public static <Key,Val> void renameClassesInMultimap(Map<String,String> renames, Multimap<Key,Val> map) {
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		for (Map.Entry<Key,Val> entry : map.entries()) {
			entriesToAdd.add(new AbstractMap.SimpleEntry<Key,Val>(
				renameClassesInThing(renames, entry.getKey()),
				renameClassesInThing(renames, entry.getValue())
			));
		}
		map.clear();
		for (Map.Entry<Key,Val> entry : entriesToAdd) {
			map.put(entry.getKey(), entry.getValue());
		}
	}
	
	public static <Key,Val> void renameMethodsInMultimap(Map<MethodEntry,MethodEntry> renames, Multimap<Key,Val> map) {
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		for (Map.Entry<Key,Val> entry : map.entries()) {
			entriesToAdd.add(new AbstractMap.SimpleEntry<Key,Val>(
				renameMethodsInThing(renames, entry.getKey()),
				renameMethodsInThing(renames, entry.getValue())
			));
		}
		map.clear();
		for (Map.Entry<Key,Val> entry : entriesToAdd) {
			map.put(entry.getKey(), entry.getValue());
		}
	}
	
	public static <Key,Val> void renameMethodsInMap(Map<MethodEntry,MethodEntry> renames, Map<Key,Val> map) {
		// for each key/value pair...
		Set<Map.Entry<Key,Val>> entriesToAdd = Sets.newHashSet();
		for (Map.Entry<Key,Val> entry : map.entrySet()) {
			entriesToAdd.add(new AbstractMap.SimpleEntry<Key,Val>(
				renameMethodsInThing(renames, entry.getKey()),
				renameMethodsInThing(renames, entry.getValue())
			));
		}
		map.clear();
		for (Map.Entry<Key,Val> entry : entriesToAdd) {
			map.put(entry.getKey(), entry.getValue());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T renameMethodsInThing(Map<MethodEntry,MethodEntry> renames, T thing) {
		if (thing instanceof MethodEntry) {
			MethodEntry methodEntry = (MethodEntry)thing;
			MethodEntry newMethodEntry = renames.get(methodEntry);
			if (newMethodEntry != null) {
				return (T)new MethodEntry(
					methodEntry.getClassEntry(),
					newMethodEntry.getName(),
					methodEntry.getSignature()
				);
			}
			return thing;
		} else if (thing instanceof ArgumentEntry) {
			ArgumentEntry argumentEntry = (ArgumentEntry)thing;
			return (T)new ArgumentEntry(
				renameMethodsInThing(renames, argumentEntry.getBehaviorEntry()),
				argumentEntry.getIndex(),
				argumentEntry.getName()
			);
		} else if (thing instanceof EntryReference) {
			EntryReference<Entry,Entry> reference = (EntryReference<Entry,Entry>)thing;
			reference.entry = renameMethodsInThing(renames, reference.entry);
			reference.context = renameMethodsInThing(renames, reference.context);
			return thing;
		}
		return thing;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T renameClassesInThing(final Map<String,String> renames, T thing) {
		if (thing instanceof String) {
			String stringEntry = (String)thing;
			if (renames.containsKey(stringEntry)) {
				return (T)renames.get(stringEntry);
			}
		} else if (thing instanceof ClassEntry) {
			ClassEntry classEntry = (ClassEntry)thing;
			return (T)new ClassEntry(renameClassesInThing(renames, classEntry.getClassName()));
		} else if (thing instanceof FieldEntry) {
			FieldEntry fieldEntry = (FieldEntry)thing;
			return (T)new FieldEntry(
				renameClassesInThing(renames, fieldEntry.getClassEntry()),
				fieldEntry.getName(),
				renameClassesInThing(renames, fieldEntry.getType())
			);
		} else if (thing instanceof ConstructorEntry) {
			ConstructorEntry constructorEntry = (ConstructorEntry)thing;
			return (T)new ConstructorEntry(
				renameClassesInThing(renames, constructorEntry.getClassEntry()),
				renameClassesInThing(renames, constructorEntry.getSignature())
			);
		} else if (thing instanceof MethodEntry) {
			MethodEntry methodEntry = (MethodEntry)thing;
			return (T)new MethodEntry(
				renameClassesInThing(renames, methodEntry.getClassEntry()),
				methodEntry.getName(),
				renameClassesInThing(renames, methodEntry.getSignature())
			);
		} else if (thing instanceof ArgumentEntry) {
			ArgumentEntry argumentEntry = (ArgumentEntry)thing;
			return (T)new ArgumentEntry(
				renameClassesInThing(renames, argumentEntry.getBehaviorEntry()),
				argumentEntry.getIndex(),
				argumentEntry.getName()
			);
		} else if (thing instanceof EntryReference) {
			EntryReference<Entry,Entry> reference = (EntryReference<Entry,Entry>)thing;
			reference.entry = renameClassesInThing(renames, reference.entry);
			reference.context = renameClassesInThing(renames, reference.context);
			return thing;
		} else if (thing instanceof Signature) {
			return (T)new Signature((Signature)thing, new ClassNameReplacer() {
				@Override
				public String replace(String className) {
					return renameClassesInThing(renames, className);
				}
			});
		} else if (thing instanceof Type) {
			return (T)new Type((Type)thing, new ClassNameReplacer() {
				@Override
				public String replace(String className) {
					return renameClassesInThing(renames, className);
				}
			});
		}
		
		return thing;
	}
}
