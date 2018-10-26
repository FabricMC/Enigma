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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.*;
import cuchaz.enigma.mapping.TypeDescriptor;
import cuchaz.enigma.mapping.entry.*;

import java.lang.Error;
import java.util.HashMap;
import java.util.Map;

public class SourceIndexMethodVisitor extends SourceIndexVisitor {
	private final ReferencedEntryPool entryPool;
	private final ProcyonEntryFactory entryFactory;

	private final ClassDefEntry ownerEntry;
	private final MethodDefEntry methodEntry;

	private Multimap<String, Identifier> unmatchedIdentifier = HashMultimap.create();
	private Map<String, Entry> identifierEntryCache = new HashMap<>();

	public SourceIndexMethodVisitor(ReferencedEntryPool entryPool, ClassDefEntry ownerEntry, MethodDefEntry methodEntry) {
		super(entryPool);
		this.entryPool = entryPool;
		this.entryFactory = new ProcyonEntryFactory(entryPool);
		this.ownerEntry = ownerEntry;
		this.methodEntry = methodEntry;
	}

	@Override
	public Void visitInvocationExpression(InvocationExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);

		// get the behavior entry
		ClassEntry classEntry = entryPool.getClass(ref.getDeclaringType().getInternalName());
		MethodEntry methodEntry = null;
		if (ref instanceof MethodReference) {
			methodEntry = entryPool.getMethod(classEntry, ref.getName(), ref.getErasedSignature());
		}
		if (methodEntry != null) {
			// get the node for the token
			AstNode tokenNode = null;
			if (node.getTarget() instanceof MemberReferenceExpression) {
				tokenNode = ((MemberReferenceExpression) node.getTarget()).getMemberNameToken();
			} else if (node.getTarget() instanceof SuperReferenceExpression) {
				tokenNode = node.getTarget();
			} else if (node.getTarget() instanceof ThisReferenceExpression) {
				tokenNode = node.getTarget();
			}
			if (tokenNode != null) {
				index.addReference(tokenNode, methodEntry, this.methodEntry);
			}
		}

