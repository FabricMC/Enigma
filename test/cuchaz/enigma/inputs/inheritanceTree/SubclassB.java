package cuchaz.enigma.inputs.inheritanceTree;

public class SubclassB extends BaseClass
{
	private int m_numThings;
	
	protected SubclassB( )
	{
		super( "B" );
		
		m_numThings = 4;
	}
	
	@Override
	public void doBaseThings( )
	{
		System.out.println( "Base things by B!" );
	}
	
	public void doBThings( )
	{
		System.out.println( "" + m_numThings + " B things!" );
	}
}
