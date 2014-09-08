package cuchaz.enigma.inputs.inheritanceTree;

public abstract class BaseClass
{
	private String m_name;
	
	protected BaseClass( String name )
	{
		m_name = name;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public abstract void doBaseThings( );
}
