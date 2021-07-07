package cuchaz.enigma.gui.elements;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

public class MainWindow {
	private final JFrame frame;
	private final JPanel workArea = new JPanel();

	private final JMenuBar menuBar = new JMenuBar();
	private final StatusBar statusBar = new StatusBar();

	public MainWindow(String title) {
		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(this.workArea, BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);
	}

	public void setVisible(boolean visible) {
		this.frame.setVisible(visible);
	}

	public JMenuBar menuBar() {
		return this.menuBar;
	}

	public StatusBar statusBar() {
		return this.statusBar;
	}

	public Container workArea() {
		return this.workArea;
	}

	public JFrame frame() {
		return this.frame;
	}

	public void setTitle(String title) {
		this.frame.setTitle(title);
	}
}
