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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.Test;

import static cuchaz.enigma.TestEntryFactory.*;

import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.mapping.ClassEntry;

public class TestInnerClasses {
	
	private JarIndex m_index;
	private Deobfuscator m_deobfuscator;
	
	private static final ClassEntry AnonymousOuter = newClass("none/a");
	private static final ClassEntry AnonymousInner = newClass("none/a$1");
	private static final ClassEntry SimpleOuter = newClass("none/d");
	private static final ClassEntry SimpleInner = newClass("none/d$a");
	private static final ClassEntry ConstructorArgsOuter = newClass("none/c");
	private static final ClassEntry ConstructorArgsInner = newClass("none/c$a");
	private static final ClassEntry AnonymousWithScopeArgsOuter = newClass("none/b");
	private static final ClassEntry AnonymousWithScopeArgsInner = newClass("none/b$1");
	private static final ClassEntry AnonymousWithOuterAccessOuter = newClass("none/e");
	private static final ClassEntry AnonymousWithOuterAccessInner = newClass("none/e$1");
	private static final ClassEntry ClassTreeRoot = newClass("none/f");
	private static final ClassEntry ClassTreeLevel1 = newClass("none/f$a");
	private static final ClassEntry ClassTreeLevel2 = newClass("none/f$a$a");
	private static final ClassEntry ClassTreeLevel3 = newClass("none/f$a$a$a");
	
	public TestInnerClasses()
	throws Exception {
		m_index = new JarIndex();
		JarFile jar = new JarFile("build/test-obf/innerClasses.jar");
		m_index.indexJar(jar, true);
		m_deobfuscator = new Deobfuscator(jar);
	}
	
	@Test
	public void simple() {
		assertThat(m_index.getOuterClass(SimpleInner), is(SimpleOuter));
		assertThat(m_index.getInnerClasses(SimpleOuter), containsInAnyOrder(SimpleInner));
		assertThat(m_index.isAnonymousClass(SimpleInner), is(false));
		decompile(SimpleOuter);
	}
	
	@Test
	public void anonymous() {
		assertThat(m_index.getOuterClass(AnonymousInner), is(AnonymousOuter));
		assertThat(m_index.getInnerClasses(AnonymousOuter), containsInAnyOrder(AnonymousInner));
		assertThat(m_index.isAnonymousClass(AnonymousInner), is(true));
		decompile(AnonymousOuter);
	}
	
	@Test
	public void constructorArgs() {
		assertThat(m_index.getOuterClass(ConstructorArgsInner), is(ConstructorArgsOuter));
		assertThat(m_index.getInnerClasses(ConstructorArgsOuter), containsInAnyOrder(ConstructorArgsInner));
		assertThat(m_index.isAnonymousClass(ConstructorArgsInner), is(false));
		decompile(ConstructorArgsOuter);
	}
	
	@Test
	public void anonymousWithScopeArgs() {
		assertThat(m_index.getOuterClass(AnonymousWithScopeArgsInner), is(AnonymousWithScopeArgsOuter));
		assertThat(m_index.getInnerClasses(AnonymousWithScopeArgsOuter), containsInAnyOrder(AnonymousWithScopeArgsInner));
		assertThat(m_index.isAnonymousClass(AnonymousWithScopeArgsInner), is(true));
		decompile(AnonymousWithScopeArgsOuter);
	}
	
	@Test
	public void anonymousWithOuterAccess() {
		assertThat(m_index.getOuterClass(AnonymousWithOuterAccessInner), is(AnonymousWithOuterAccessOuter));
		assertThat(m_index.getInnerClasses(AnonymousWithOuterAccessOuter), containsInAnyOrder(AnonymousWithOuterAccessInner));
		assertThat(m_index.isAnonymousClass(AnonymousWithOuterAccessInner), is(true));
		decompile(AnonymousWithOuterAccessOuter);
	}
	
	@Test
	public void classTree() {
		
		// root level
		assertThat(m_index.containsObfClass(ClassTreeRoot), is(true));
		assertThat(m_index.getOuterClass(ClassTreeRoot), is(nullValue()));
		assertThat(m_index.getInnerClasses(ClassTreeRoot), containsInAnyOrder(ClassTreeLevel1));
		
		// level 1
		ClassEntry fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getInnermostClassName()
		);
		assertThat(m_index.containsObfClass(fullClassEntry), is(true));
		assertThat(m_index.getOuterClass(ClassTreeLevel1), is(ClassTreeRoot));
		assertThat(m_index.getInnerClasses(ClassTreeLevel1), containsInAnyOrder(ClassTreeLevel2));
		
		// level 2
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getInnermostClassName()
			+ "$" + ClassTreeLevel2.getInnermostClassName()
		);
		assertThat(m_index.containsObfClass(fullClassEntry), is(true));
		assertThat(m_index.getOuterClass(ClassTreeLevel2), is(ClassTreeLevel1));
		assertThat(m_index.getInnerClasses(ClassTreeLevel2), containsInAnyOrder(ClassTreeLevel3));
		
		// level 3
		fullClassEntry = new ClassEntry(ClassTreeRoot.getName()
			+ "$" + ClassTreeLevel1.getInnermostClassName()
			+ "$" + ClassTreeLevel2.getInnermostClassName()
			+ "$" + ClassTreeLevel3.getInnermostClassName()
		);
		assertThat(m_index.containsObfClass(fullClassEntry), is(true));
		assertThat(m_index.getOuterClass(ClassTreeLevel3), is(ClassTreeLevel2));
		assertThat(m_index.getInnerClasses(ClassTreeLevel3), is(empty()));
	}
	
	private void decompile(ClassEntry classEntry) {
		m_deobfuscator.getSourceTree(classEntry.getName());
	}
}
