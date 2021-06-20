package cuchaz.enigma.gui.elements;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.EditableType;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.panels.EditorPanel;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.I18n;

public class EditorPopupMenu {

	private final JPopupMenu ui = new JPopupMenu();

	private final JMenuItem renameItem = new JMenuItem();
	private final JMenuItem editJavadocItem = new JMenuItem();
	private final JMenuItem showInheritanceItem = new JMenuItem();
	private final JMenuItem showImplementationsItem = new JMenuItem();
	private final JMenuItem showCallsItem = new JMenuItem();
	private final JMenuItem showCallsSpecificItem = new JMenuItem();
	private final JMenuItem openEntryItem = new JMenuItem();
	private final JMenuItem openPreviousItem = new JMenuItem();
	private final JMenuItem openNextItem = new JMenuItem();
	private final JMenuItem toggleMappingItem = new JMenuItem();
	private final JMenuItem zoomInItem = new JMenuItem();
	private final JMenuItem zoomOutMenu = new JMenuItem();
	private final JMenuItem resetZoomItem = new JMenuItem();

	private final EditorPanel editor;
	private final Gui gui;

	public EditorPopupMenu(EditorPanel editor, Gui gui) {
		this.editor = editor;
		this.gui = gui;

		this.retranslateUi();

		this.ui.add(this.renameItem);
		this.ui.add(this.editJavadocItem);
		this.ui.add(this.showInheritanceItem);
		this.ui.add(this.showImplementationsItem);
		this.ui.add(this.showCallsItem);
		this.ui.add(this.showCallsSpecificItem);
		this.ui.add(this.openEntryItem);
		this.ui.add(this.openPreviousItem);
		this.ui.add(this.openNextItem);
		this.ui.add(this.toggleMappingItem);
		this.ui.addSeparator();
		this.ui.add(this.zoomInItem);
		this.ui.add(this.zoomOutMenu);
		this.ui.add(this.resetZoomItem);

		this.renameItem.setEnabled(false);
		this.editJavadocItem.setEnabled(false);
		this.showInheritanceItem.setEnabled(false);
		this.showImplementationsItem.setEnabled(false);
		this.showCallsItem.setEnabled(false);
		this.showCallsSpecificItem.setEnabled(false);
		this.openEntryItem.setEnabled(false);
		this.openPreviousItem.setEnabled(false);
		this.openNextItem.setEnabled(false);
		this.toggleMappingItem.setEnabled(false);

		this.renameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
		this.editJavadocItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
		this.showInheritanceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
		this.showImplementationsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
		this.showCallsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		this.showCallsSpecificItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
		this.openEntryItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		this.openPreviousItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
		this.openNextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
		this.toggleMappingItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		this.zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
		this.zoomOutMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));

		this.renameItem.addActionListener(event -> gui.startRename(editor));
		this.editJavadocItem.addActionListener(event -> gui.startDocChange(editor));
		this.showInheritanceItem.addActionListener(event -> gui.showInheritance(editor));
		this.showImplementationsItem.addActionListener(event -> gui.showImplementations(editor));
		this.showCallsItem.addActionListener(event -> gui.showCalls(editor, true));
		this.showCallsSpecificItem.addActionListener(event -> gui.showCalls(editor, false));
		this.openEntryItem.addActionListener(event -> gui.getController().navigateTo(editor.getCursorReference().entry));
		this.openPreviousItem.addActionListener(event -> gui.getController().openPreviousReference());
		this.openNextItem.addActionListener(event -> gui.getController().openNextReference());
		this.toggleMappingItem.addActionListener(event -> gui.toggleMapping(editor));
		this.zoomInItem.addActionListener(event -> editor.offsetEditorZoom(2));
		this.zoomOutMenu.addActionListener(event -> editor.offsetEditorZoom(-2));
		this.resetZoomItem.addActionListener(event -> editor.resetEditorZoom());
	}

	// TODO have editor redirect key event to menu so that the actions get
	//  	triggered without having to hardcode them here, because this
	//		is a hack
	public boolean handleKeyEvent(KeyEvent event) {
		if (event.isControlDown()) {
			switch (event.getKeyCode()) {
				case KeyEvent.VK_I:
					this.showInheritanceItem.doClick();
					return true;
				case KeyEvent.VK_M:
					this.showImplementationsItem.doClick();
					return true;
				case KeyEvent.VK_N:
					this.openEntryItem.doClick();
					return true;
				case KeyEvent.VK_P:
					this.openPreviousItem.doClick();
					return true;
				case KeyEvent.VK_E:
					this.openNextItem.doClick();
					return true;
				case KeyEvent.VK_C:
					if (event.isShiftDown()) {
						this.showCallsSpecificItem.doClick();
					} else {
						this.showCallsItem.doClick();
					}
					return true;
				case KeyEvent.VK_O:
					this.toggleMappingItem.doClick();
					return true;
				case KeyEvent.VK_R:
					this.renameItem.doClick();
					return true;
				case KeyEvent.VK_D:
					this.editJavadocItem.doClick();
					return true;
			}
		}
		return false;
	}

	public void updateUiState() {
		EntryReference<Entry<?>, Entry<?>> ref = this.editor.getCursorReference();
		Entry<?> referenceEntry = ref == null ? null : ref.entry;
		GuiController controller = this.gui.getController();

		boolean isClassEntry = referenceEntry instanceof ClassEntry;
		boolean isFieldEntry = referenceEntry instanceof FieldEntry;
		boolean isMethodEntry = referenceEntry instanceof MethodEntry me && !me.isConstructor();
		boolean isConstructorEntry = referenceEntry instanceof MethodEntry me && me.isConstructor();
		boolean isRenamable = ref != null && controller.project.isRenamable(ref);

		// TODO get rid of this with Entry rework
		EditableType type = null;

		if (referenceEntry instanceof ClassEntry) {
			type = EditableType.CLASS;
		} else if (referenceEntry instanceof MethodEntry me) {
			if (me.isConstructor()) {
				// treat constructors as classes because renaming one renames
				// the class
				type = EditableType.CLASS;
			} else {
				type = EditableType.METHOD;
			}
		} else if (referenceEntry instanceof FieldEntry) {
			type = EditableType.FIELD;
		} else if (referenceEntry instanceof LocalVariableEntry lve) {
			if (lve.isArgument()) {
				type = EditableType.PARAMETER;
			} else {
				type = EditableType.LOCAL_VARIABLE;
			}
		}

		this.renameItem.setEnabled(isRenamable && (type != null && this.gui.isEditable(type)));
		this.editJavadocItem.setEnabled(isRenamable && this.gui.isEditable(EditableType.JAVADOC));
		this.showInheritanceItem.setEnabled(isClassEntry || isMethodEntry || isConstructorEntry);
		this.showImplementationsItem.setEnabled(isClassEntry || isMethodEntry);
		this.showCallsItem.setEnabled(isRenamable && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		this.showCallsSpecificItem.setEnabled(isRenamable && isMethodEntry);
		this.openEntryItem.setEnabled(isRenamable && (isClassEntry || isFieldEntry || isMethodEntry || isConstructorEntry));
		this.openPreviousItem.setEnabled(controller.hasPreviousReference());
		this.openNextItem.setEnabled(controller.hasNextReference());
		this.toggleMappingItem.setEnabled(isRenamable && (type != null && this.gui.isEditable(type)));

		if (referenceEntry != null && this.gui.getController().project.getMapper().extendedDeobfuscate(referenceEntry).isDeobfuscated()) {
			this.toggleMappingItem.setText(I18n.translate("popup_menu.reset_obfuscated"));
		} else {
			this.toggleMappingItem.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		}
	}

	public void retranslateUi() {
		this.renameItem.setText(I18n.translate("popup_menu.rename"));
		this.editJavadocItem.setText(I18n.translate("popup_menu.javadoc"));
		this.showInheritanceItem.setText(I18n.translate("popup_menu.inheritance"));
		this.showImplementationsItem.setText(I18n.translate("popup_menu.implementations"));
		this.showCallsItem.setText(I18n.translate("popup_menu.calls"));
		this.showCallsSpecificItem.setText(I18n.translate("popup_menu.calls.specific"));
		this.openEntryItem.setText(I18n.translate("popup_menu.declaration"));
		this.openPreviousItem.setText(I18n.translate("popup_menu.back"));
		this.openNextItem.setText(I18n.translate("popup_menu.forward"));
		this.toggleMappingItem.setText(I18n.translate("popup_menu.mark_deobfuscated"));
		this.zoomInItem.setText(I18n.translate("popup_menu.zoom.in"));
		this.zoomOutMenu.setText(I18n.translate("popup_menu.zoom.out"));
		this.resetZoomItem.setText(I18n.translate("popup_menu.zoom.reset"));
	}

	public JPopupMenu getUi() {
		return ui;
	}

}
