package cuchaz.enigma.inputs.inheritanceTree;

// none/c extends none/a
public class SubclassB extends BaseClass {
	
	// a
	private int m_numThings;
	
	// <init>()V
	protected SubclassB() {
		// none/a.<init>(Ljava/lang/String;)V
		super("B");
		
		// access to a
		m_numThings = 4;
	}
	
	@Override
	// a()V
	public void doBaseThings() {
		// call to none/a.a()Ljava/lang/String;
		System.out.println("Base things by B! " + getName());
	}
	
	// b()V
	public void doBThings() {
		// access to a
		System.out.println("" + m_numThings + " B things!");
	}
}
