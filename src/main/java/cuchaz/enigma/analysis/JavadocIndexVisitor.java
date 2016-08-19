package cuchaz.enigma.analysis;

import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Pattern;

import java.util.List;

/**
 * Permit to data all javadoc markers
 * Created by Thog
 * 19/08/2016
 */
public class JavadocIndexVisitor implements IAstVisitor<List<Annotation>, Void>
{

    protected Void recurse(AstNode node, List<Annotation> data) {
        for (final AstNode child : node.getChildren()) {
            child.acceptVisitor(this, data);
        }
        return null;
    }

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitFieldDeclaration(FieldDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override public Void visitTypeDeclaration(TypeDeclaration node, List<Annotation> data)
    {
        return recurse(node, data);
    }

    @Override
    public Void visitEnumValueDeclaration(EnumValueDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitParameterDeclaration(ParameterDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitInvocationExpression(InvocationExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitMemberReferenceExpression(MemberReferenceExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitSimpleType(SimpleType node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitIdentifierExpression(IdentifierExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitComment(Comment node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitPatternPlaceholder(AstNode node, Pattern pattern, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitTypeReference(TypeReferenceExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitJavaTokenNode(JavaTokenNode node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitIdentifier(Identifier node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitNullReferenceExpression(NullReferenceExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitThisReferenceExpression(ThisReferenceExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitSuperReferenceExpression(SuperReferenceExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitClassOfExpression(ClassOfExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitBlockStatement(BlockStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitBreakStatement(BreakStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitContinueStatement(ContinueStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitDoWhileStatement(DoWhileStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitEmptyStatement(EmptyStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitIfElseStatement(IfElseStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitLabelStatement(LabelStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitLabeledStatement(LabeledStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitReturnStatement(ReturnStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitSwitchStatement(SwitchStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitSwitchSection(SwitchSection node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitCaseLabel(CaseLabel node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitThrowStatement(ThrowStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitCatchClause(CatchClause node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitAnnotation(Annotation node, List<Annotation> data) {
        String annotationType = node.getFirstChild().toString();
        if (annotationType.startsWith("FieldDoc") || annotationType.startsWith("MethodDoc") || annotationType.startsWith("ClassDoc"))
            data.add(node);
        return recurse(node, data);
    }

    @Override
    public Void visitNewLine(NewLineNode node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitVariableInitializer(VariableInitializer node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitText(TextNode node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitImportDeclaration(ImportDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitInitializerBlock(InstanceInitializer node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitTypeParameterDeclaration(TypeParameterDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitCompilationUnit(CompilationUnit node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitPackageDeclaration(PackageDeclaration node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitArraySpecifier(ArraySpecifier node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitComposedType(ComposedType node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitWhileStatement(WhileStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitPrimitiveExpression(PrimitiveExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitCastExpression(CastExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitBinaryOperatorExpression(BinaryOperatorExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitInstanceOfExpression(InstanceOfExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitIndexerExpression(IndexerExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitUnaryOperatorExpression(UnaryOperatorExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitArrayInitializerExpression(ArrayInitializerExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitObjectCreationExpression(ObjectCreationExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitArrayCreationExpression(ArrayCreationExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitAssignmentExpression(AssignmentExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitForStatement(ForStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitTryCatchStatement(TryCatchStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitGotoStatement(GotoStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitParenthesizedExpression(ParenthesizedExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitSynchronizedStatement(SynchronizedStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitWildcardType(WildcardType node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitMethodGroupExpression(MethodGroupExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitAssertStatement(AssertStatement node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpression node, List<Annotation> data) {
        return recurse(node, data);
    }

    @Override
    public Void visitLocalTypeDeclarationStatement(LocalTypeDeclarationStatement node, List<Annotation> data) {
        return recurse(node, data);
    }
}
