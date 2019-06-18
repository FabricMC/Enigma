/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

public class TestDeobfuscator {

	private EnigmaProject openProject() throws IOException {
		Enigma enigma = Enigma.create();
		return enigma.openJar(Paths.get("build/test-obf/loneClass.jar"), ProgressListener.none());
	}

	@Test
	public void loadJar()
		throws Exception {
		openProject();
	}

	@Test
	public void decompileClass() throws Exception {
		EnigmaProject project = openProject();

		CompiledSourceTypeLoader typeLoader = new CompiledSourceTypeLoader(project.getClassCache());
		SourceProvider sourceProvider = new SourceProvider(SourceProvider.createSettings(), typeLoader);

		sourceProvider.writeSourceToString(sourceProvider.getSources("a"));
	}
}
