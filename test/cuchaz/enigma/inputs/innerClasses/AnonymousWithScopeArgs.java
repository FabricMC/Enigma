package cuchaz.enigma.inputs.innerClasses;

public class AnonymousWithScopeArgs
{
	public static void foo( final Simple arg )
	{
		System.out.println( new Object( )
		{
			@Override
			public String toString( )
			{
				return arg.toString();
			}
		} );
	}
}
