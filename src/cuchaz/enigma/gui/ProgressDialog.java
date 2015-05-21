/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator.ProgressListener;

public class ProgressDialog implements ProgressListener, AutoCloseable {
	
	private JFrame m_frame;
	private JLabel m_title;
	private JLabel m_text;
	private JProgressBar m_progress;
	
	public ProgressDialog(JFrame parent) {
		
		// init frame
		m_frame = new JFrame(Constants.Name + " - Operation in progress");
		final Container pane = m_frame.getContentPane();
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.LEFT);
		pane.setLayout(layout);
		
		m_title = new JLabel();
		pane.add(m_title);
		
		// set up the progress bar
		JPanel panel = new JPanel();
		pane.add(panel);
		panel.setLayout(new BorderLayout());
		m_text = GuiTricks.unboldLabel(new JLabel());
		m_progress = new JProgressBar();
		m_text.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		panel.add(m_text, BorderLayout.NORTH);
		panel.add(m_progress, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(360, 50));
		
		// show the frame
		pane.doLayout();
		m_frame.setSize(400, 120);
		m_frame.setResizable(false);
		m_frame.setLocationRelativeTo(parent);
		m_frame.setVisible(true);
		m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}
	
	public void close() {
		m_frame.dispose();
	}
	
	@Override
	public void init(int totalWork, String title) {
		m_title.setText(title);
		m_progress.setMinimum(0);
		m_progress.setMaximum(totalWork);
		m_progress.setValue(0);
	}
	
	@Override
	public void onProgress(int numDone, String message) {
		m_text.setText(message);
		m_progress.setValue(numDone);
		
		// update the frame
		m_frame.validate();
		m_frame.repaint();
	}
	
	public static interface ProgressRunnable {
		void run(ProgressListener listener) throws Exception;
	}
	
	public static void runInThread(final JFrame parent, final ProgressRunnable runnable) {
		new Thread() {
			@Override
			public void run() {
				try (ProgressDialog progress = new ProgressDialog(parent)) {
					runnable.run(progress);
				} catch (Exception ex) {
					throw new Error(ex);
				}
			}
		}.start();
	}
}
