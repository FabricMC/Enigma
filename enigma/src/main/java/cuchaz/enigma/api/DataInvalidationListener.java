package cuchaz.enigma.api;

@FunctionalInterface
public interface DataInvalidationListener {
	void onDataInvalidated(DataInvalidationEvent event);
}
