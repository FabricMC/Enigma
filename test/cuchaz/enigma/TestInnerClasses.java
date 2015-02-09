/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.analysis.JarIndex;

public class TestInnerClasses {
	
	private JarIndex m_index;
	private Deobfuscator m_deobfuscator;
	
	private static final String AnonymousOuter = "none/a";
	private static final String AnonymousInner = "b";
	private static final String SimpleOuter = "none/g";
	private static final String SimpleInner = "h";
	private static final String ConstructorArgsOuter = "none/e";
	private static final String ConstructorArgsInner = "f";
	private static final String AnonymousWithScopeArgsOuter = "none/c";
	private static final String AnonymousWithScopeArgsInner = "d";
	private static final String AnonymousWithOuterAccessOuter = "none/i";
	private static final String AnonymousWithOuterAccessInner = "j";
	
	public TestInnerClasses()
	throws Exception {
		m_index = new JarIndex();
		JarFile jar = new JarFile("build/testInnerClasses.obf.jar");
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
	
	private void decompile(String name) {
		m_deobfuscator.getSourceTree(name);
	}
}
