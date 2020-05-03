package cuchaz.enigma.network;

import cuchaz.enigma.translation.mapping.EntryRemapper;

import javax.swing.*;

public class IntegratedEnigmaServer extends EnigmaServer {
	public IntegratedEnigmaServer(byte[] jarChecksum, char[] password, EntryRemapper mappings, int port) {
		super(jarChecksum, password, mappings, port);
	}

	@Override
	protected void runOnThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}
}
