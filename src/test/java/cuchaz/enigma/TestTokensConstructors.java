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
		assertThat(getDeclarationToken(newConstructor("a", "()V")), is("a"));
		assertThat(getDeclarationToken(newConstructor("a", "(I)V")), is("a"));
	}
	
	@Test
	public void subDeclarations() {
		assertThat(getDeclarationToken(newConstructor("d", "()V")), is("d"));
		assertThat(getDeclarationToken(newConstructor("d", "(I)V")), is("d"));
		assertThat(getDeclarationToken(newConstructor("d", "(II)V")), is("d"));
		assertThat(getDeclarationToken(newConstructor("d", "(III)V")), is("d"));
	}
	
	@Test
	public void subsubDeclarations() {
		assertThat(getDeclarationToken(newConstructor("e", "(I)V")), is("e"));
	}
	
	@Test
	public void defaultDeclarations() {
		assertThat(getDeclarationToken(newConstructor("c", "()V")), nullValue());
	}
	
	@Test
	public void baseDefaultReferences() {
		BehaviorEntry source = newConstructor("a", "()V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "a", "()V")),
			containsInAnyOrder("a")
		);
		assertThat(
			getReferenceTokens(newBehaviorReferenceByConstructor(source, "d", "()V")),
			is(empty()) // implicit call, not decompiled to token
		);
		assertThat(
			getReferenceTokens(newBehaviorReferenceByConstructor(source, "d", "(III)V")),
			is(empty()) // implicit call, not decompiled to token
		);
	}
	
	@Test
	public void baseIntReferences() {
		BehaviorEntry source = newConstructor("a", "(I)V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "b", "()V")),
			containsInAnyOrder("a")
		);
	}
	
	@Test
	public void subDefaultReferences() {
		BehaviorEntry source = newConstructor("d", "()V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "c", "()V")),
			containsInAnyOrder("d")
		);
		assertThat(
			getReferenceTokens(newBehaviorReferenceByConstructor(source, "d", "(I)V")),
			containsInAnyOrder("this")
		);
	}
	
	@Test
	public void subIntReferences() {
		BehaviorEntry source = newConstructor("d", "(I)V");
		assertThat(getReferenceTokens(
			newBehaviorReferenceByMethod(source, "b", "d", "()V")),
			containsInAnyOrder("d")
		);
		assertThat(getReferenceTokens(
			newBehaviorReferenceByConstructor(source, "d", "(II)V")),
			containsInAnyOrder("this")
		);
		assertThat(getReferenceTokens(
			newBehaviorReferenceByConstructor(source, "e", "(I)V")),
			containsInAnyOrder("super")
		);
	}
	
	@Test
	public void subIntIntReferences() {
		BehaviorEntry source = newConstructor("d", "(II)V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "e", "()V")),
			containsInAnyOrder("d")
		);
	}
	
	@Test
	public void subsubIntReferences() {
		BehaviorEntry source = newConstructor("e", "(I)V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "f", "()V")),
			containsInAnyOrder("e")
		);
	}
	
	@Test
	public void defaultConstructableReferences() {
		BehaviorEntry source = newConstructor("c", "()V");
		assertThat(
			getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "g", "()V")),
			containsInAnyOrder("c")
		);
	}
}
