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
package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.*;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator.ProgressListener;

public class ProgressDialog implements ProgressListener, AutoCloseable {

    private JFrame frame;
    private JLabel labelTitle;
    private JLabel labelText;
    private JProgressBar progress;

    public ProgressDialog(JFrame parent) {

        // init frame
        this.frame = new JFrame(Constants.NAME + " - Operation in progress");
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
        this.labelText = GuiTricks.unboldLabel(new JLabel());
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
    public void onProgress(int numDone, String message) {
        this.labelText.setText(message);
        this.progress.setValue(numDone);

        // update the frame
        this.frame.validate();
        this.frame.repaint();
    }

    public interface ProgressRunnable {
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
