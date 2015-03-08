package cuchaz.enigma.gui;

import cuchaz.enigma.mapping.ClassEntry;


public class ScoredClassEntry extends ClassEntry {

	private static final long serialVersionUID = -8798725308554217105L;
	
	private float m_score;
	
	public ScoredClassEntry(ClassEntry other, float score) {
		super(other);
		m_score = score;
	}
	
	public float getScore() {
		return m_score;
	}
}
