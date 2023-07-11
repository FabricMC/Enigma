package cuchaz.enigma.source.jadx;

import java.lang.reflect.Field;

import org.checkerframework.checker.nullness.qual.Nullable;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.impl.InMemoryCodeCache;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;

public class JadxDecompiler implements Decompiler {
	private final SourceSettings settings;
	private final ClassProvider classProvider;
	private boolean lineEndingFixed;

	public JadxDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.settings = sourceSettings;
		this.classProvider = classProvider;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper mapper) {
		fixLineEnding();
		return new JadxSource(settings, this::createJadxArgs, classProvider.get(className), mapper);
	}

	/**
	 * JADX uses the system default line ending, but SyntaxPane does not (seems to be hardcoded to \n).
	 * This causes tokens to be offset by one char per preceding line, since Windows' \r\n is one char longer than plain \n or \r.
	 * Unfortunately, there's currently no way of telling JADX to use a different value (see https://github.com/skylot/jadx/issues/1948),
	 * and reflection doesn't help us either (can't override interface fields), hence why we need to use Unsafe.
	 */
	private void fixLineEnding() {
		if (lineEndingFixed || System.getProperty("line.separator").equals("\n")) {
			return;
		}

		try {
			// Get Unsafe reference
			Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);

			// Force NL's value to be loaded from the system property
			ICodeWriter.NL.toString();

			// Get NL via reflection
			Field NL = ICodeWriter.class.getDeclaredField("NL");

			// Overwrite its value with \n
			long offset = unsafe.staticFieldOffset(NL);
			unsafe.putObject(ICodeWriter.class, offset, "\n");
		} catch (Throwable throwable) {
			throw new RuntimeException("Failed to change JADX's line separator to '\\n'", throwable);
		}

		lineEndingFixed = true;
	}

	private JadxArgs createJadxArgs() {
		JadxArgs args = new JadxArgs();
		args.setCodeCache(new InMemoryCodeCache());
		args.setShowInconsistentCode(true);
		args.setInlineAnonymousClasses(false);
		args.setInlineMethods(false);
		args.setRespectBytecodeAccModifiers(true);
		args.setRenameValid(false);

		if (settings.removeImports) {
			// Commented out for now, since JADX would use full identifiers everywhere
			// args.setUseImports(false);
		}

		return args;
	}
}
