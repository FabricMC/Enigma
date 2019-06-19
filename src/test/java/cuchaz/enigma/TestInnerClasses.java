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

import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.junit.Test;

import java.nio.file.Paths;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestInnerClasses {

	private static final ClassEntry SimpleOuter = newClass("d");
	private static final ClassEntry SimpleInner = newClass("d$a");
	private static final ClassEntry ConstructorArgsOuter = newClass("c");
	private static final ClassEntry ConstructorArgsInner = newClass("c$a");
	private static final ClassEntry ClassTreeRoot = newClass("f");
	private static final ClassEntry ClassTreeLevel1 = newClass("f$a");
	private static final ClassEntry ClassTreeLevel2 = newClass("f$a$a");
	private static final ClassEntry ClassTreeLevel3 = newClass("f$a$a$a");
	private JarIndex index;
	private SourceProvider sourceProvider;

	public TestInnerClasses() throws Exception {
		ClassCache classCache = ClassCache.of(Paths.get("build/test-obf/innerClasses.jar"));
		index = classCache.index(ProgressListener.none());

		CompiledSourceTypeLoader typeLoader = new CompiledSourceTypeLoader(classCache);
		sourceProvider = new SourceProvider(SourceProvider.createSettings(), typeLoader);
	}

	@Test
	public void simple() {
		decompile(SimpleOuter);
	}

	@Test
	public void constructorArgs() {
		decompile(ConstructorArgsOuter);
	}

	@Test
	public void classTree() {

		// root level
		assertThat(index.getEntryIndex().hasClass(ClassTreeRoot), is(true));

		// level 1
		ClassEntry fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getSimpleName());
		assertThat(index.getEntryIndex().hasClass(fullClassEntry), is(true));

		// level 2
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getSimpleName()
			+ "$" + ClassTreeLevel2.getSimpleName());
		assertThat(index.getEntryIndex().hasClass(fullClassEntry), is(true));

		// level 3
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getSimpleName()
			+ "$" + ClassTreeLevel2.getSimpleName()
			+ "$" + ClassTreeLevel3.getSimpleName());
		assertThat(index.getEntryIndex().hasClass(fullClassEntry), is(true));
	}

	private void decompile(ClassEntry classEntry) {
		sourceProvider.getSources(classEntry.getName());
	}
}
