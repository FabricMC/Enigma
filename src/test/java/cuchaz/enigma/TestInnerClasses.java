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

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.ClassEntry;
import org.junit.Test;

import java.util.jar.JarFile;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class TestInnerClasses {

	private static final ClassEntry AnonymousOuter = newClass("a");
	private static final ClassEntry AnonymousInner = newClass("a$1");
	private static final ClassEntry SimpleOuter = newClass("d");
	private static final ClassEntry SimpleInner = newClass("d$a");
	private static final ClassEntry ConstructorArgsOuter = newClass("c");
	private static final ClassEntry ConstructorArgsInner = newClass("c$a");
	private static final ClassEntry AnonymousWithScopeArgsOuter = newClass("b");
	private static final ClassEntry AnonymousWithScopeArgsInner = newClass("b$1");
	private static final ClassEntry AnonymousWithOuterAccessOuter = newClass("e");
	private static final ClassEntry AnonymousWithOuterAccessInner = newClass("e$1");
	private static final ClassEntry ClassTreeRoot = newClass("f");
	private static final ClassEntry ClassTreeLevel1 = newClass("f$a");
	private static final ClassEntry ClassTreeLevel2 = newClass("f$a$a");
	private static final ClassEntry ClassTreeLevel3 = newClass("f$a$a$a");
	private JarIndex index;
	private Deobfuscator deobfuscator;

	public TestInnerClasses()
		throws Exception {
		index = new JarIndex();
		JarFile jar = new JarFile("build/test-obf/innerClasses.jar");
		index.indexJar(jar, true);
		deobfuscator = new Deobfuscator(jar);
	}

	@Test
	public void simple() {
		assertThat(index.getOuterClass(SimpleInner), is(SimpleOuter));
		assertThat(index.getInnerClasses(SimpleOuter), containsInAnyOrder(SimpleInner));
		assertThat(index.isAnonymousClass(SimpleInner), is(false));
		decompile(SimpleOuter);
	}

	@Test
	public void anonymous() {
		assertThat(index.getOuterClass(AnonymousInner), is(AnonymousOuter));
		assertThat(index.getInnerClasses(AnonymousOuter), containsInAnyOrder(AnonymousInner));
		assertThat(index.isAnonymousClass(AnonymousInner), is(true));
		decompile(AnonymousOuter);
	}

	@Test
	public void constructorArgs() {
		assertThat(index.getOuterClass(ConstructorArgsInner), is(ConstructorArgsOuter));
		assertThat(index.getInnerClasses(ConstructorArgsOuter), containsInAnyOrder(ConstructorArgsInner));
		assertThat(index.isAnonymousClass(ConstructorArgsInner), is(false));
		decompile(ConstructorArgsOuter);
	}

	@Test
	public void anonymousWithScopeArgs() {
		assertThat(index.getOuterClass(AnonymousWithScopeArgsInner), is(AnonymousWithScopeArgsOuter));
		assertThat(index.getInnerClasses(AnonymousWithScopeArgsOuter), containsInAnyOrder(AnonymousWithScopeArgsInner));
		assertThat(index.isAnonymousClass(AnonymousWithScopeArgsInner), is(true));
		decompile(AnonymousWithScopeArgsOuter);
	}

	@Test
	public void anonymousWithOuterAccess() {
		assertThat(index.getOuterClass(AnonymousWithOuterAccessInner), is(AnonymousWithOuterAccessOuter));
		assertThat(index.getInnerClasses(AnonymousWithOuterAccessOuter), containsInAnyOrder(AnonymousWithOuterAccessInner));
		assertThat(index.isAnonymousClass(AnonymousWithOuterAccessInner), is(true));
		decompile(AnonymousWithOuterAccessOuter);
	}

	@Test
	public void classTree() {

		// root level
		assertThat(index.containsObfClass(ClassTreeRoot), is(true));
		assertThat(index.getOuterClass(ClassTreeRoot), is(nullValue()));
		assertThat(index.getInnerClasses(ClassTreeRoot), containsInAnyOrder(ClassTreeLevel1));

		// level 1
		ClassEntry fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getInnermostClassName()
		);
		assertThat(index.containsObfClass(fullClassEntry), is(true));
		assertThat(index.getOuterClass(ClassTreeLevel1), is(ClassTreeRoot));
		assertThat(index.getInnerClasses(ClassTreeLevel1), containsInAnyOrder(ClassTreeLevel2));

		// level 2
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getInnermostClassName()
			+ "$" + ClassTreeLevel2.getInnermostClassName()
		);
		assertThat(index.containsObfClass(fullClassEntry), is(true));
		assertThat(index.getOuterClass(ClassTreeLevel2), is(ClassTreeLevel1));
		assertThat(index.getInnerClasses(ClassTreeLevel2), containsInAnyOrder(ClassTreeLevel3));

		// level 3
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getInnermostClassName()
			+ "$" + ClassTreeLevel2.getInnermostClassName()
			+ "$" + ClassTreeLevel3.getInnermostClassName()
		);
		assertThat(index.containsObfClass(fullClassEntry), is(true));
		assertThat(index.getOuterClass(ClassTreeLevel3), is(ClassTreeLevel2));
		assertThat(index.getInnerClasses(ClassTreeLevel3), is(empty()));
	}

	private void decompile(ClassEntry classEntry) {
		deobfuscator.getSourceTree(classEntry.getName());
	}
}
