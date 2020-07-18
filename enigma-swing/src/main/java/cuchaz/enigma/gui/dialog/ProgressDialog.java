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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.CompletableFuture;

import javax.swing.*;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public class ProgressDialog implements ProgressListener, AutoCloseable {

	private final JDialog dialog;
	private final JLabel labelTitle = new JLabel();
	private final JLabel labelText = GuiUtil.unboldLabel(new JLabel());
	private final JProgressBar progress = new JProgressBar();

	public ProgressDialog(JFrame parent) {
		// init frame
		this.dialog = new JDialog(parent, String.format(I18n.translate("progress.operation"), Enigma.NAME));
		Container pane = this.dialog.getContentPane();
		pane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
				.insets(2)
				.anchor(GridBagConstraints.WEST)
				.fill(GridBagConstraints.BOTH)
				.weight(1.0, 0.0);

		// Set label text since otherwise the label height is 0, which makes the
		// window size get set incorrectly
		this.labelTitle.setText("Idle");
		this.labelText.setText("Idle");
		this.progress.setPreferredSize(ScaleUtil.getDimension(0, 30));

		pane.add(this.labelTitle, cb.pos(0, 0).build());
		pane.add(this.labelText, cb.pos(0, 1).build());
		// set padding because otherwise the progress bar gets cut off when
		// using the Darkula theme
		pane.add(this.progress, cb.pos(0, 2).weight(1.0, 1.0).padding(10).build());

		// show the frame
		this.dialog.pack();
		Dimension size = this.dialog.getSize();
		this.dialog.setMinimumSize(size);
		size.width = ScaleUtil.scale(400);
		this.dialog.setSize(size);

		this.dialog.setResizable(false);
		this.dialog.setLocationRelativeTo(parent);
		this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.dialog.setVisible(true);
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
		this.dialog.dispose();
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
		this.dialog.validate();
		this.dialog.repaint();
	}

	public interface ProgressRunnable {
		void run(ProgressListener listener) throws Exception;
	}
}
