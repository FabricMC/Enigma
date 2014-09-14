package cuchaz.enigma.inputs.inheritanceTree;

// none/d extends none/b
public class SubsubclassAA extends SubclassA
{
	protected SubsubclassAA( )
	{
		// call to none/b.<init>(Ljava/lang/String;)V
		super( "AA" );
	}
	
	@Override
	// a()Ljava/lang/String;
	public String getName( )
	{
		// call to none/b.a()Ljava/lang/String;
		return "subsub" + super.getName();
	}
	
	@Override
	// a()V
	public void doBaseThings( )
	{
		// call to none/d.a()Ljava/lang/String;
		System.out.println( "Base things by " + getName() );
	}
}
