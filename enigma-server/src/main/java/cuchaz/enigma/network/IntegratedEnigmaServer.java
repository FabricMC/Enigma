package cuchaz.enigma.network;

import javax.swing.SwingUtilities;

import cuchaz.enigma.translation.mapping.EntryRemapper;

public class IntegratedEnigmaServer extends EnigmaServer {
	public IntegratedEnigmaServer(byte[] jarChecksum, char[] password, EntryRemapper mappings, int port) {
		super(jarChecksum, password, mappings, port);
	}

	@Override
	protected void runOnThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}
}
