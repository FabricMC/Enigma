package cuchaz.enigma.utils;

import java.io.PrintWriter;
import java.io.Writer;

public class LFPrintWriter extends PrintWriter {
	public LFPrintWriter(Writer out) {
		super(out);
	}

	@Override
	public void println() {
		// https://stackoverflow.com/a/14749004
		write('\n');
	}
}
