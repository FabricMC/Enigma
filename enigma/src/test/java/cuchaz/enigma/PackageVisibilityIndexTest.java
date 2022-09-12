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

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.analysis.index.PackageVisibilityIndex;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class PackageVisibilityIndexTest {
	public static final Path JAR = Paths.get("build/test-obf/packageAccess.jar");
	private static final ClassEntry KEEP = newClass("cuchaz/enigma/inputs/Keep");
	private static final ClassEntry BASE = newClass("a");
	private static final ClassEntry SAME_PACKAGE_CHILD = newClass("b");
	private static final ClassEntry SAME_PACKAGE_CHILD_INNER = newClass("b$a");
	private static final ClassEntry OTHER_PACKAGE_CHILD = newClass("c");
	private static final ClassEntry OTHER_PACKAGE_CHILD_INNER = newClass("c$a");
	private final JarIndex jarIndex;

	public PackageVisibilityIndexTest() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		jarIndex = JarIndex.empty();
		jarIndex.indexJar(jcp.getClassNames(), jcp, ProgressListener.none());
	}

	@Test
	public void test() {
		PackageVisibilityIndex visibilityIndex = jarIndex.getPackageVisibilityIndex();
		assertThat(visibilityIndex.getPartition(BASE), containsInAnyOrder(BASE, SAME_PACKAGE_CHILD, SAME_PACKAGE_CHILD_INNER));
		System.out.println(visibilityIndex.getPartitions());
		assertThat(visibilityIndex.getPartitions(), containsInAnyOrder(containsInAnyOrder(BASE, SAME_PACKAGE_CHILD, SAME_PACKAGE_CHILD_INNER), containsInAnyOrder(OTHER_PACKAGE_CHILD, OTHER_PACKAGE_CHILD_INNER), contains(KEEP)));
	}
}
