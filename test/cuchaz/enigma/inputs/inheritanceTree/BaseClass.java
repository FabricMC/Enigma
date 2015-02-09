package cuchaz.enigma.inputs.inheritanceTree;

// none/a
public abstract class BaseClass {
	
	// a
	private String m_name;
	
	// <init>(Ljava/lang/String;)V
	protected BaseClass(String name) {
		m_name = name;
	}
	
	// a()Ljava/lang/String;
	public String getName() {
		return m_name;
	}
	
	// a()V
	public abstract void doBaseThings();
}
