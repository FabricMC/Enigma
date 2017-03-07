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
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.*;
import cuchaz.enigma.mapping.*;
import javassist.bytecode.Descriptor;

import java.util.HashMap;
import java.util.Map;

public class SourceIndexBehaviorVisitor extends SourceIndexVisitor {

    private BehaviorEntry behaviorEntry;

    // TODO: Really fix Procyon index problem with inner classes
    private int argumentPosition;
    private int localsPosition;
    private Multimap<String, Identifier> unmatchedIdentifier = HashMultimap.create();
    private Map<String, Entry> identifierEntryCache = new HashMap<>();

    public SourceIndexBehaviorVisitor(BehaviorEntry behaviorEntry) {
        this.behaviorEntry = behaviorEntry;
        this.argumentPosition = 0;
        this.localsPosition = 0;
    }

    @Override
    public Void visitInvocationExpression(InvocationExpression node, SourceIndex index) {
        MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);

        // get the behavior entry
        ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
        BehaviorEntry behaviorEntry = null;
        if (ref instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) ref;
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
                tokenNode = ((MemberReferenceExpression) node.getTarget()).getMemberNameToken();
            } else if (node.getTarget() instanceof SuperReferenceExpression) {
                tokenNode = node.getTarget();
            } else if (node.getTarget() instanceof ThisReferenceExpression) {
                tokenNode = node.getTarget();
            }
            if (tokenNode != null) {
                index.addReference(tokenNode, behaviorEntry, this.behaviorEntry);
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
        if (ref != null) {
            // make sure this is actually a field
            if (ref.getErasedSignature().indexOf('(') >= 0) {
                throw new Error("Expected a field here! got " + ref);
            }

            ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
            FieldEntry fieldEntry = new FieldEntry(classEntry, ref.getName(), new Type(ref.getErasedSignature()));
            index.addReference(node.getMemberNameToken(), fieldEntry, this.behaviorEntry);
        }

        return recurse(node, index);
    }

    @Override
    public Void visitSimpleType(SimpleType node, SourceIndex index) {
        TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
        if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
            ClassEntry classEntry = new ClassEntry(ref.getInternalName());
            index.addReference(node.getIdentifierToken(), classEntry, this.behaviorEntry);
        }

        return recurse(node, index);
    }

    @Override
    public Void visitParameterDeclaration(ParameterDeclaration node, SourceIndex index) {
        ParameterDefinition def = node.getUserData(Keys.PARAMETER_DEFINITION);
        if (def.getMethod() instanceof MemberReference && def.getMethod() instanceof MethodReference)
        {
            ArgumentEntry argumentEntry = new ArgumentEntry(ProcyonEntryFactory.getBehaviorEntry((MethodReference) def.getMethod()),
                    argumentPosition++, node.getName());
            Identifier identifier = node.getNameToken();
            // cache the argument entry and the identifier
            identifierEntryCache.put(identifier.getName(), argumentEntry);
            index.addDeclaration(identifier, argumentEntry);
        }

        return recurse(node, index);
    }

    @Override
    public Void visitIdentifierExpression(IdentifierExpression node, SourceIndex index) {
        MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
        if (ref != null) {
            ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
            FieldEntry fieldEntry = new FieldEntry(classEntry, ref.getName(), new Type(ref.getErasedSignature()));
            index.addReference(node.getIdentifierToken(), fieldEntry, this.behaviorEntry);
        }
        else
            this.checkIdentifier(node, index);
        return recurse(node, index);
    }

    private void checkIdentifier(IdentifierExpression node, SourceIndex index)
    {
        if (identifierEntryCache.containsKey(node.getIdentifier())) // If it's in the argument cache, create a token!
            index.addDeclaration(node.getIdentifierToken(), identifierEntryCache.get(node.getIdentifier()));
        else
            unmatchedIdentifier.put(node.getIdentifier(), node.getIdentifierToken()); // Not matched actually, put it!
    }

    private void addDeclarationToUnmatched(String key, SourceIndex index)
    {
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
            ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
            ConstructorEntry constructorEntry = new ConstructorEntry(classEntry, new Signature(ref.getErasedSignature()));
            if (node.getType() instanceof SimpleType) {
                SimpleType simpleTypeNode = (SimpleType) node.getType();
                index.addReference(simpleTypeNode.getIdentifierToken(), constructorEntry, this.behaviorEntry);
            }
        }

        return recurse(node, index);
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node, SourceIndex index) {
        if (node.getVariableType() instanceof SimpleType)
        {
            SimpleType type = (SimpleType) node.getVariableType();
            TypeReference typeReference = type.getUserData(Keys.TYPE_REFERENCE);
            Identifier identifier = node.getVariableNameToken();
            String signature = Descriptor.of(typeReference.getErasedDescription());
            LocalVariableEntry localVariableEntry = new LocalVariableEntry(behaviorEntry, localsPosition++, identifier.getName(), new Type(signature));
            identifierEntryCache.put(identifier.getName(), localVariableEntry);
            addDeclarationToUnmatched(identifier.getName(), index);
            index.addDeclaration(identifier, localVariableEntry);
        }
        return recurse(node, index);
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationStatement node, SourceIndex index) {
        AstNodeCollection<VariableInitializer> variables = node.getVariables();

        // Single assignation
        if (variables.size() == 1)
        {
            VariableInitializer initializer = variables.firstOrNullObject();
            if (initializer != null && node.getType() instanceof SimpleType)
            {
                SimpleType type = (SimpleType) node.getType();
                TypeReference typeReference = type.getUserData(Keys.TYPE_REFERENCE);
                String signature = Descriptor.of(typeReference.getErasedDescription());
                Identifier identifier = initializer.getNameToken();
                LocalVariableEntry localVariableEntry = new LocalVariableEntry(behaviorEntry, localsPosition++, initializer.getName(), new Type(signature));
                identifierEntryCache.put(identifier.getName(), localVariableEntry);
                addDeclarationToUnmatched(identifier.getName(), index);
                index.addDeclaration(identifier, localVariableEntry);
            }
        }
        return recurse(node, index);
    }
}
