package cuchaz.enigma.translation.mapping.serde.recaf;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

public class TestRecaf {
	@Test
	public void testIntegrity() throws Exception {
		Set<String> contents;

		try (InputStream in = getClass().getResourceAsStream("/recaf.mappings")) {
			contents = Sets.newHashSet(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R"));
		}

		try (FileSystem fs = Jimfs.newFileSystem()) {
			Path path = fs.getPath("recaf.mappings");
			Files.writeString(path, String.join("\n", contents));

			RecafMappingsWriter writer = RecafMappingsWriter.INSTANCE;
			RecafMappingsReader reader = RecafMappingsReader.INSTANCE;

			EntryTree<EntryMapping> mappings = reader.read(path, ProgressListener.none(), null);
			writer.write(mappings, path, ProgressListener.none(), null);

			reader.read(path, ProgressListener.none(), null);
			Set<String> newContents = new HashSet<>(Files.readAllLines(path));

			assertEquals(contents, newContents);
		}
	}
}
