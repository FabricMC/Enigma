package cuchaz.enigma;


import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.BeforeClass;
import org.junit.Test;

import cuchaz.enigma.analysis.JarIndex;


public class TestJarIndexDeobfed {

	private static JarIndex m_index;
	
	@BeforeClass
	public static void beforeClass()
	throws Exception {
		m_index = new JarIndex();
		m_index.indexJar(new JarFile("build/testTranslation.deobf.jar"), true);
	}
	
	@Test
	public void obfEntries() {
		assertThat(m_index.getObfClassEntries(), containsInAnyOrder(
			newClass("cuchaz/enigma/inputs/Keep"),
			newClass("none/a"),
			newClass("none/b"),
			newClass("none/c"),
			newClass("none/d"),
			newClass("none/d$e"),
			newClass("none/f"),
			newClass("none/g"),
			newClass("none/h"),
			newClass("none/h$i"),
			newClass("none/h$i$j"),
			newClass("none/h$k"),
			newClass("none/h$k$l"),
			newClass("none/m"),
			newClass("none/m$n"),
			newClass("none/m$n$o"),
			newClass("none/m$p"),
			newClass("none/m$p$q"),
			newClass("none/m$p$q$r"),
			newClass("none/m$p$q$s")
		));
	}
}
