package cuchaz.enigma.inputs.translation;

import java.util.ArrayList;
import java.util.List;

public class D_AnonymousTesting {
	
	public List<Object> getObjs() {
		List<Object> objs = new ArrayList<Object>();
		objs.add(new Object() {
			@Override
			public String toString() {
				return "Object!";
			}
		});
		return objs;
	}
}
