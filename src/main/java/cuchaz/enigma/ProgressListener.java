package cuchaz.enigma;

public interface ProgressListener {
	void init(int totalWork, String title);

	void step(int numDone, String message);
}
