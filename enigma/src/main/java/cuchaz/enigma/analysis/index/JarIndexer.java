package cuchaz.enigma.analysis.index;

import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public interface JarIndexer {
	default void indexClass(ClassDefEntry classEntry) {
	}

	default void indexField(FieldDefEntry fieldEntry) {
	}

	default void indexMethod(MethodDefEntry methodEntry) {
	}

	default void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
	}

	default void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
	}

	default void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
	}

	default void indexInnerClass(ClassDefEntry classEntry, InnerClassData innerClassData) {
	}

	default void indexOuterClass(ClassDefEntry classEntry, OuterClassData outerClassData) {
	}

	default void processIndex(JarIndex index) {
	}

	record InnerClassData(String name, String innerName, String outerName, int access) {
		public boolean hasInnerName() {
			return innerName != null;
		}

		public boolean hasOuterName() {
			return outerName != null;
		}
	}

	record OuterClassData(String owner, String name, String descriptor) {
		public boolean hasEnclosingMethod() {
			return name != null && descriptor != null;
		}
	}
}
