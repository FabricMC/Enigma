package cuchaz.enigma.config;

import java.util.*;
import java.util.function.Function;

public class ConfigSection {

	private final Map<String, String> values;
	private final Map<String, ConfigSection> sections;

	private ConfigSection(Map<String, String> values, Map<String, ConfigSection> sections) {
		this.values = values;
		this.sections = sections;
	}

	public ConfigSection() {
		this(new HashMap<>(), new HashMap<>());
	}

	public ConfigSection section(String name) {
		return this.sections.computeIfAbsent(name, _s -> new ConfigSection());
	}

	public Map<String, String> values() {
		return Collections.unmodifiableMap(this.values);
	}

	public Map<String, ConfigSection> sections() {
		return Collections.unmodifiableMap(this.sections);
	}

	public boolean remove(String key) {
		return this.values.remove(key) != null;
	}

	public boolean removeSection(String key) {
		return this.sections.remove(key) != null;
	}

	public Optional<String> getString(String key) {
		return Optional.ofNullable(this.values.get(key));
	}

	public void setString(String key, String value) {
		this.values.put(key, value);
	}

	public String setIfAbsentString(String key, String value) {
		this.values.putIfAbsent(key, value);
		return this.values.get(key);
	}

	public Optional<Boolean> getBool(String key) {
		return ConfigSerializer.parseBool(this.values.get(key));
	}

	public boolean setIfAbsentBool(String key, boolean value) {
		return this.getBool(key).orElseGet(() -> {
			this.setBool(key, value);
			return value;
		});
	}

	public void setBool(String key, boolean value) {
		this.values.put(key, Boolean.toString(value));
	}

	public OptionalInt getInt(String key) {
		return ConfigSerializer.parseInt(this.values.get(key));
	}

	public void setInt(String key, int value) {
		this.values.put(key, Integer.toString(value));
	}

	public int setIfAbsentInt(String key, int value) {
		return this.getInt(key).orElseGet(() -> {
			this.setInt(key, value);
			return value;
		});
	}

	public OptionalDouble getDouble(String key) {
		return ConfigSerializer.parseDouble(this.values.get(key));
	}

	public void setDouble(String key, double value) {
		this.values.put(key, Double.toString(value));
	}

	public double setIfAbsentDouble(String key, double value) {
		return this.getDouble(key).orElseGet(() -> {
			this.setDouble(key, value);
			return value;
		});
	}

	public OptionalInt getRgbColor(String key) {
		return ConfigSerializer.parseRgbColor(this.values.get(key));
	}

	public void setRgbColor(String key, int value) {
		this.values.put(key, ConfigSerializer.rgbColorToString(value));
	}

	public int setIfAbsentRgbColor(String key, int value) {
		return this.getRgbColor(key).orElseGet(() -> {
			this.setRgbColor(key, value);
			return value;
		});
	}

	public Optional<String[]> getArray(String key) {
		return ConfigSerializer.parseArray(this.values.get(key));
	}

	public void setArray(String key, String[] value) {
		this.values.put(key, ConfigSerializer.arrayToString(value));
	}

	public String[] setIfAbsentArray(String key, String[] value) {
		return this.getArray(key).orElseGet(() -> {
			this.setArray(key, value);
			return value;
		});
	}

	public Optional<int[]> getIntArray(String key) {
		return this.getArray(key).map(arr -> Arrays.stream(arr).mapToInt(s -> ConfigSerializer.parseInt(s).orElse(0)).toArray());
	}

	public void setIntArray(String key, int[] value) {
		this.setArray(key, Arrays.stream(value).mapToObj(Integer::toString).toArray(String[]::new));
	}

	public int[] setIfAbsentIntArray(String key, int[] value) {
		return this.getIntArray(key).orElseGet(() -> {
			this.setIntArray(key, value);
			return value;
		});
	}

	public <T extends Enum<T>> Optional<T> getEnum(Function<String, T> byName, String key) {
		return ConfigSerializer.parseEnum(byName, this.values.get(key));
	}

	public <T extends Enum<T>> void setEnum(String key, T value) {
		this.values.put(key, value.name());
	}

	public <T extends Enum<T>> T setIfAbsentEnum(Function<String, T> byName, String key, T value) {
		return this.getEnum(byName, key).orElseGet(() -> {
			this.setEnum(key, value);
			return value;
		});
	}

	public ConfigSection copy() {
		Map<String, ConfigSection> sections = new HashMap<>(this.sections);
		sections.replaceAll((k, v) -> v.copy());
		return new ConfigSection(new HashMap<>(this.values), sections);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ConfigSection)) return false;
		ConfigSection that = (ConfigSection) o;
		return values.equals(that.values) &&
				sections.equals(that.sections);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values, sections);
	}

	@Override
	public String toString() {
		return String.format("ConfigSection { values: %s, sections: %s }", values, sections);
	}

}
