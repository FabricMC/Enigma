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
package cuchaz.enigma.mapping;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cuchaz.enigma.analysis.TranslationIndex;

public class Translator {
	
	private TranslationDirection m_direction;
	private Map<String,ClassMapping> m_classes;
	private TranslationIndex m_index;
	
	private ClassNameReplacer m_classNameReplacer = new ClassNameReplacer() {
		@Override
		public String replace(String className) {
			return translateEntry(new ClassEntry(className)).getName();
		}
	};
	
	public Translator() {
		m_direction = null;
		m_classes = Maps.newHashMap();
		m_index = new TranslationIndex();
	}
	
	public Translator(TranslationDirection direction, Map<String,ClassMapping> classes, TranslationIndex index) {
		m_direction = direction;
		m_classes = classes;
		m_index = index;
	}
	
	public TranslationDirection getDirection() {
		return m_direction;
	}
	
	public TranslationIndex getTranslationIndex() {
		return m_index;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Entry> T translateEntry(T entry) {
		if (entry instanceof ClassEntry) {
			return (T)translateEntry((ClassEntry)entry);
		} else if (entry instanceof FieldEntry) {
			return (T)translateEntry((FieldEntry)entry);
		} else if (entry instanceof MethodEntry) {
			return (T)translateEntry((MethodEntry)entry);
		} else if (entry instanceof ConstructorEntry) {
			return (T)translateEntry((ConstructorEntry)entry);
		} else if (entry instanceof ArgumentEntry) {
			return (T)translateEntry((ArgumentEntry)entry);
		} else {
			throw new Error("Unknown entry type: " + entry.getClass().getName());
		}
	}
	
	public <T extends Entry> String translate(T entry) {
		if (entry instanceof ClassEntry) {
			return translate((ClassEntry)entry);
		} else if (entry instanceof FieldEntry) {
			return translate((FieldEntry)entry);
		} else if (entry instanceof MethodEntry) {
			return translate((MethodEntry)entry);
		} else if (entry instanceof ConstructorEntry) {
			return translate((ConstructorEntry)entry);
		} else if (entry instanceof ArgumentEntry) {
			return translate((ArgumentEntry)entry);
		} else {
			throw new Error("Unknown entry type: " + entry.getClass().getName());
		}
	}
	
	public String translate(ClassEntry in) {
		ClassEntry translated = translateEntry(in);
		if (translated.equals(in)) {
			return null;
		}
		return translated.getName();
	}
	
	public String translateClass(String className) {
		return translate(new ClassEntry(className));
	}
	
	public ClassEntry translateEntry(ClassEntry in) {
		
		if (in.isInnerClass()) {
			
			// translate as much of the class chain as we can
			List<ClassMapping> mappingsChain = getClassMappingChain(in);
			String[] obfClassNames = in.getName().split("\\$");
			StringBuilder buf = new StringBuilder();
			for (int i=0; i<obfClassNames.length; i++) {
				boolean isFirstClass = buf.length() == 0;
				String className = null;
				ClassMapping classMapping = mappingsChain.get(i);
				if (classMapping != null) {
					className = m_direction.choose(
						classMapping.getDeobfName(),
						isFirstClass ? classMapping.getObfFullName() : classMapping.getObfSimpleName()
					);
				}
				if (className == null) {
					className = obfClassNames[i];
				}
				if (!isFirstClass) {
					buf.append("$");
				}
				buf.append(className);
			}
			return new ClassEntry(buf.toString());
			
		} else {
			
			// normal classes are easy
			ClassMapping classMapping = m_classes.get(in.getName());
			if (classMapping == null) {
				return in;
			}
			return m_direction.choose(
				classMapping.getDeobfName() != null ? new ClassEntry(classMapping.getDeobfName()) : in,
				new ClassEntry(classMapping.getObfFullName())
			);
		}
	}
	
	public String translate(FieldEntry in) {
		
		// resolve the class entry
		ClassEntry resolvedClassEntry = m_index.resolveEntryClass(in);
		if (resolvedClassEntry != null) {
		
			// look for the class
			ClassMapping classMapping = findClassMapping(resolvedClassEntry);
			if (classMapping != null) {
				
				// look for the field
				String translatedName = m_direction.choose(
					classMapping.getDeobfFieldName(in.getName(), in.getType()),
					classMapping.getObfFieldName(in.getName(), translateType(in.getType()))
				);
				if (translatedName != null) {
					return translatedName;
				}
			}
		}
		return null;
	}
	
	public FieldEntry translateEntry(FieldEntry in) {
		String name = translate(in);
		if (name == null) {
			name = in.getName();
		}
		return new FieldEntry(translateEntry(in.getClassEntry()), name, translateType(in.getType()));
	}
	
	public String translate(MethodEntry in) {
		
		// resolve the class entry
		ClassEntry resolvedClassEntry = m_index.resolveEntryClass(in);
		if (resolvedClassEntry != null) {
		
			// look for class
			ClassMapping classMapping = findClassMapping(resolvedClassEntry);
			if (classMapping != null) {
				
				// look for the method
				MethodMapping methodMapping = m_direction.choose(
					classMapping.getMethodByObf(in.getName(), in.getSignature()),
					classMapping.getMethodByDeobf(in.getName(), translateSignature(in.getSignature()))
				);
				if (methodMapping != null) {
					return m_direction.choose(methodMapping.getDeobfName(), methodMapping.getObfName());
				}
			}
		}
		return null;
	}
	
	public MethodEntry translateEntry(MethodEntry in) {
		String name = translate(in);
		if (name == null) {
			name = in.getName();
		}
		return new MethodEntry(translateEntry(in.getClassEntry()), name, translateSignature(in.getSignature()));
	}
	
	public ConstructorEntry translateEntry(ConstructorEntry in) {
		if (in.isStatic()) {
			return new ConstructorEntry(translateEntry(in.getClassEntry()));
		} else {
			return new ConstructorEntry(translateEntry(in.getClassEntry()), translateSignature(in.getSignature()));
		}
	}
	
	public BehaviorEntry translateEntry(BehaviorEntry in) {
		if (in instanceof MethodEntry) {
			return translateEntry((MethodEntry)in);
		} else if (in instanceof ConstructorEntry) {
			return translateEntry((ConstructorEntry)in);
		}
		throw new Error("Wrong entry type!");
	}
	
	public String translate(ArgumentEntry in) {
		
		// look for the class
		ClassMapping classMapping = findClassMapping(in.getClassEntry());
		if (classMapping != null) {
			
			// look for the method
			MethodMapping methodMapping = m_direction.choose(
				classMapping.getMethodByObf(in.getMethodName(), in.getMethodSignature()),
				classMapping.getMethodByDeobf(in.getMethodName(), translateSignature(in.getMethodSignature()))
			);
			if (methodMapping != null) {
				return m_direction.choose(
					methodMapping.getDeobfArgumentName(in.getIndex()),
					methodMapping.getObfArgumentName(in.getIndex())
				);
			}
		}
		return null;
	}
	
	public ArgumentEntry translateEntry(ArgumentEntry in) {
		String name = translate(in);
		if (name == null) {
			name = in.getName();
		}
		return new ArgumentEntry(translateEntry(in.getBehaviorEntry()), in.getIndex(), name);
	}
	
	public Type translateType(Type type) {
		return new Type(type, m_classNameReplacer);
	}
	
	public Signature translateSignature(Signature signature) {
		return new Signature(signature, m_classNameReplacer);
	}
	
	private ClassMapping findClassMapping(ClassEntry in) {
		List<ClassMapping> mappingChain = getClassMappingChain(in);
		return mappingChain.get(mappingChain.size() - 1);
	}
	
	private List<ClassMapping> getClassMappingChain(ClassEntry in) {
		
		// get a list of all the classes in the hierarchy
		String[] parts = in.getName().split("\\$");
		List<ClassMapping> mappingsChain = Lists.newArrayList();
		
		// get mappings for the outer class
		ClassMapping outerClassMapping = m_classes.get(parts[0]);
		mappingsChain.add(outerClassMapping);
		
		for (int i=1; i<parts.length; i++) {
			
			// get mappings for the inner class
			ClassMapping innerClassMapping = null;
			if (outerClassMapping != null) {
				innerClassMapping = m_direction.choose(
					outerClassMapping.getInnerClassByObfSimple(parts[i]),
					outerClassMapping.getInnerClassByDeobfThenObfSimple(parts[i])
				);
			}
			mappingsChain.add(innerClassMapping);
			outerClassMapping = innerClassMapping;
		}
		
		assert(mappingsChain.size() == parts.length);
		return mappingsChain;
	}
}
