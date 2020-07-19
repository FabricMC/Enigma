package cuchaz.enigma.inputs.visibility;

class ClassA {

	protected Object protectedParentPrivateChild;
	public Object publicParentPrivateChild;

	public static Object LOGGER = null;

	protected static Object LOGGER2 = null;

	public Object publicPublic;

	public static void equalAccessStatic() {}

	protected static void protectedPublicStatic(){}

	private static void privateStaticParentPublicStaticChild(){}

	private void privateParentPublicStaticChild() {}

	static void packagePrivateParentProtectedChild(){}

	private static void packagePrivateChild(){}

	public ClassA returningSubclass(){return null;}

}