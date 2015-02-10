package cuchaz.enigma.inputs.translation;

public class C_SubClass extends B_BaseClass {
	
	public char f2; // shadows B_BaseClass.f2
	public int f3;
	public int f4;
	
	@Override
	public int m1() {
		return 32;
	}
	
	public int m3() {
		return 7;
	}
}
