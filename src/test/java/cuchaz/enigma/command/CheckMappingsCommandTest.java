package cuchaz.enigma.command;

import org.junit.Test;

import java.io.File;

public class CheckMappingsCommandTest {

	@Test(expected = IllegalStateException.class)
	public void test() throws Exception {
		new CheckMappingsCommand().run(new File("build/test-obf/packageAccess.jar").getAbsolutePath(), new File("src/test/resources" +
				"/packageAccessMappings").getAbsolutePath());
	}
}
