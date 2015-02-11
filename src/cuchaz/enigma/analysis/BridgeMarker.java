package cuchaz.enigma.analysis;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;

import com.google.common.collect.BiMap;

import cuchaz.enigma.mapping.EntryFactory;
import cuchaz.enigma.mapping.MethodEntry;

public class BridgeMarker {
	
	private BiMap<MethodEntry,MethodEntry> m_bridgedMethods;
	
	public BridgeMarker(BiMap<MethodEntry,MethodEntry> bridgedMethods) {
		m_bridgedMethods = bridgedMethods;
	}
	
	public void markBridges(CtClass c) {
		
		for (CtMethod method : c.getDeclaredMethods()) {
			MethodEntry methodEntry = EntryFactory.getMethodEntry(method);
			
			// is this a bridge method?
			MethodEntry bridgedMethodEntry = m_bridgedMethods.get(methodEntry);
			if (bridgedMethodEntry != null) {
				
				// it's a bridge method! add the bridge flag
				int flags = method.getMethodInfo().getAccessFlags();
				flags |= AccessFlag.BRIDGE;
				method.getMethodInfo().setAccessFlags(flags);
			}
		}
	}
}
