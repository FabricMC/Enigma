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

import com.strobel.componentmodel.Key;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Pattern;

import java.io.*;
import java.nio.charset.Charset;

public class TreeDumpVisitor implements IAstVisitor<Void, Void> {

	private File file;
	private Writer out;

	public TreeDumpVisitor(File file) {
		this.file = file;
	}

	@Override
	public Void visitCompilationUnit(CompilationUnit node, Void ignored) {
		try {
			out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
			recurse(node, ignored);
			out.close();
			return null;
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

	private Void recurse(AstNode node, Void ignored) {
		// show the tree
		try {
			out.write(getIndent(node) + node.getClass().getSimpleName() + " " + getText(node) + " " + dumpUserData(node) + " " + node.getRegion() + "\n");
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
			return "\"" + ((Identifier) node).getName() + "\"";
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
