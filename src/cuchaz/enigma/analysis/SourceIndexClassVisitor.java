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

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.EnumValueDeclaration;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.ProcyonEntryFactory;

public class SourceIndexClassVisitor extends SourceIndexVisitor {
	
	private ClassEntry m_classEntry;
	
	public SourceIndexClassVisitor(ClassEntry classEntry) {
		m_classEntry = classEntry;
	}
	
	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		// is this this class, or a subtype?
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassEntry classEntry = new ClassEntry(def.getInternalName());
		if (!classEntry.equals(m_classEntry)) {
			// it's a sub-type, recurse
			index.addDeclaration(node.getNameToken(), classEntry);
			return node.acceptVisitor(new SourceIndexClassVisitor(classEntry), index);
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitSimpleType(SimpleType node, SourceIndex index) {
		TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
		if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
			ClassEntry classEntry = new ClassEntry(ref.getInternalName());
			index.addReference(node.getIdentifierToken(), classEntry, m_classEntry);
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitMethodDeclaration(MethodDeclaration node, SourceIndex index) {
		MethodDefinition def = node.getUserData(Keys.METHOD_DEFINITION);
		BehaviorEntry behaviorEntry = ProcyonEntryFactory.getBehaviorEntry(def);
		AstNode tokenNode = node.getNameToken();
		
		if (behaviorEntry instanceof ConstructorEntry) {
			ConstructorEntry constructorEntry = (ConstructorEntry)behaviorEntry;
			if (constructorEntry.isStatic()) {
				// for static initializers, check elsewhere for the token node
				tokenNode = node.getModifiers().firstOrNullObject();
			}
		}
		index.addDeclaration(tokenNode, behaviorEntry);
		return node.acceptVisitor(new SourceIndexBehaviorVisitor(behaviorEntry), index);
	}
	
	@Override
	public Void visitConstructorDeclaration(ConstructorDeclaration node, SourceIndex index) {
		MethodDefinition def = node.getUserData(Keys.METHOD_DEFINITION);
		ConstructorEntry constructorEntry = ProcyonEntryFactory.getConstructorEntry(def);
		index.addDeclaration(node.getNameToken(), constructorEntry);
		return node.acceptVisitor(new SourceIndexBehaviorVisitor(constructorEntry), index);
	}
	
	@Override
	public Void visitFieldDeclaration(FieldDeclaration node, SourceIndex index) {
		FieldDefinition def = node.getUserData(Keys.FIELD_DEFINITION);
		FieldEntry fieldEntry = ProcyonEntryFactory.getFieldEntry(def);
		assert (node.getVariables().size() == 1);
		VariableInitializer variable = node.getVariables().firstOrNullObject();
		index.addDeclaration(variable.getNameToken(), fieldEntry);
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitEnumValueDeclaration(EnumValueDeclaration node, SourceIndex index) {
		// treat enum declarations as field declarations
		FieldDefinition def = node.getUserData(Keys.FIELD_DEFINITION);
		FieldEntry fieldEntry = ProcyonEntryFactory.getFieldEntry(def);
		index.addDeclaration(node.getNameToken(), fieldEntry);
		
		return recurse(node, index);
	}
}
