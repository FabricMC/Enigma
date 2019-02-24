package cuchaz.enigma.analysis;

import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

import javax.lang.model.element.Modifier;

public final class DropVarModifiersAstTransform implements IAstTransform {
	public static final DropVarModifiersAstTransform INSTANCE = new DropVarModifiersAstTransform();

	private DropVarModifiersAstTransform() {
	}

	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(), null);
	}

	static class Visitor extends DepthFirstAstVisitor<Void, Void> {
		@Override
		public Void visitParameterDeclaration(ParameterDeclaration node, Void data) {
			for (JavaModifierToken modifierToken : node.getChildrenByRole(EntityDeclaration.MODIFIER_ROLE)) {
				if (modifierToken.getModifier() == Modifier.FINAL) {
					modifierToken.remove();
				}
			}

			return null;
		}

		@Override
		public Void visitVariableDeclaration(VariableDeclarationStatement node, Void data) {
			node.removeModifier(Modifier.FINAL);
			return null;
		}
	}
}