		// Check for identifier
		node.getArguments().stream().filter(expression -> expression instanceof IdentifierExpression)
				.forEach(expression -> this.checkIdentifier((IdentifierExpression) expression, index));
		return recurse(node, index);
	}

	@Override
	public Void visitMemberReferenceExpression(MemberReferenceExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		if (ref instanceof FieldReference) {
			// make sure this is actually a field
			String erasedSignature = ref.getErasedSignature();
			if (erasedSignature.indexOf('(') >= 0) {
				throw new Error("Expected a field here! got " + ref);
			}

			ClassEntry classEntry = entryPool.getClass(ref.getDeclaringType().getInternalName());
			FieldEntry fieldEntry = entryPool.getField(classEntry, ref.getName(), new TypeDescriptor(erasedSignature));
			if (fieldEntry == null) {
				throw new Error("Failed to find field " + ref.getName() + " on " + classEntry.getName());
			}
			index.addReference(node.getMemberNameToken(), fieldEntry, this.methodEntry);
		}

		return recurse(node, index);
	}

	@Override
	public Void visitSimpleType(SimpleType node, SourceIndex index) {
		TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
		if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
			ClassEntry classEntry = entryPool.getClass(ref.getInternalName());
			index.addReference(node.getIdentifierToken(), classEntry, this.methodEntry);
		}

		return recurse(node, index);
	}

	@Override
	public Void visitParameterDeclaration(ParameterDeclaration node, SourceIndex index) {
		ParameterDefinition def = node.getUserData(Keys.PARAMETER_DEFINITION);

		int variableOffset = this.methodEntry.getVariableOffset(ownerEntry);
		int parameterIndex = def.getSlot() - variableOffset;

		if (parameterIndex >= 0) {
			LocalVariableEntry localVariableEntry = new LocalVariableEntry(methodEntry, parameterIndex, node.getName());
			Identifier identifier = node.getNameToken();
			// cache the argument entry and the identifier
			identifierEntryCache.put(identifier.getName(), localVariableEntry);
			index.addDeclaration(identifier, localVariableEntry);
		}

		return recurse(node, index);
	}

	@Override
	public Void visitIdentifierExpression(IdentifierExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		if (ref != null) {
			ClassEntry classEntry = entryPool.getClass(ref.getDeclaringType().getInternalName());
			FieldEntry fieldEntry = entryPool.getField(classEntry, ref.getName(), new TypeDescriptor(ref.getErasedSignature()));
			if (fieldEntry == null) {
				throw new Error("Failed to find field " + ref.getName() + " on " + classEntry.getName());
			}
			index.addReference(node.getIdentifierToken(), fieldEntry, this.methodEntry);
		} else
			this.checkIdentifier(node, index);
		return recurse(node, index);
	}

	private void checkIdentifier(IdentifierExpression node, SourceIndex index) {
		if (identifierEntryCache.containsKey(node.getIdentifier())) // If it's in the argument cache, create a token!
			index.addDeclaration(node.getIdentifierToken(), identifierEntryCache.get(node.getIdentifier()));
		else
			unmatchedIdentifier.put(node.getIdentifier(), node.getIdentifierToken()); // Not matched actually, put it!
	}

	private void addDeclarationToUnmatched(String key, SourceIndex index) {
		Entry entry = identifierEntryCache.get(key);

		// This cannot happened in theory
		if (entry == null)
			return;
		for (Identifier identifier : unmatchedIdentifier.get(key))
			index.addDeclaration(identifier, entry);
		unmatchedIdentifier.removeAll(key);
	}

	@Override
	public Void visitObjectCreationExpression(ObjectCreationExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
		if (ref != null) {
			ClassEntry classEntry = entryPool.getClass(ref.getDeclaringType().getInternalName());
			MethodEntry constructorEntry = entryPool.getMethod(classEntry, "<init>", ref.getErasedSignature());
			if (node.getType() instanceof SimpleType) {
				SimpleType simpleTypeNode = (SimpleType) node.getType();
				index.addReference(simpleTypeNode.getIdentifierToken(), constructorEntry, this.methodEntry);
			}
		}

		return recurse(node, index);
	}

	@Override
	public Void visitVariableDeclaration(VariableDeclarationStatement node, SourceIndex index) {
		AstNodeCollection<VariableInitializer> variables = node.getVariables();

		// Single assignation
		if (variables.size() == 1) {
			VariableInitializer initializer = variables.firstOrNullObject();
			if (initializer != null && node.getType() instanceof SimpleType) {
				Identifier identifier = initializer.getNameToken();
				Variable variable = initializer.getUserData(Keys.VARIABLE);
				if (variable != null) {
					VariableDefinition originalVariable = variable.getOriginalVariable();
					if (originalVariable != null) {
						int variableOffset = methodEntry.getVariableOffset(ownerEntry);
						int variableIndex = originalVariable.getSlot() - variableOffset;
						if (variableIndex >= 0) {
							LocalVariableEntry localVariableEntry = new LocalVariableEntry(methodEntry, variableIndex, initializer.getName());
							identifierEntryCache.put(identifier.getName(), localVariableEntry);
							addDeclarationToUnmatched(identifier.getName(), index);
							index.addDeclaration(identifier, localVariableEntry);
						}
					}
				}
			}
		}
		return recurse(node, index);
	}

	@Override
	public Void visitMethodGroupExpression(MethodGroupExpression node, SourceIndex index) {
		MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);

		if (ref instanceof MethodReference) {
			// get the behavior entry
			ClassEntry classEntry = entryPool.getClass(ref.getDeclaringType().getInternalName());
			MethodEntry methodEntry = null;

			methodEntry = entryPool.getMethod(classEntry, ref.getName(), ref.getErasedSignature());
			// get the node for the token
			AstNode tokenNode = node.getMethodNameToken();
			if (tokenNode == null || (tokenNode.getRegion().getBeginLine() == 0 || tokenNode.getRegion().getEndLine() == 0)){
				tokenNode = node.getTarget();
			}
			if (tokenNode != null) {
				index.addReference(tokenNode, methodEntry, this.methodEntry);
			}
		}

		return recurse(node, index);
	}
}
