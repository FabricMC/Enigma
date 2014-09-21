package cuchaz.enigma.inputs.innerClasses;

public class Anonymous // a
{
	public void foo( )
	{
		Runnable runnable = new Runnable( ) // b
		{
			@Override
			public void run( )
			{
				// don't care
			}
		};
		runnable.run();
	}
}
