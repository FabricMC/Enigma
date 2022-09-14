package cuchaz.enigma.gui.util;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeSelectionModel;

public class SingleTreeSelectionModel extends DefaultTreeSelectionModel {
	public SingleTreeSelectionModel() {
		this.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	}
}
