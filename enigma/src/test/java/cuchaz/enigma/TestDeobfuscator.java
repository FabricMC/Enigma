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

package cuchaz.enigma;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;

import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Decompilers;
import cuchaz.enigma.source.SourceSettings;

public class TestDeobfuscator {
	private EnigmaProject openProject() throws IOException {
		Enigma enigma = Enigma.create();
		return enigma.openJar(Paths.get("build/test-obf/loneClass.jar"), new ClasspathClassProvider(), ProgressListener.none());
	}

	@Test
	public void loadJar() throws Exception {
		openProject();
	}

	@Test
	public void decompileClass() throws Exception {
		EnigmaProject project = openProject();
		Decompiler decompiler = Decompilers.CFR.create(project.getClassProvider(), new SourceSettings(false, false));

		decompiler.getSource("a").asString();
	}
}
