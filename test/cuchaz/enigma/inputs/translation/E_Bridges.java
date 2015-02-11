package cuchaz.enigma.inputs.translation;

import java.util.Iterator;


public class E_Bridges implements Iterator<Object> {

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public String next() {
		// the compiler will generate a bridge for this method
		return "foo";
	}

	@Override
	public void remove() {
	}
}
