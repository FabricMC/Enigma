package cuchaz.enigma.inputs.innerClasses;

public class E_AnonymousWithOuterAccess {
	
	// reproduction of error case documented at:
	// https://bitbucket.org/cuchaz/enigma/issue/61/stackoverflowerror-when-deobfuscating
	
	public Object makeInner() {
		outerMethod();
		return new Object() {
			@Override
			public String toString() {
				return outerMethod();
			}
		};
	}
	
	private String outerMethod() {
		return "foo";
	}
}
