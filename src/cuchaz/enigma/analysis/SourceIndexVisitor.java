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

import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.languages.java.ast.Annotation;
import com.strobel.decompiler.languages.java.ast.AnonymousObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayInitializerExpression;
import com.strobel.decompiler.languages.java.ast.ArraySpecifier;
import com.strobel.decompiler.languages.java.ast.AssertStatement;
import com.strobel.decompiler.languages.java.ast.AssignmentExpression;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.BreakStatement;
import com.strobel.decompiler.languages.java.ast.CaseLabel;
import com.strobel.decompiler.languages.java.ast.CastExpression;
import com.strobel.decompiler.languages.java.ast.CatchClause;
import com.strobel.decompiler.languages.java.ast.ClassOfExpression;
import com.strobel.decompiler.languages.java.ast.Comment;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.ComposedType;
import com.strobel.decompiler.languages.java.ast.ConditionalExpression;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.ContinueStatement;
import com.strobel.decompiler.languages.java.ast.DoWhileStatement;
import com.strobel.decompiler.languages.java.ast.EmptyStatement;
import com.strobel.decompiler.languages.java.ast.EnumValueDeclaration;
import com.strobel.decompiler.languages.java.ast.ExpressionStatement;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.ForEachStatement;
import com.strobel.decompiler.languages.java.ast.ForStatement;
import com.strobel.decompiler.languages.java.ast.GotoStatement;
import com.strobel.decompiler.languages.java.ast.IAstVisitor;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.IfElseStatement;
import com.strobel.decompiler.languages.java.ast.ImportDeclaration;
import com.strobel.decompiler.languages.java.ast.IndexerExpression;
import com.strobel.decompiler.languages.java.ast.InstanceInitializer;
import com.strobel.decompiler.languages.java.ast.InstanceOfExpression;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.JavaTokenNode;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.LabelStatement;
import com.strobel.decompiler.languages.java.ast.LabeledStatement;
import com.strobel.decompiler.languages.java.ast.LambdaExpression;
import com.strobel.decompiler.languages.java.ast.LocalTypeDeclarationStatement;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.MethodGroupExpression;
import com.strobel.decompiler.languages.java.ast.NewLineNode;
import com.strobel.decompiler.languages.java.ast.NullReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.PackageDeclaration;
import com.strobel.decompiler.languages.java.ast.ParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.ParenthesizedExpression;
import com.strobel.decompiler.languages.java.ast.PrimitiveExpression;
import com.strobel.decompiler.languages.java.ast.ReturnStatement;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.SuperReferenceExpression;
import com.strobel.decompiler.languages.java.ast.SwitchSection;
import com.strobel.decompiler.languages.java.ast.SwitchStatement;
import com.strobel.decompiler.languages.java.ast.SynchronizedStatement;
import com.strobel.decompiler.languages.java.ast.TextNode;
import com.strobel.decompiler.languages.java.ast.ThisReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ThrowStatement;
import com.strobel.decompiler.languages.java.ast.TryCatchStatement;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.VariableDeclarationStatement;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;
import com.strobel.decompiler.languages.java.ast.WhileStatement;
import com.strobel.decompiler.languages.java.ast.WildcardType;
import com.strobel.decompiler.patterns.Pattern;

import cuchaz.enigma.mapping.ClassEntry;

public class SourceIndexVisitor implements IAstVisitor<SourceIndex,Void> {
	
	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassEntry classEntry = new ClassEntry(def.getInternalName());
		index.addDeclaration(node.getNameToken(), classEntry);
		
		return node.acceptVisitor(new SourceIndexClassVisitor(classEntry), index);
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
