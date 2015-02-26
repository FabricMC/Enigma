package cuchaz.enigma;


import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.BeforeClass;
import org.junit.Test;

import cuchaz.enigma.analysis.JarIndex;


public class TestDeobfed {

	private static JarFile m_jar;
	private static JarIndex m_index;
	
	@BeforeClass
	public static void beforeClass()
	throws Exception {
		m_jar = new JarFile("build/testTranslation.deobf.jar");
		m_index = new JarIndex();
		m_index.indexJar(m_jar, true);
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
	
	@Test
	public void decompile()
	throws Exception {
		Deobfuscator deobfuscator = new Deobfuscator(m_jar);
		deobfuscator.getSourceTree("none/a");
		deobfuscator.getSourceTree("none/b");
		deobfuscator.getSourceTree("none/c");
		deobfuscator.getSourceTree("none/d");
		deobfuscator.getSourceTree("none/d$e");
		deobfuscator.getSourceTree("none/f");
		deobfuscator.getSourceTree("none/g");
		deobfuscator.getSourceTree("none/h");
		deobfuscator.getSourceTree("none/h$i");
		deobfuscator.getSourceTree("none/h$i$j");
		deobfuscator.getSourceTree("none/h$k");
		deobfuscator.getSourceTree("none/h$k$l");
		deobfuscator.getSourceTree("none/m");
		deobfuscator.getSourceTree("none/m$n");
		deobfuscator.getSourceTree("none/m$n$o");
		deobfuscator.getSourceTree("none/m$p");
		deobfuscator.getSourceTree("none/m$p$q");
		deobfuscator.getSourceTree("none/m$p$q$r");
		deobfuscator.getSourceTree("none/m$p$q$s");
	}
}
