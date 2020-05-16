package cuchaz.enigma.source.procyon.transformers;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.CommonTypeReferences;
import com.strobel.assembler.metadata.Flags;
import com.strobel.assembler.metadata.IGenericInstance;
import com.strobel.assembler.metadata.IMemberDefinition;
import com.strobel.assembler.metadata.JvmType;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.java.ast.ArrayCreationExpression;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstNodeCollection;
import com.strobel.decompiler.languages.java.ast.AstType;
import com.strobel.decompiler.languages.java.ast.CastExpression;
import com.strobel.decompiler.languages.java.ast.ComposedType;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.Roles;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.WildcardType;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

/**
 * Created by Thiakil on 12/07/2018.
 */
public class Java8Generics implements IAstTransform {

	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(), null);
	}

	static class Visitor extends DepthFirstAstVisitor<Void,Void>{

		@Override
		public Void visitInvocationExpression(InvocationExpression node, Void data) {
			super.visitInvocationExpression(node, data);
			if (node.getTarget() instanceof MemberReferenceExpression){
				MemberReferenceExpression referenceExpression = (MemberReferenceExpression) node.getTarget();
				if (referenceExpression.getTypeArguments().stream().map(t->{
					TypeReference tr = t.toTypeReference();
					if (tr.getDeclaringType() != null){//ensure that inner types are resolved so we can get the TypeDefinition below
						TypeReference resolved = tr.resolve();
						if (resolved != null)
							return resolved;
					}
					return tr;
				}).anyMatch(t -> t.isWildcardType() || (t instanceof TypeDefinition && ((TypeDefinition) t).isAnonymous()))) {
					//these are invalid for invocations, let the compiler work it out
					referenceExpression.getTypeArguments().clear();
				} else if (referenceExpression.getTypeArguments().stream().allMatch(t->t.toTypeReference().equals(CommonTypeReferences.Object))){
					//all are <Object>, thereby redundant and/or bad
					referenceExpression.getTypeArguments().clear();
				}
			}
			return null;
		}

		@Override
		public Void visitObjectCreationExpression(ObjectCreationExpression node, Void data) {
			super.visitObjectCreationExpression(node, data);
			AstType type = node.getType();
			if (type instanceof SimpleType && !((SimpleType) type).getTypeArguments().isEmpty()){
				SimpleType simpleType = (SimpleType) type;
				AstNodeCollection<AstType> typeArguments = simpleType.getTypeArguments();
				if (typeArguments.size() == 1 && typeArguments.firstOrNullObject().toTypeReference().equals(CommonTypeReferences.Object)){
					//all are <Object>, thereby redundant and/or bad
					typeArguments.firstOrNullObject().getChildByRole(Roles.IDENTIFIER).replaceWith(Identifier.create(""));
				}
			}
			return null;
		}

		@Override
		public Void visitCastExpression(CastExpression node, Void data) {
			boolean doReplace = false;
			TypeReference typeReference = node.getType().toTypeReference();
			if (typeReference.isArray() && typeReference.getElementType().isGenericType()){
				doReplace = true;
			} else if (typeReference.isGenericType()) {
				Expression target = node.getExpression();
				if (typeReference instanceof IGenericInstance && ((IGenericInstance)typeReference).getTypeArguments().stream().anyMatch(t->t.isWildcardType())){
					doReplace = true;
				} else if (target instanceof InvocationExpression) {
					InvocationExpression invocationExpression = (InvocationExpression)target;
					if (invocationExpression.getTarget() instanceof MemberReferenceExpression && !((MemberReferenceExpression) invocationExpression.getTarget()).getTypeArguments().isEmpty()) {
						((MemberReferenceExpression) invocationExpression.getTarget()).getTypeArguments().clear();
						doReplace = true;
					}
				}
			}
			super.visitCastExpression(node, data);
			if (doReplace){
				node.replaceWith(node.getExpression());
			}
			return null;
		}
	}
}
