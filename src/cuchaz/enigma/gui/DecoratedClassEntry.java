package cuchaz.enigma.gui;

import cuchaz.enigma.mapping.ClassEntry;


public class DecoratedClassEntry extends ClassEntry {

	private static final long serialVersionUID = -8798725308554217105L;
	
	private String m_decoration;
	
	public DecoratedClassEntry(ClassEntry other, String decoration) {
		super(other);
		m_decoration = decoration;
	}
	
	public String getDecoration() {
		return m_decoration;
	}
}
