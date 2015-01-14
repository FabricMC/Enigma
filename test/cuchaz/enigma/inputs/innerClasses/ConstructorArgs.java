package cuchaz.enigma.inputs.innerClasses;

@SuppressWarnings("unused")
public class ConstructorArgs {
	
	class Inner {
		
		private int a;
		
		public Inner(int a) {
			this.a = a;
		}
	}
	
	Inner i;
	
	public void foo() {
		i = new Inner(5);
	}
}
