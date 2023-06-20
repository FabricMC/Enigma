package cuchaz.enigma.analysis;

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class MethodTreeNode extends DefaultMutableTreeNode {
	protected final Translator translator;
	private final MethodEntry entry;

	public MethodTreeNode(Translator translator, MethodEntry entry) {
		this.translator = translator;
		this.entry = entry;
	}

	/**
	 * Returns the method entry represented by this tree node.
	 */
	public MethodEntry getMethodEntry() {
		return this.entry;
	}
}
