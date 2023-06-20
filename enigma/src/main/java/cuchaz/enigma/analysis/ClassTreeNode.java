package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class ClassTreeNode extends DefaultMutableTreeNode {
	protected final Translator translator;
	private final ClassEntry entry;
	
	public ClassTreeNode(Translator translator, ClassEntry entry) {
		this.translator = translator;
		this.entry = entry;
	}
	
	/**
	 * Returns the class entry represented by this tree node.
	 */
	public ClassEntry getClassEntry() {
		return this.entry;
	}
	
	@Override
	public String toString() {
		return translator.translate(this.entry).getFullName();
	}
}
