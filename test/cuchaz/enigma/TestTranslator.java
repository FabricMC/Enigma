package cuchaz.enigma;

import static cuchaz.enigma.EntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.TranslationDirection;
import cuchaz.enigma.mapping.Translator;


public class TestTranslator {

	private Deobfuscator m_deobfuscator;
	private Mappings m_mappings;
	
	public TestTranslator()
	throws Exception {
		m_deobfuscator = new Deobfuscator(new JarFile("build/testTranslation.obf.jar"));
		try (InputStream in = getClass().getResourceAsStream("/cuchaz/enigma/resources/translation.mappings")) {
			m_mappings = new MappingsReader().read(new InputStreamReader(in));
			m_deobfuscator.setMappings(m_mappings);
		}
	}
	
	@Test
	public void deobfuscatingTranslations()
	throws Exception {
		Translator translator = m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating);
		assertThat(translator.translateEntry(newClass("none/a")), is(newClass("deobf/A")));
		assertThat(translator.translateEntry(newField("none/a", "a", "I")), is(newField("deobf/A", "one", "I")));
		assertThat(translator.translateEntry(newField("none/a", "a", "F")), is(newField("deobf/A", "two", "F")));
		assertThat(translator.translateEntry(newField("none/a", "a", "Ljava/lang/String;")), is(newField("deobf/A", "three", "Ljava/lang/String;")));
	}
}
