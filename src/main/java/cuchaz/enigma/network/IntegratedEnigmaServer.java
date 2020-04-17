package cuchaz.enigma.network;

import cuchaz.enigma.translation.mapping.EntryRemapper;

import javax.swing.*;

public class IntegratedEnigmaServer extends EnigmaServer {
	public IntegratedEnigmaServer(byte[] jarChecksum, EntryRemapper mappings, int port) {
		super(jarChecksum, mappings, port);
	}

	@Override
	protected void runOnThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}
}
