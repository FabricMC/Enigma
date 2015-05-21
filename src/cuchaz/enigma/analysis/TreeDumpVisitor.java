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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.strobel.componentmodel.Key;
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

public class TreeDumpVisitor implements IAstVisitor<Void,Void> {
	
	private File m_file;
	private Writer m_out;
	
	public TreeDumpVisitor(File file) {
		m_file = file;
		m_out = null;
	}
	
	@Override
	public Void visitCompilationUnit(CompilationUnit node, Void ignored) {
		try {
			m_out = new FileWriter(m_file);
			recurse(node, ignored);
			m_out.close();
			return null;
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
	
	private Void recurse(AstNode node, Void ignored) {
		// show the tree
		try {
			m_out.write(getIndent(node) + node.getClass().getSimpleName() + " " + getText(node) + " " + dumpUserData(node) + " " + node.getRegion() + "\n");
		} catch (IOException ex) {
			throw new Error(ex);
		}
		
		// recurse
		for (final AstNode child : node.getChildren()) {
			child.acceptVisitor(this, ignored);
		}
		return null;
	}
	
	private String getText(AstNode node) {
		if (node instanceof Identifier) {
			return "\"" + ((Identifier)node).getName() + "\"";
		}
		return "";
	}
	
	private String dumpUserData(AstNode node) {
		StringBuilder buf = new StringBuilder();
		for (Key<?> key : Keys.ALL_KEYS) {
			Object val = node.getUserData(key);
			if (val != null) {
				buf.append(String.format(" [%s=%s]", key, val));
			}
		}
		return buf.toString();
	}
	
	private String getIndent(AstNode node) {
		StringBuilder buf = new StringBuilder();
		int depth = getDepth(node);
		for (int i = 0; i < depth; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}
	
	private int getDepth(AstNode node) {
		int depth = -1;
		while (node != null) {
			depth++;
			node = node.getParent();
		}
		return depth;
	}
	
	// OVERRIDES WE DON'T CARE ABOUT
	
	@Override
	public Void visitInvocationExpression(InvocationExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitMemberReferenceExpression(MemberReferenceExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitSimpleType(SimpleType node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitMethodDeclaration(MethodDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitConstructorDeclaration(ConstructorDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitParameterDeclaration(ParameterDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitFieldDeclaration(FieldDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitComment(Comment node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitPatternPlaceholder(AstNode node, Pattern pattern, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitTypeReference(TypeReferenceExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitJavaTokenNode(JavaTokenNode node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitIdentifier(Identifier node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitNullReferenceExpression(NullReferenceExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitThisReferenceExpression(ThisReferenceExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitSuperReferenceExpression(SuperReferenceExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitClassOfExpression(ClassOfExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitBlockStatement(BlockStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitExpressionStatement(ExpressionStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitBreakStatement(BreakStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitContinueStatement(ContinueStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitDoWhileStatement(DoWhileStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitEmptyStatement(EmptyStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitIfElseStatement(IfElseStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitLabelStatement(LabelStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitLabeledStatement(LabeledStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitReturnStatement(ReturnStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitSwitchStatement(SwitchStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitSwitchSection(SwitchSection node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitCaseLabel(CaseLabel node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitThrowStatement(ThrowStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitCatchClause(CatchClause node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitAnnotation(Annotation node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitNewLine(NewLineNode node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitVariableDeclaration(VariableDeclarationStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitVariableInitializer(VariableInitializer node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitText(TextNode node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitImportDeclaration(ImportDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitInitializerBlock(InstanceInitializer node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitTypeParameterDeclaration(TypeParameterDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitPackageDeclaration(PackageDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitArraySpecifier(ArraySpecifier node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitComposedType(ComposedType node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitWhileStatement(WhileStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitPrimitiveExpression(PrimitiveExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitCastExpression(CastExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitBinaryOperatorExpression(BinaryOperatorExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitInstanceOfExpression(InstanceOfExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitIndexerExpression(IndexerExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitIdentifierExpression(IdentifierExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitUnaryOperatorExpression(UnaryOperatorExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitConditionalExpression(ConditionalExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitArrayInitializerExpression(ArrayInitializerExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitObjectCreationExpression(ObjectCreationExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitArrayCreationExpression(ArrayCreationExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitAssignmentExpression(AssignmentExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitForStatement(ForStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitForEachStatement(ForEachStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitTryCatchStatement(TryCatchStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitGotoStatement(GotoStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitParenthesizedExpression(ParenthesizedExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitSynchronizedStatement(SynchronizedStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitWildcardType(WildcardType node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitMethodGroupExpression(MethodGroupExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitEnumValueDeclaration(EnumValueDeclaration node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitAssertStatement(AssertStatement node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitLambdaExpression(LambdaExpression node, Void ignored) {
		return recurse(node, ignored);
	}
	
	@Override
	public Void visitLocalTypeDeclarationStatement(LocalTypeDeclarationStatement node, Void ignored) {
		return recurse(node, ignored);
	}
}
