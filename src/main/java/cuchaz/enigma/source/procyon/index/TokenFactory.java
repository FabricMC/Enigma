package cuchaz.enigma.source.procyon.index;

import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.source.SourceIndex;

import java.util.regex.Pattern;

public class TokenFactory {
    private static final Pattern ANONYMOUS_INNER = Pattern.compile("\\$\\d+$");

    public static Token createToken(SourceIndex index, AstNode node) {
        String name = node instanceof Identifier ? ((Identifier) node).getName() : "";
        Region region = node.getRegion();

        Token token = new Token(
                index.getPosition(region.getBeginLine(), region.getBeginColumn()),
                index.getPosition(region.getEndLine(), region.getEndColumn()),
                index.getSource()
        );

        boolean isAnonymousInner =
                node instanceof Identifier &&
                name.indexOf('$') >= 0 && node.getParent() instanceof ConstructorDeclaration &&
                name.lastIndexOf('$') >= 0 &&
                !ANONYMOUS_INNER.matcher(name).matches();

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
