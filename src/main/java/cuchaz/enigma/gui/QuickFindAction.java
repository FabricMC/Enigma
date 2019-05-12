package cuchaz.enigma.gui;

import de.sciss.syntaxpane.SyntaxDocument;
import de.sciss.syntaxpane.actions.DefaultSyntaxAction;

import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

public final class QuickFindAction extends DefaultSyntaxAction {
	public QuickFindAction() {
		super("quick-find");
	}

	@Override
	public void actionPerformed(JTextComponent target, SyntaxDocument document, int dot, ActionEvent event) {
		Data data = Data.get(target);
		data.showFindDialog(target);
	}

	private static class Data {
		private static final String KEY = "enigma-find-data";
		private EnigmaQuickFindDialog findDialog;

		private Data() {
		}

		public static Data get(JTextComponent target) {
			Object o = target.getDocument().getProperty(KEY);
			if (o instanceof Data) {
				return (Data) o;
			}

			Data data = new Data();
			target.getDocument().putProperty(KEY, data);
			return data;
		}

		public void showFindDialog(JTextComponent target) {
			if (findDialog == null) {
				findDialog = new EnigmaQuickFindDialog(target);
			}
			findDialog.showFor(target);
		}
	}
}
