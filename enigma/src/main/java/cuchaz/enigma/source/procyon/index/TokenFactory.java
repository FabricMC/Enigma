package cuchaz.enigma.source.procyon.index;

import java.util.regex.Pattern;

import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;

import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.Token;

public class TokenFactory {
	private static final Pattern ANONYMOUS_INNER = Pattern.compile("\\$\\d+$");

	public static Token createToken(SourceIndex index, AstNode node) {
		String name = node instanceof Identifier ? ((Identifier) node).getName() : "";
		Region region = node.getRegion();

		if (region.getBeginLine() == 0) {
			System.err.println("Got bad region from Procyon for node " + node);
			return null;
		}

		int start = index.getPosition(region.getBeginLine(), region.getBeginColumn());
		int end = index.getPosition(region.getEndLine(), region.getEndColumn());
		String text = index.getSource().substring(start, end);
		Token token = new Token(start, end, text);

		boolean isAnonymousInner = node instanceof Identifier && name.indexOf('$') >= 0 && node.getParent() instanceof ConstructorDeclaration && name.lastIndexOf('$') >= 0 && !ANONYMOUS_INNER.matcher(name).matches();

		if (isAnonymousInner) {
			TypeDeclaration type = node.getParent().getParent() instanceof TypeDeclaration ? (TypeDeclaration) node.getParent().getParent() : null;

			if (type != null) {
				name = type.getName();
				token.end = token.start + name.length();
			}
		}

		return token;
	}
}
