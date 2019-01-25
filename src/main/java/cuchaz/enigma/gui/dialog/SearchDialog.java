package cuchaz.enigma.gui.dialog;

import com.google.common.collect.Lists;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchDialog {

	JPanel pane;

	JTextField searchField;
	JList<String> list;

	JFrame frame;
	Gui gui;
	List<ClassEntry> deobfClasses;

	KeyEventDispatcher keyEventDispatcher;

	public SearchDialog(Gui gui) {
		this.gui = gui;

		deobfClasses = Lists.newArrayList();
		this.gui.getController().getDeobfuscator().getSeparatedClasses(Lists.newArrayList(), deobfClasses);
		deobfClasses.removeIf(ClassEntry::isInnerClass);
	}

	public void show() {
		frame = new JFrame("SearchDialog classes");
		frame.setVisible(false);
		pane = new JPanel();
		pane.setBorder(new EmptyBorder(5, 10, 5, 10));

		addRow(jPanel -> {
			searchField = new JTextField("", 20);

			searchField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent keyEvent) {
					updateList();
				}
			});

			jPanel.add(searchField);
		});

		addRow(jPanel -> {
			list = new JList<>();
			list.setLayoutOrientation(JList.VERTICAL);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			list.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent mouseEvent) {
					if(mouseEvent.getClickCount() >= 2){
						select();
					}
				}
			});
			jPanel.add(list);
		});


		keyEventDispatcher = keyEvent -> {
			if(!frame.isVisible()){
				return false;
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_DOWN){
				int next = list.isSelectionEmpty() ? 0 : list.getSelectedIndex() + 1;
				list.setSelectedIndex(next);
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_UP){
				int next = list.isSelectionEmpty() ? list.getModel().getSize() : list.getSelectedIndex() - 1;
				list.setSelectedIndex(next);
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_ENTER){
				select();
			}
			if(keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE){
				close();
			}
			return false;
		};

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher);

		frame.setContentPane(pane);
		frame.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		frame.setSize(360, 500);
		frame.setAlwaysOnTop(true);
		frame.setResizable(false);
		frame.setLocationRelativeTo(gui.getFrame());
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		searchField.requestFocusInWindow();
	}

	private void select(){
		close();
		deobfClasses.stream()
			.filter(classEntry -> !classEntry.isInnerClass())
			.filter(classEntry -> classEntry.getSimpleName().equals(list.getSelectedValue())).
			findFirst()
			.ifPresent(classEntry -> {
				gui.navigateTo(classEntry);
				gui.getDeobfPanel().deobfClasses.setSelectionClass(classEntry);
			});
	}

	private void close(){
		frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyEventDispatcher);
	}

	private void addRow(Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		pane.add(panel, BorderLayout.CENTER);
	}

	private void updateList() {
		DefaultListModel<String> listModel = new DefaultListModel<>();

		List<ExtractedResult> results = FuzzySearch.extractTop(searchField.getText(), deobfClasses.stream().map(ClassEntry::getSimpleName).collect(Collectors.toList()), 25);
		results.forEach(extractedResult -> listModel.addElement(extractedResult.getString()));

		list.setModel(listModel);
	}



}
