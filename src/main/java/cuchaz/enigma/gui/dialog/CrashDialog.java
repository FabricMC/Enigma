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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.*;

import cuchaz.enigma.Constants;
import cuchaz.enigma.utils.Utils;

public class CrashDialog {

    private static CrashDialog instance = null;

    private JFrame    frame;
    private JTextArea text;

    private CrashDialog(JFrame parent) {
        // init frame
        frame = new JFrame(Constants.NAME + " - Crash Report");
        final Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        JLabel label = new JLabel(Constants.NAME + " has crashed! =(");
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.add(label, BorderLayout.NORTH);

        // report panel
        text = new JTextArea();
        text.setTabSize(2);
        pane.add(new JScrollPane(text), BorderLayout.CENTER);

        // buttons panel
        JPanel buttonsPanel = new JPanel();
        FlowLayout buttonsLayout = new FlowLayout();
        buttonsLayout.setAlignment(FlowLayout.RIGHT);
        buttonsPanel.setLayout(buttonsLayout);
        buttonsPanel.add(Utils.unboldLabel(new JLabel("If you choose exit, you will lose any unsaved work.")));
        JButton ignoreButton = new JButton("Ignore");
        ignoreButton.addActionListener(event -> {
            // close (hide) the dialog
            frame.setVisible(false);
        });
        buttonsPanel.add(ignoreButton);
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(event -> {
            // exit enigma
            System.exit(1);
        });
        buttonsPanel.add(exitButton);
        pane.add(buttonsPanel, BorderLayout.SOUTH);

        // show the frame
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(parent);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    public static void init(JFrame parent) {
        instance = new CrashDialog(parent);
    }

    public static void show(Throwable ex) {
        // get the error report
        StringWriter buf = new StringWriter();
        ex.printStackTrace(new PrintWriter(buf));
        String report = buf.toString();

        // show it!
        instance.text.setText(report);
        instance.frame.doLayout();
        instance.frame.setVisible(true);
    }
}
