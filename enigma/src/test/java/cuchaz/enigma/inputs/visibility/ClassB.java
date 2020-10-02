package cuchaz.enigma.inputs.visibility;

public class ClassB extends ClassA {
	private Object protectedParentPrivateChild;

	private Object publicParentPrivateChild;

	public Object publicPublic;

	public static Object LOGGER;

	public static Object LOGGER2 = null;

	public static void equalAccessStatic() {
	}

	public static void protectedPublicStatic() {
	}

	public static void privateStaticParentPublicStaticChild() {
	}

	public static void privateParentPublicStaticChild() {
	}

	protected static void packagePrivateParentProtectedChild(){}

	static void packagePrivateChild(){}

	public ClassB returningSubclass(){return null;}
}
