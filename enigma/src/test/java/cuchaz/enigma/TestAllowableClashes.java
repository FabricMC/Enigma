package cuchaz.enigma;

import java.io.IOException;
import java.nio.file.Paths;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.validation.ValidationContext;

/**
 * Test that we can accept some name clashes that are allowed by javac
 */
public class TestAllowableClashes {

	@Test
	public void test() throws IOException, MappingParseException {
		//Load produced mappings
		Enigma enigma = Enigma.create();
		EnigmaProject project = enigma.openJar(Paths.get("build/test-obf/visibility.jar"), new ClasspathClassProvider(), ProgressListener.none());
		EntryTree<EntryMapping> obfToDeobf = MappingFormat.PROGUARD.read(Paths.get("build/visibility-mapping.txt"), ProgressListener.none(), null);

		//Load them into enigma, none should conflict
		EntryRemapper mapper = project.getMapper();
		for (int round=0; round<2; round++) {
			for (EntryTreeNode<EntryMapping> node : obfToDeobf) {
				Assert.assertNotEquals(null, node.getValue());
				if (node.getEntry() instanceof MethodEntry && (node.getEntry()
						.getName()
						.equals("<init>") || node.getEntry().getName().equals("<clinit>"))) {
					//skip proguard's constructor entries
					continue;
				}
				System.out.println(node.getEntry().toString() + " -> " + node.getValue().getTargetName());
				ValidationContext vc = new ValidationContext();
				mapper.mapFromObf(vc, node.getEntry(), node.getValue());
				MatcherAssert.assertThat(vc, ValidationContextMatcher.INSTANCE);
			}
		}
	}

}
