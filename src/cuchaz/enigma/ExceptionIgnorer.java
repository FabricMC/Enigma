package cuchaz.enigma;

public class ExceptionIgnorer {

	public static boolean shouldIgnore(Throwable t) {
		
		// is this that pesky concurrent access bug in the highlight painter system?
		// (ancient ui code is ancient)
		if (t instanceof ArrayIndexOutOfBoundsException) {
			StackTraceElement[] stackTrace = t.getStackTrace();
			if (stackTrace.length > 1) {
			
				// does this stack frame match javax.swing.text.DefaultHighlighter.paint*() ?
				StackTraceElement frame = stackTrace[1];
				if (frame.getClassName().equals("javax.swing.text.DefaultHighlighter") && frame.getMethodName().startsWith("paint")) {
					return true;
				}
			}
		}
		
		return false;
	}
	
}
