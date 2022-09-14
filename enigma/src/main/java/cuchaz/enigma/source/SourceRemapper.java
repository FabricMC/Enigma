package cuchaz.enigma.source;

import java.util.HashMap;
import java.util.Map;

public class SourceRemapper {
	private final String source;
	private final Iterable<Token> tokens;

	public SourceRemapper(String source, Iterable<Token> tokens) {
		this.source = source;
		this.tokens = tokens;
	}

	public Result remap(Remapper remapper) {
		StringBuffer remappedSource = new StringBuffer(source);
		Map<Token, Token> remappedTokens = new HashMap<>();

		int accumulatedOffset = 0;

		for (Token token : tokens) {
			Token movedToken = token.move(accumulatedOffset);

			String remappedName = remapper.remap(token, movedToken);

			if (remappedName != null) {
				accumulatedOffset += movedToken.getRenameOffset(remappedName);
				movedToken.rename(remappedSource, remappedName);
			}

			if (!token.equals(movedToken)) {
				remappedTokens.put(token, movedToken);
			}
		}

		return new Result(remappedSource.toString(), remappedTokens);
	}

	public static class Result {
		private final String remappedSource;
		private final Map<Token, Token> remappedTokens;

		Result(String remappedSource, Map<Token, Token> remappedTokens) {
			this.remappedSource = remappedSource;
			this.remappedTokens = remappedTokens;
		}

		public String getSource() {
			return remappedSource;
		}

		public Token getRemappedToken(Token token) {
			return remappedTokens.getOrDefault(token, token);
		}

		public boolean isEmpty() {
			return remappedTokens.isEmpty();
		}
	}

	public interface Remapper {
		String remap(Token token, Token movedToken);
	}
}
