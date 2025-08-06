/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.gui.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.CompletableFuture;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

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

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2).anchor(GridBagConstraints.WEST).fill(GridBagConstraints.BOTH).weight(1.0, 0.0);

		pane.add(this.labelTitle, cb.pos(0, 0).build());
		pane.add(this.labelText, cb.pos(0, 1).build());
		pane.add(this.progress, cb.pos(0, 2).weight(1.0, 1.0).build());

		// Set label text since otherwise the label height is 0, which makes the
		// window size get set incorrectly
		this.labelTitle.setText("Idle");
		this.labelText.setText("Idle");
		this.progress.setPreferredSize(ScaleUtil.getDimension(0, 20));

		// show the frame
		this.dialog.setResizable(false);
		this.reposition();
		this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	// This tries to set the window size to the smallest it can be vertically,
	// and 400 units in width.
	// Gets called twice, including after the window opens to try to fix the
	// window size (more specifically, the progress bar size) being smaller when
	// the dialog opens for the very first time compared to afterwards. (#366)
	private void reposition() {
		this.dialog.pack();
		Dimension size = this.dialog.getSize();
		this.dialog.setMinimumSize(size);
		size.width = ScaleUtil.scale(400);
		this.dialog.setSize(size);

		this.dialog.setLocationRelativeTo(this.dialog.getParent());
	}

	public static CompletableFuture<Void> runOffThread(final JFrame parent, final ProgressRunnable runnable) {
		return CompletableFuture.supplyAsync(() -> {
			ProgressDialog progress = new ProgressDialog(parent);

			// Somehow opening the dialog, disposing it, then reopening it
			// and then repositioning it fixes the size issues detailed above
			// most of the time.
			// Using setVisible(false) instead of dispose() does not work as
			// well.
			// Don't ask me why.
			progress.dialog.setVisible(true);
			progress.dialog.dispose();
			progress.dialog.setVisible(true);
			progress.reposition();

			return progress;
		}, SwingUtilities::invokeLater).thenAcceptAsync(progress -> {
			try (progress) {
				runnable.run(progress);
			} catch (Throwable e) {
				CrashDialog.show(e);
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void close() {
		SwingUtilities.invokeLater(this.dialog::dispose);
	}

	@Override
	public void init(int totalWork, String title) {
		SwingUtilities.invokeLater(() -> {
			this.labelTitle.setText(title);
			this.progress.setMinimum(0);
			this.progress.setMaximum(totalWork);
			this.progress.setValue(0);
		});
	}

	@Override
	public void step(int numDone, String message) {
		SwingUtilities.invokeLater(() -> {
			this.labelText.setText(message);

			if (numDone != -1) {
				this.progress.setValue(numDone);
				this.progress.setIndeterminate(false);
			} else {
				this.progress.setIndeterminate(true);
			}
		});
	}

	public interface ProgressRunnable {
		void run(ProgressListener listener) throws Exception;
	}
}
