package cuchaz.enigma.translation.mapping;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Pair;

/**
 * Tests that a MappingFormat can write out a fixed set of mappings and read them back without losing any information.
 * Javadoc skipped for Tiny (v1) as it doesn't support them.
 */
public class TestReadWriteCycle {
	private final MappingSaveParameters parameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);

	private final Pair<ClassEntry, EntryMapping> testClazz = new Pair<>(new ClassEntry("a/b/c"), new EntryMapping("alpha/beta/charlie", "this is a test class"));

	private final Pair<FieldEntry, EntryMapping> testField1 = new Pair<>(FieldEntry.parse("a/b/c", "field1", "I"), new EntryMapping("mapped1", "this is field 1"));

	private final Pair<FieldEntry, EntryMapping> testField2 = new Pair<>(FieldEntry.parse("a/b/c", "field2", "I"), new EntryMapping("mapped2", "this is field 2"));

	private final Pair<MethodEntry, EntryMapping> testMethod1 = new Pair<>(MethodEntry.parse("a/b/c", "method1", "()V"), new EntryMapping("mapped3", "this is method1"));

	private final Pair<MethodEntry, EntryMapping> testMethod2 = new Pair<>(MethodEntry.parse("a/b/c", "method2", "()V"), new EntryMapping("mapped4", "this is method 2"));

	private void insertMapping(EntryTree<EntryMapping> mappings, Pair<? extends Entry<?>, EntryMapping> mappingPair) {
		mappings.insert(mappingPair.a, mappingPair.b);
	}

	private void testReadWriteCycle(MappingFormat mappingFormat, boolean supportsJavadoc, String tmpNameSuffix) throws IOException, MappingParseException {
		//construct some known mappings to test with
		EntryTree<EntryMapping> testMappings = new HashEntryTree<>();
		insertMapping(testMappings, testClazz);
		insertMapping(testMappings, testField1);
		insertMapping(testMappings, testField2);
		insertMapping(testMappings, testMethod1);
		insertMapping(testMappings, testMethod2);

		Assert.assertTrue("Test mapping insertion failed: testClazz", testMappings.contains(testClazz.a));
		Assert.assertTrue("Test mapping insertion failed: testField1", testMappings.contains(testField1.a));
		Assert.assertTrue("Test mapping insertion failed: testField2", testMappings.contains(testField2.a));
		Assert.assertTrue("Test mapping insertion failed: testMethod1", testMappings.contains(testMethod1.a));
		Assert.assertTrue("Test mapping insertion failed: testMethod2", testMappings.contains(testMethod2.a));

		File tempFile = File.createTempFile("readWriteCycle", tmpNameSuffix);
		//remove the auto created file
		tempFile.delete();

		mappingFormat.write(testMappings, tempFile.toPath(), ProgressListener.none(), parameters);
		Assert.assertTrue("Written file not created", tempFile.exists());

		EntryTree<EntryMapping> loadedMappings = mappingFormat.read(tempFile.toPath(), ProgressListener.none(), parameters, null);

		Assert.assertTrue("Loaded mappings don't contain testClazz", loadedMappings.contains(testClazz.a));
		Assert.assertTrue("Loaded mappings don't contain testField1", loadedMappings.contains(testField1.a));
		Assert.assertTrue("Loaded mappings don't contain testField2", loadedMappings.contains(testField2.a));
		Assert.assertTrue("Loaded mappings don't contain testMethod1", loadedMappings.contains(testMethod1.a));
		Assert.assertTrue("Loaded mappings don't contain testMethod2", loadedMappings.contains(testMethod2.a));

		Assert.assertEquals("Incorrect mapping: testClazz", testClazz.b.targetName(), loadedMappings.get(testClazz.a).targetName());
		Assert.assertEquals("Incorrect mapping: testField1", testField1.b.targetName(), loadedMappings.get(testField1.a).targetName());
		Assert.assertEquals("Incorrect mapping: testField2", testField2.b.targetName(), loadedMappings.get(testField2.a).targetName());
		Assert.assertEquals("Incorrect mapping: testMethod1", testMethod1.b.targetName(), loadedMappings.get(testMethod1.a).targetName());
		Assert.assertEquals("Incorrect mapping: testMethod2", testMethod2.b.targetName(), loadedMappings.get(testMethod2.a).targetName());

		if (supportsJavadoc) {
			Assert.assertEquals("Incorrect javadoc: testClazz", testClazz.b.javadoc(), loadedMappings.get(testClazz.a).javadoc());
			Assert.assertEquals("Incorrect javadoc: testField1", testField1.b.javadoc(), loadedMappings.get(testField1.a).javadoc());
			Assert.assertEquals("Incorrect javadoc: testField2", testField2.b.javadoc(), loadedMappings.get(testField2.a).javadoc());
			Assert.assertEquals("Incorrect javadoc: testMethod1", testMethod1.b.javadoc(), loadedMappings.get(testMethod1.a).javadoc());
			Assert.assertEquals("Incorrect javadoc: testMethod2", testMethod2.b.javadoc(), loadedMappings.get(testMethod2.a).javadoc());
		}

		tempFile.delete();
	}

	@BeforeClass
	public static void setup() {
		System.getProperties().setProperty("enigma.use_mappingio", "false");
	}

	@Test
	public void testEnigmaFile() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.ENIGMA_FILE, true, ".enigma");
	}

	@Test
	public void testEnigmaDir() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.ENIGMA_DIRECTORY, true, ".tmp");
	}

	@Test
	public void testTinyFile() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.TINY_FILE, false, ".tiny");
	}

	@Test
	public void testTinyV2() throws IOException, MappingParseException {
		testReadWriteCycle(MappingFormat.TINY_V2, true, ".tinyv2");
	}
}
