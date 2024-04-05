package cuchaz.enigma.source.vineflower;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.Pair;
import org.jetbrains.java.decompiler.util.token.TextRange;

import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

class EnigmaTextTokenCollector extends TextTokenVisitor {
	private final Map<Token, Entry<?>> declarations = new HashMap<>();
	private final Map<Token, Pair<Entry<?>, Entry<?>>> references = new HashMap<>();
	private final Set<Token> tokens = new LinkedHashSet<>();
	private String content;
	private MethodEntry currentMethod;

	EnigmaTextTokenCollector(TextTokenVisitor next) {
		super(next);
	}

	@Override
	public void start(String content) {
		this.content = content;
	}

	@Override
	public void visitClass(TextRange range, boolean declaration, String name) {
		super.visitClass(range, declaration, name);
		Token token = getToken(range);

		if (declaration) {
			addDeclaration(token, classEntryOf(name));
		} else {
			addReference(token, classEntryOf(name), currentMethod);
		}
	}

	@Override
	public void visitField(TextRange range, boolean declaration, String className, String name, FieldDescriptor descriptor) {
		super.visitField(range, declaration, className, name, descriptor);
		Token token = getToken(range);

		if (declaration) {
			addDeclaration(token, fieldEntryOf(className, name, descriptor));
		} else {
			addReference(token, fieldEntryOf(className, name, descriptor), currentMethod);
		}
	}

	@Override
	public void visitMethod(TextRange range, boolean declaration, String className, String name, MethodDescriptor descriptor) {
		super.visitMethod(range, declaration, className, name, descriptor);
		Token token = getToken(range);

		if (token.text.equals("new")) {
			return;
		}

		MethodEntry entry = methodEntryOf(className, name, descriptor);

		if (declaration) {
			addDeclaration(token, entry);
			currentMethod = entry;
		} else {
			addReference(token, entry, currentMethod);
		}
	}

	@Override
	public void visitParameter(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int lvIndex, String name) {
		super.visitParameter(range, declaration, className, methodName, methodDescriptor, lvIndex, name);
		Token token = getToken(range);
		MethodEntry parent = methodEntryOf(className, methodName, methodDescriptor);

		if (declaration) {
			addDeclaration(token, argEntryOf(parent, lvIndex, name));
		} else {
			addReference(token, argEntryOf(parent, lvIndex, name), currentMethod);
		}
	}

	@Override
	public void visitLocal(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int lvIndex, String name) {
		super.visitLocal(range, declaration, className, methodName, methodDescriptor, lvIndex, name);
		Token token = getToken(range);
		MethodEntry parent = methodEntryOf(className, methodName, methodDescriptor);

		if (declaration) {
			addDeclaration(token, varEntryOf(parent, lvIndex, name));
		} else {
			addReference(token, varEntryOf(parent, lvIndex, name), currentMethod);
		}
	}

	private ClassEntry classEntryOf(String name) {
		return ClassEntry.parse(name);
	}

	private FieldEntry fieldEntryOf(String className, String name, FieldDescriptor descriptor) {
		return FieldEntry.parse(className, name, descriptor.descriptorString);
	}

	private MethodEntry methodEntryOf(String className, String name, MethodDescriptor descriptor) {
		return MethodEntry.parse(className, name, descriptor.toString());
	}

	private LocalVariableEntry argEntryOf(MethodEntry className, int lvIndex, String name) {
		return new LocalVariableEntry(className, lvIndex, name, true, null);
	}

	private LocalVariableEntry varEntryOf(MethodEntry className, int lvIndex, String name) {
		return new LocalVariableEntry(className, lvIndex, name, false, null);
	}

	private Token getToken(TextRange range) {
		return new Token(range.start, range.start + range.length, content.substring(range.start, range.start + range.length));
	}

	private void addDeclaration(Token token, Entry<?> entry) {
		declarations.put(token, entry);
		tokens.add(token);
	}

	private void addReference(Token token, Entry<?> entry, Entry<?> context) {
		references.put(token, Pair.of(entry, context));
		tokens.add(token);
	}

	public void addTokensToIndex(SourceIndex index) {
		for (Token token : tokens) {
			if (declarations.get(token) != null) {
				index.addDeclaration(token, declarations.get(token));
			} else {
				Pair<Entry<?>, Entry<?>> reference = references.get(token);
				index.addReference(token, reference.a, reference.b);
			}
		}
	}
}
