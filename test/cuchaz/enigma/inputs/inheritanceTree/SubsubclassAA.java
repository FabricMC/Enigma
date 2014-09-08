package cuchaz.enigma.inputs.inheritanceTree;

public class SubsubclassAA extends SubclassA
{
	protected SubsubclassAA( )
	{
		super( "AA" );
	}
	
	@Override
	public String getName( )
	{
		return "subsub" + super.getName();
	}
	
	@Override
	public void doBaseThings( )
	{
		System.out.println( "Base things by " + getName() );
	}
}
