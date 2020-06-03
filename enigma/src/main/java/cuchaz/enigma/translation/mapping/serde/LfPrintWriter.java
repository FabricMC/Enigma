package cuchaz.enigma.translation.mapping.serde;

import java.io.PrintWriter;
import java.io.Writer;

public class LfPrintWriter extends PrintWriter {
    public LfPrintWriter(Writer out) {
        super(out);
    }

    @Override
    public void println() {
        // https://stackoverflow.com/a/14749004
        write('\n');
    }
}
