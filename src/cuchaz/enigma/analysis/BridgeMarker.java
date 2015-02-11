package cuchaz.enigma.analysis;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;
import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.MethodEntry;

public class BridgeMarker {
	
	private JarIndex m_jarIndex;
	
	public BridgeMarker(JarIndex jarIndex) {
		m_jarIndex = jarIndex;
	}
	
	public void markBridges(CtClass c) {
		
		for (CtMethod method : c.getDeclaredMethods()) {
			MethodEntry methodEntry = EntryFactory.getMethodEntry(method);
			
			// is this a bridge method?
			MethodEntry bridgedMethodEntry = m_jarIndex.getBridgedMethod(methodEntry);
			if (bridgedMethodEntry != null) {
				
				// it's a bridge method! add the bridge flag
				int flags = method.getMethodInfo().getAccessFlags();
				flags |= AccessFlag.BRIDGE;
				method.getMethodInfo().setAccessFlags(flags);
			}
		}
	}
}
