package cuchaz.enigma.inputs.innerClasses;

public class Anonymous {
	
	public void foo() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// don't care
			}
		};
		runnable.run();
	}
}
