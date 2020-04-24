package cuchaz.enigma.utils.search;

import java.util.List;

public interface SearchEntry {

	List<String> getSearchableNames();

	/**
	 * Returns a type that uniquely identifies this search entry across possible changes.
	 * This is used for tracking the amount of times this entry has been selected.
	 *
	 * @return a unique identifier for this search entry
	 */
	String getIdentifier();

}
