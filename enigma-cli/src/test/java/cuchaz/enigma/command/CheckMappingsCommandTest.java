package cuchaz.enigma.command;

import java.io.File;

import org.junit.Test;

public class CheckMappingsCommandTest {
	private static final String PACKAGE_ACCESS = "../enigma/build/test-obf/packageAccess.jar";

	@Test(expected = IllegalStateException.class)
	public void testWrong() throws Exception {
		new CheckMappingsCommand().run(new File(PACKAGE_ACCESS).getAbsolutePath(), new File("src/test/resources" + "/packageAccess/wrongMappings").getAbsolutePath());
	}

	@Test
	public void testRight() throws Exception {
		new CheckMappingsCommand().run(new File(PACKAGE_ACCESS).getAbsolutePath(), new File("src/test/resources" + "/packageAccess/correctMappings").getAbsolutePath());
	}
}
