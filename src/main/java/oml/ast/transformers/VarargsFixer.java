package oml.ast.transformers;

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataFilters;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MethodBinder;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.ArrayCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayInitializerExpression;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstNodeCollection;
import com.strobel.decompiler.languages.java.ast.CastExpression;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.JavaResolver;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import com.strobel.decompiler.semantics.ResolveResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thiakil on 12/07/2018.
 */
public class VarargsFixer implements IAstTransform {
	private final DecompilerContext _context;

	public VarargsFixer(final DecompilerContext context) {
		_context = VerifyArgument.notNull(context, "context");
	}

	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(_context), null);
	}

	class Visitor extends ContextTrackingVisitor<Void> {
		private final JavaResolver _resolver;
		protected Visitor(DecompilerContext context) {
			super(context);
			_resolver = new JavaResolver(context);
		}

		//remove `new Object[0]` on varagrs as the normal tranformer doesnt do them
		@Override
		public Void visitInvocationExpression(InvocationExpression node, Void data) {
			super.visitInvocationExpression(node, data);
			MemberReference definition = node.getUserData(Keys.MEMBER_REFERENCE);
			if (definition instanceof MethodDefinition && ((MethodDefinition) definition).isVarArgs()){
				AstNodeCollection<Expression> arguments = node.getArguments();
				Expression lastParam = arguments.lastOrNullObject();
				if (!lastParam.isNull() && lastParam instanceof ArrayCreationExpression){
					ArrayCreationExpression varargArray = (ArrayCreationExpression)lastParam;
					if (varargArray.getInitializer().isNull() || varargArray.getInitializer().getElements().isEmpty()){
						lastParam.remove();
					} else {
						for (Expression e : varargArray.getInitializer().getElements()){
							arguments.insertBefore(varargArray, e.clone());
						}
						varargArray.remove();
					}
				}
			}
			return null;
		}

		//applies the vararg transform to object creation
		@Override
		public Void visitObjectCreationExpression(ObjectCreationExpression node, Void data) {
			super.visitObjectCreationExpression(node, data);
			final AstNodeCollection<Expression> arguments = node.getArguments();
			final Expression lastArgument = arguments.lastOrNullObject();

			Expression arrayArg = lastArgument;

			if (arrayArg instanceof CastExpression)
				arrayArg = ((CastExpression) arrayArg).getExpression();

			if (arrayArg == null ||
					arrayArg.isNull() ||
					!(arrayArg instanceof ArrayCreationExpression &&
							node.getTarget() instanceof MemberReferenceExpression)) {

				return null;
			}

			final ArrayCreationExpression newArray = (ArrayCreationExpression) arrayArg;
			final MemberReferenceExpression target = (MemberReferenceExpression) node.getTarget();

			if (!newArray.getAdditionalArraySpecifiers().hasSingleElement()) {
				return null;
			}

			final MethodReference method = (MethodReference) node.getUserData(Keys.MEMBER_REFERENCE);

			if (method == null) {
				return null;
			}

			final MethodDefinition resolved = method.resolve();

			if (resolved == null || !resolved.isVarArgs()) {
				return null;
			}

			final List<MethodReference> candidates;
			final Expression invocationTarget = target.getTarget();

			if (invocationTarget == null || invocationTarget.isNull()) {
				candidates = MetadataHelper.findMethods(
						context.getCurrentType(),
						MetadataFilters.matchName(resolved.getName())
				);
			}
			else {
				final ResolveResult targetResult = _resolver.apply(invocationTarget);

				if (targetResult == null || targetResult.getType() == null) {
					return null;
				}

				candidates = MetadataHelper.findMethods(
						targetResult.getType(),
						MetadataFilters.matchName(resolved.getName())
				);
			}

			final List<TypeReference> argTypes = new ArrayList<>();

			for (final Expression argument : arguments) {
				final ResolveResult argResult = _resolver.apply(argument);

				if (argResult == null || argResult.getType() == null) {
					return null;
				}

				argTypes.add(argResult.getType());
			}

			final MethodBinder.BindResult c1 = MethodBinder.selectMethod(candidates, argTypes);

			if (c1.isFailure() || c1.isAmbiguous()) {
				return null;
			}

			argTypes.remove(argTypes.size() - 1);

			final ArrayInitializerExpression initializer = newArray.getInitializer();
			final boolean hasElements = !initializer.isNull() && !initializer.getElements().isEmpty();

			if (hasElements) {
				for (final Expression argument : initializer.getElements()) {
					final ResolveResult argResult = _resolver.apply(argument);

					if (argResult == null || argResult.getType() == null) {
						return null;
					}

					argTypes.add(argResult.getType());
				}
			}

			final MethodBinder.BindResult c2 = MethodBinder.selectMethod(candidates, argTypes);

			if (c2.isFailure() ||
					c2.isAmbiguous() ||
					!StringUtilities.equals(c2.getMethod().getErasedSignature(), c1.getMethod().getErasedSignature())) {

				return null;
			}

			lastArgument.remove();

			if (!hasElements) {
				lastArgument.remove();
				return null;
			}

			for (final Expression newArg : initializer.getElements()) {
				newArg.remove();
				arguments.add(newArg);
			}

			return null;
		}
	}
}
