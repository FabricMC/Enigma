package cuchaz.enigma.inputs.innerClasses;

public class A_Anonymous {
	
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
