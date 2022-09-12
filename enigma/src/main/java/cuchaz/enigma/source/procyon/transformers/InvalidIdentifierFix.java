package cuchaz.enigma.source.procyon.transformers;

import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

/**
 * Created by Thiakil on 13/07/2018.
 */
public class InvalidIdentifierFix implements IAstTransform {
	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(), null);
	}

	class Visitor extends DepthFirstAstVisitor<Void, Void> {
		@Override
		public Void visitIdentifier(Identifier node, Void data) {
			super.visitIdentifier(node, data);

			if (node.getName().equals("do") || node.getName().equals("if")) {
				Identifier newIdentifier = Identifier.create(node.getName() + "_", node.getStartLocation());
				newIdentifier.copyUserDataFrom(node);
				node.replaceWith(newIdentifier);
			}

			return null;
		}
	}
}
