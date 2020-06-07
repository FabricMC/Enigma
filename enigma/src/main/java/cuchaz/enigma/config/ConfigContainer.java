package cuchaz.enigma.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigContainer {

	private Path configPath;

	private final ConfigSection root = new ConfigSection();

	public ConfigSection data() {
		return this.root;
	}

	public void save() {
		if (this.configPath == null) throw new IllegalStateException("File has no config path set!");
		try {
			Files.createDirectories(this.configPath.getParent());
			Files.write(this.configPath, this.serialize().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveAs(Path path) {
		this.configPath = path;
		this.save();
	}

	public String serialize() {
		return ConfigSerializer.structureToString(this.root);
	}

	public static ConfigContainer create() {
		return new ConfigContainer();
	}

	public static ConfigContainer getOrCreate(String name) {
		return ConfigContainer.getOrCreate(ConfigPaths.getConfigFilePath(name));
	}

	public static ConfigContainer getOrCreate(Path path) {
		ConfigContainer cc = null;
		try {
			if (Files.exists(path)) {
				String s = String.join("\n", Files.readAllLines(path));
				cc = ConfigContainer.parse(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (cc == null) {
			cc = ConfigContainer.create();
		}

		cc.configPath = path;
		return cc;
	}

	public static ConfigContainer parse(String source) {
		ConfigContainer cc = ConfigContainer.create();
		List<ConfigSection> stack = new ArrayList<>();
		ConfigSection current = cc.data();
		ConfigSerializer.parseStructure(source, new ConfigStructureVisitor() {
			@Override
			public void visitKeyValue(String key, String value) {
				stack.get(stack.size() - 1).setString(key, value);
			}

			@Override
			public void visitSection(String section) {
				stack.add(stack.get(stack.size() - 1).section(section));
			}

			@Override
			public void endSection() {
				stack.remove(stack.size() - 1);
			}
		});
		return cc;
	}

}
