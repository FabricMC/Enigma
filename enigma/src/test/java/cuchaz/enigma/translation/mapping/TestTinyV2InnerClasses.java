/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
*     Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation.mapping;

import java.nio.file.Path;
import java.nio.file.Paths;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsReader;

public final class TestTinyV2InnerClasses {
	private Path jar;
	private Path mappings;

	public TestTinyV2InnerClasses() throws Exception {
		jar = Paths.get("build/test-obf/innerClasses.jar");
		mappings = Paths.get(TestTinyV2InnerClasses.class.getResource("/tinyV2InnerClasses/").toURI());
	}

	//	@Test
	public void testMappings() throws Exception {
		EnigmaProject project = Enigma.create().openJar(jar, new ClasspathClassProvider(), ProgressListener.none());
		project.setMappings(EnigmaMappingsReader.DIRECTORY.read(mappings, ProgressListener.none(), project.getEnigma().getProfile().getMappingSaveParameters()));
	}
}
