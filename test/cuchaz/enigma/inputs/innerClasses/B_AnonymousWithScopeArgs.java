package cuchaz.enigma.inputs.innerClasses;

public class B_AnonymousWithScopeArgs {
	
	public static void foo(final D_Simple arg) {
		System.out.println(new Object() {
			@Override
			public String toString() {
				return arg.toString();
			}
		});
	}
}
