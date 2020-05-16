package cuchaz.enigma.source.procyon.transformers;

import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.ImportDeclaration;
import com.strobel.decompiler.languages.java.ast.PackageDeclaration;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

public final class DropImportAstTransform implements IAstTransform {
	public static final DropImportAstTransform INSTANCE = new DropImportAstTransform();

	private DropImportAstTransform() {
	}

	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(), null);
	}

	static class Visitor extends DepthFirstAstVisitor<Void, Void> {
		@Override
		public Void visitPackageDeclaration(PackageDeclaration node, Void data) {
			node.remove();
			return null;
		}

		@Override
		public Void visitImportDeclaration(ImportDeclaration node, Void data) {
			node.remove();
			return null;
		}
	}
}
