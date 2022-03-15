package cuchaz.enigma.analysis.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class InnerClassIndex implements JarIndexer {
	private Multimap<ClassDefEntry, InnerClassData> innerClasses = ArrayListMultimap.create();
	private Map<ClassDefEntry, OuterClassData> outerClassesData = new HashMap<>();

	@Override
	public void indexInnerClass(ClassDefEntry classEntry, InnerClassData innerClassData) {
		innerClasses.put(classEntry, innerClassData);
	}

	@Override
	public void indexOuterClass(ClassDefEntry classEntry, OuterClassData outerClassData) {
		outerClassesData.put(classEntry, outerClassData);
	}

	private Optional<Map.Entry<ClassDefEntry, InnerClassData>> findInnerClassEntry(ClassEntry classEntry) {
		return innerClasses.entries().stream().filter(entry -> entry.getValue().name().equals(classEntry.getFullName())).findFirst();
	}

	public boolean isInnerClass(ClassEntry classEntry) {
		return findInnerClassEntry(classEntry).isPresent();
	}

	public InnerClassData getInnerClassData(ClassEntry classEntry) {
		return findInnerClassEntry(classEntry).map(Map.Entry::getValue).orElse(null);
	}

	public ClassDefEntry getOuterClass(ClassEntry classEntry) {
		return findInnerClassEntry(classEntry).map(Map.Entry::getKey).orElse(null);
	}

	private Optional<Map.Entry<ClassDefEntry, OuterClassData>> findOuterClassDataEntry(ClassEntry classEntry) {
		return outerClassesData.entrySet().stream().filter(entry -> entry.getKey().equals(classEntry)).findFirst();
	}

	public boolean hasOuterClassData(ClassEntry classEntry) {
		return findOuterClassDataEntry(classEntry).isPresent();
	}

	public OuterClassData getOuterClassData(ClassEntry classEntry) {
		return findOuterClassDataEntry(classEntry).map(Map.Entry::getValue).orElse(null);
	}
}
