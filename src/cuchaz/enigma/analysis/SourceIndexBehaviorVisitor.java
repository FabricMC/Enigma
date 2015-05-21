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

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.SuperReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ThisReferenceExpression;

import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.ProcyonEntryFactory;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Type;

public class SourceIndexBehaviorVisitor extends SourceIndexVisitor {
	
	private BehaviorEntry m_behaviorEntry;
	
	public SourceIndexBehaviorVisitor(BehaviorEntry behaviorEntry) {
		m_behaviorEntry = behaviorEntry;
	}
	
	@Override
	public Void visitInvocationExpression(InvocationExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		
		// get the behavior entry
		ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
		BehaviorEntry behaviorEntry = null;
		if (ref instanceof MethodReference) {
			MethodReference methodRef = (MethodReference)ref;
			if (methodRef.isConstructor()) {
				behaviorEntry = new ConstructorEntry(classEntry, new Signature(ref.getErasedSignature()));
			} else if (methodRef.isTypeInitializer()) {
				behaviorEntry = new ConstructorEntry(classEntry);
			} else {
				behaviorEntry = new MethodEntry(classEntry, ref.getName(), new Signature(ref.getErasedSignature()));
			}
		}
		if (behaviorEntry != null) {
			// get the node for the token
			AstNode tokenNode = null;
			if (node.getTarget() instanceof MemberReferenceExpression) {
				tokenNode = ((MemberReferenceExpression)node.getTarget()).getMemberNameToken();
			} else if (node.getTarget() instanceof SuperReferenceExpression) {
				tokenNode = node.getTarget();
			} else if (node.getTarget() instanceof ThisReferenceExpression) {
				tokenNode = node.getTarget();
			}
			if (tokenNode != null) {
				index.addReference(tokenNode, behaviorEntry, m_behaviorEntry);
			}
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitMemberReferenceExpression(MemberReferenceExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		if (ref != null) {
			// make sure this is actually a field
			if (ref.getErasedSignature().indexOf('(') >= 0) {
				throw new Error("Expected a field here! got " + ref);
			}
			
			ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
			FieldEntry fieldEntry = new FieldEntry(classEntry, ref.getName(), new Type(ref.getErasedSignature()));
			index.addReference(node.getMemberNameToken(), fieldEntry, m_behaviorEntry);
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitSimpleType(SimpleType node, SourceIndex index) {
		TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
		if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
			ClassEntry classEntry = new ClassEntry(ref.getInternalName());
			index.addReference(node.getIdentifierToken(), classEntry, m_behaviorEntry);
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitParameterDeclaration(ParameterDeclaration node, SourceIndex index) {
		ParameterDefinition def = node.getUserData(Keys.PARAMETER_DEFINITION);
		if (def.getMethod() instanceof MethodDefinition) {
			MethodDefinition methodDef = (MethodDefinition)def.getMethod();
			BehaviorEntry behaviorEntry = ProcyonEntryFactory.getBehaviorEntry(methodDef);
			ArgumentEntry argumentEntry = new ArgumentEntry(behaviorEntry, def.getPosition(), node.getName());
			index.addDeclaration(node.getNameToken(), argumentEntry);
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitIdentifierExpression(IdentifierExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		if (ref != null) {
			ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
			FieldEntry fieldEntry = new FieldEntry(classEntry, ref.getName(), new Type(ref.getErasedSignature()));
			index.addReference(node.getIdentifierToken(), fieldEntry, m_behaviorEntry);
		}
		
		return recurse(node, index);
	}
	
	@Override
	public Void visitObjectCreationExpression(ObjectCreationExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		if (ref != null) {
			ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
			ConstructorEntry constructorEntry = new ConstructorEntry(classEntry, new Signature(ref.getErasedSignature()));
			if (node.getType() instanceof SimpleType) {
				SimpleType simpleTypeNode = (SimpleType)node.getType();
				index.addReference(simpleTypeNode.getIdentifierToken(), constructorEntry, m_behaviorEntry);
			}
		}
		
		return recurse(node, index);
	}
}
