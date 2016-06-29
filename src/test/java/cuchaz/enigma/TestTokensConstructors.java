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

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.jar.JarFile;

import org.junit.Test;

import cuchaz.enigma.mapping.BehaviorEntry;

public class TestTokensConstructors extends TokenChecker {
	
	public TestTokensConstructors()
	throws Exception {
		super(new JarFile("build/test-obf/constructors.jar"));
	}
	
	@Test
	public void baseDeclarations() {
		assertThat(getDeclarationToken(newConstructor("none/a", "()V")), is("a"));
		assertThat(getDeclarationToken(newConstructor("none/a", "(I)V")), is("a"));
	}
	
	@Test
	public void subDeclarations() {
		assertThat(getDeclarationToken(newConstructor("none/d", "()V")), is("d"));
		assertThat(getDeclarationToken(newConstructor("none/d", "(I)V")), is("d"));
		assertThat(getDeclarationToken(newConstructor("none/d", "(II)V")), is("d"));
		assertThat(getDeclarationToken(newConstructor("none/d", "(III)V")), is("d"));
	}
	
	@Test
	public void subsubDeclarations() {
		assertThat(getDeclarationToken(newConstructor("none/e", "(I)V")), is("e"));
	}
	
	@Test
	public void defaultDeclarations() {
		assertThat(getDeclarationToken(newConstructor("none/c", "()V")), nullValue());
	}
	
	@Test
	public void baseDefaultReferences() {
		BehaviorEntry source = newConstructor("none/a", "()V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "none/b", "a", "()V")),
			containsInAnyOrder("a")
		);
		assertThat(
			getReferenceTokens(newBehaviorReferenceByConstructor(source, "none/d", "()V")),
			is(empty()) // implicit call, not decompiled to token
		);
		assertThat(
			getReferenceTokens(newBehaviorReferenceByConstructor(source, "none/d", "(III)V")),
			is(empty()) // implicit call, not decompiled to token
		);
	}
	
	@Test
	public void baseIntReferences() {
		BehaviorEntry source = newConstructor("none/a", "(I)V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "none/b", "b", "()V")),
			containsInAnyOrder("a")
		);
	}
	
	@Test
	public void subDefaultReferences() {
		BehaviorEntry source = newConstructor("none/d", "()V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "none/b", "c", "()V")),
			containsInAnyOrder("d")
		);
		assertThat(
			getReferenceTokens(newBehaviorReferenceByConstructor(source, "none/d", "(I)V")),
			containsInAnyOrder("this")
		);
	}
	
	@Test
	public void subIntReferences() {
		BehaviorEntry source = newConstructor("none/d", "(I)V");
		assertThat(getReferenceTokens(
			newBehaviorReferenceByMethod(source, "none/b", "d", "()V")),
			containsInAnyOrder("d")
		);
		assertThat(getReferenceTokens(
			newBehaviorReferenceByConstructor(source, "none/d", "(II)V")),
			containsInAnyOrder("this")
		);
		assertThat(getReferenceTokens(
			newBehaviorReferenceByConstructor(source, "none/e", "(I)V")),
			containsInAnyOrder("super")
		);
	}
	
	@Test
	public void subIntIntReferences() {
		BehaviorEntry source = newConstructor("none/d", "(II)V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "none/b", "e", "()V")),
			containsInAnyOrder("d")
		);
	}
	
	@Test
	public void subsubIntReferences() {
		BehaviorEntry source = newConstructor("none/e", "(I)V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "none/b", "f", "()V")),
			containsInAnyOrder("e")
		);
	}
	
	@Test
	public void defaultConstructableReferences() {
		BehaviorEntry source = newConstructor("none/c", "()V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "none/b", "g", "()V")),
			containsInAnyOrder("c")
		);
	}
}
