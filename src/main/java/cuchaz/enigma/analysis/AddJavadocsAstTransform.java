package cuchaz.enigma.analysis;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;
import java.util.stream.Stream;

public final class AddJavadocsAstTransform implements IAstTransform {

	private final EntryRemapper remapper;

	public AddJavadocsAstTransform(EntryRemapper remapper) {
		this.remapper = remapper;
	}

	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(remapper), null);
	}

	static class Visitor extends DepthFirstAstVisitor<Void, Void> {

		private final EntryRemapper remapper;

		Visitor(EntryRemapper remapper) {
			this.remapper = remapper;
		}

		private <T extends AstNode> void addDoc(T node, Function<T, Entry<?>> retriever) {
			final EntryMapping mapping = remapper.getDeobfMapping(retriever.apply(node));
			final String docs = mapping == null ? null : mapping.getJavadoc();
			final Comment[] comments = Strings.emptyToNull(docs) == null ? null : Stream.of(docs.split("\\R")).map(st -> new Comment(st,
					CommentType.Documentation)).toArray(Comment[]::new);
			if (comments != null) {
				node.insertChildrenBefore(node.getFirstChild(), Roles.COMMENT, comments);
			}
		}

		@Override
		protected Void visitChildren(AstNode node, Void data) {
			for (final AstNode child : node.getChildren()) {
				child.acceptVisitor(this, data);
			}
			return null;
		}

		@Override
		public Void visitMethodDeclaration(MethodDeclaration node, Void data) {
			addDoc(node, dec -> MethodDefEntry.parse(dec.getUserData(Keys.METHOD_DEFINITION)));
			return super.visitMethodDeclaration(node, data);
		}

		@Override
		public Void visitConstructorDeclaration(ConstructorDeclaration node, Void data) {
			addDoc(node, dec -> MethodDefEntry.parse(dec.getUserData(Keys.METHOD_DEFINITION)));
			return super.visitConstructorDeclaration(node, data);
		}

		@Override
		public Void visitFieldDeclaration(FieldDeclaration node, Void data) {
			addDoc(node, dec -> FieldDefEntry.parse(dec.getUserData(Keys.FIELD_DEFINITION)));
			return super.visitFieldDeclaration(node, data);
		}

		@Override
		public Void visitTypeDeclaration(TypeDeclaration node, Void data) {
			addDoc(node, dec -> ClassDefEntry.parse(dec.getUserData(Keys.TYPE_DEFINITION)));
			return super.visitTypeDeclaration(node, data);
		}

		@Override
		public Void visitEnumValueDeclaration(EnumValueDeclaration node, Void data) {
			addDoc(node, dec -> FieldDefEntry.parse(dec.getUserData(Keys.FIELD_DEFINITION)));
			return super.visitEnumValueDeclaration(node, data);
		}
	}
}
