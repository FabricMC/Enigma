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

import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Pattern;
import cuchaz.enigma.bytecode.AccessFlags;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.ClassDefEntry;
import cuchaz.enigma.translation.representation.ReferencedEntryPool;

public class SourceIndexVisitor implements IAstVisitor<SourceIndex, Void> {
	private final ReferencedEntryPool entryPool;

	public SourceIndexVisitor(ReferencedEntryPool entryPool) {
		this.entryPool = entryPool;
	}

	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassDefEntry classEntry = new ClassDefEntry(def.getInternalName(), Signature.createSignature(def.getSignature()), new AccessFlags(def.getModifiers()));
		index.addDeclaration(node.getNameToken(), classEntry);

		return node.acceptVisitor(new SourceIndexClassVisitor(entryPool, classEntry), index);
	}

	protected Void recurse(AstNode node, SourceIndex index) {
		for (final AstNode child : node.getChildren()) {
			child.acceptVisitor(this, index);
		}
		return null;
	}

	@Override
	public Void visitMethodDeclaration(MethodDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitConstructorDeclaration(ConstructorDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitFieldDeclaration(FieldDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitEnumValueDeclaration(EnumValueDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitParameterDeclaration(ParameterDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitInvocationExpression(InvocationExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitMemberReferenceExpression(MemberReferenceExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitSimpleType(SimpleType node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitIdentifierExpression(IdentifierExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitComment(Comment node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitPatternPlaceholder(AstNode node, Pattern pattern, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitTypeReference(TypeReferenceExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitJavaTokenNode(JavaTokenNode node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitIdentifier(Identifier node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitNullReferenceExpression(NullReferenceExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitThisReferenceExpression(ThisReferenceExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitSuperReferenceExpression(SuperReferenceExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitClassOfExpression(ClassOfExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitBlockStatement(BlockStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitExpressionStatement(ExpressionStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitBreakStatement(BreakStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitContinueStatement(ContinueStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitDoWhileStatement(DoWhileStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitEmptyStatement(EmptyStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitIfElseStatement(IfElseStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitLabelStatement(LabelStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitLabeledStatement(LabeledStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitReturnStatement(ReturnStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitSwitchStatement(SwitchStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitSwitchSection(SwitchSection node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitCaseLabel(CaseLabel node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitThrowStatement(ThrowStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitCatchClause(CatchClause node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitAnnotation(Annotation node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitNewLine(NewLineNode node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitVariableDeclaration(VariableDeclarationStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitVariableInitializer(VariableInitializer node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitText(TextNode node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitImportDeclaration(ImportDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitInitializerBlock(InstanceInitializer node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitTypeParameterDeclaration(TypeParameterDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitCompilationUnit(CompilationUnit node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitPackageDeclaration(PackageDeclaration node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitArraySpecifier(ArraySpecifier node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitComposedType(ComposedType node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitWhileStatement(WhileStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitPrimitiveExpression(PrimitiveExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitCastExpression(CastExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitBinaryOperatorExpression(BinaryOperatorExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitInstanceOfExpression(InstanceOfExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitIndexerExpression(IndexerExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitUnaryOperatorExpression(UnaryOperatorExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitConditionalExpression(ConditionalExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitArrayInitializerExpression(ArrayInitializerExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitObjectCreationExpression(ObjectCreationExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitArrayCreationExpression(ArrayCreationExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitAssignmentExpression(AssignmentExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitForStatement(ForStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitForEachStatement(ForEachStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitTryCatchStatement(TryCatchStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitGotoStatement(GotoStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitParenthesizedExpression(ParenthesizedExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitSynchronizedStatement(SynchronizedStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitWildcardType(WildcardType node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitMethodGroupExpression(MethodGroupExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitAssertStatement(AssertStatement node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitLambdaExpression(LambdaExpression node, SourceIndex index) {
		return recurse(node, index);
	}

	@Override
	public Void visitLocalTypeDeclarationStatement(LocalTypeDeclarationStatement node, SourceIndex index) {
		return recurse(node, index);
	}
}
