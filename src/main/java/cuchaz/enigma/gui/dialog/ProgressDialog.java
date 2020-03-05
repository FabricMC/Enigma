/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.Constants;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class ProgressDialog implements ProgressListener, AutoCloseable {

	private JFrame frame;
	private JLabel labelTitle;
	private JLabel labelText;
	private JProgressBar progress;

	public ProgressDialog(JFrame parent) {

		// init frame
		this.frame = new JFrame(String.format(I18n.translate("progress.operation"), Constants.NAME));
		final Container pane = this.frame.getContentPane();
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.LEFT);
		pane.setLayout(layout);

		this.labelTitle = new JLabel();
		pane.add(this.labelTitle);

		// set up the progress bar
		JPanel panel = new JPanel();
		pane.add(panel);
		panel.setLayout(new BorderLayout());
		this.labelText = Utils.unboldLabel(new JLabel());
		this.progress = new JProgressBar();
		this.labelText.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		panel.add(this.labelText, BorderLayout.NORTH);
		panel.add(this.progress, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(360, 50));

		// show the frame
		pane.doLayout();
		this.frame.setSize(400, 120);
		this.frame.setResizable(false);
		this.frame.setLocationRelativeTo(parent);
		this.frame.setVisible(true);
		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	public static CompletableFuture<Void> runOffThread(final JFrame parent, final ProgressRunnable runnable) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		new Thread(() ->
		{
			try (ProgressDialog progress = new ProgressDialog(parent)) {
				runnable.run(progress);
				future.complete(null);
			} catch (Exception ex) {
				future.completeExceptionally(ex);
				throw new Error(ex);
			}
		}).start();
		return future;
	}

	@Override
	public void close() {
		this.frame.dispose();
	}

	@Override
	public void init(int totalWork, String title) {
		this.labelTitle.setText(title);
		this.progress.setMinimum(0);
		this.progress.setMaximum(totalWork);
		this.progress.setValue(0);
	}

	@Override
	public void step(int numDone, String message) {
		this.labelText.setText(message);
		if (numDone != -1) {
			this.progress.setValue(numDone);
			this.progress.setIndeterminate(false);
		} else {
			this.progress.setIndeterminate(true);
		}

		// update the frame
		this.frame.validate();
		this.frame.repaint();
	}

	public interface ProgressRunnable {
		void run(ProgressListener listener) throws Exception;
	}
}
