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

package cuchaz.enigma.analysis;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.*;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;

public class SourceIndexClassVisitor extends SourceIndexVisitor {
	private ClassDefEntry classEntry;

	public SourceIndexClassVisitor(ClassDefEntry classEntry) {
		this.classEntry = classEntry;
	}

	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		// is this this class, or a subtype?
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassDefEntry classEntry = ClassDefEntry.parse(def);
		if (!classEntry.equals(this.classEntry)) {
			// it's a subtype, recurse
			index.addDeclaration(node.getNameToken(), classEntry);
			return node.acceptVisitor(new SourceIndexClassVisitor(classEntry), index);
		}

		return visitChildren(node, index);
	}

	@Override
	public Void visitSimpleType(SimpleType node, SourceIndex index) {
		TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
		if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
			ClassEntry classEntry = new ClassEntry(ref.getInternalName());
			index.addReference(node.getIdentifierToken(), classEntry, this.classEntry);
		}

		return visitChildren(node, index);
	}

	@Override
	public Void visitMethodDeclaration(MethodDeclaration node, SourceIndex index) {
		MethodDefinition def = node.getUserData(Keys.METHOD_DEFINITION);
		MethodDefEntry methodEntry = MethodDefEntry.parse(def);
		AstNode tokenNode = node.getNameToken();
		if (methodEntry.isConstructor() && methodEntry.getName().equals("<clinit>")) {
			// for static initializers, check elsewhere for the token node
			tokenNode = node.getModifiers().firstOrNullObject();
		}
		index.addDeclaration(tokenNode, methodEntry);
		return node.acceptVisitor(new SourceIndexMethodVisitor(methodEntry), index);
	}

	@Override
	public Void visitConstructorDeclaration(ConstructorDeclaration node, SourceIndex index) {
		MethodDefinition def = node.getUserData(Keys.METHOD_DEFINITION);
		MethodDefEntry methodEntry = MethodDefEntry.parse(def);
		index.addDeclaration(node.getNameToken(), methodEntry);
		return node.acceptVisitor(new SourceIndexMethodVisitor(methodEntry), index);
	}

	@Override
	public Void visitFieldDeclaration(FieldDeclaration node, SourceIndex index) {
		FieldDefinition def = node.getUserData(Keys.FIELD_DEFINITION);
		FieldDefEntry fieldEntry = FieldDefEntry.parse(def);
		assert (node.getVariables().size() == 1);
		VariableInitializer variable = node.getVariables().firstOrNullObject();
		index.addDeclaration(variable.getNameToken(), fieldEntry);
		return visitChildren(node, index);
	}

	@Override
	public Void visitEnumValueDeclaration(EnumValueDeclaration node, SourceIndex index) {
		// treat enum declarations as field declarations
		FieldDefinition def = node.getUserData(Keys.FIELD_DEFINITION);
		FieldDefEntry fieldEntry = FieldDefEntry.parse(def);
		index.addDeclaration(node.getNameToken(), fieldEntry);
		return visitChildren(node, index);
	}
}
