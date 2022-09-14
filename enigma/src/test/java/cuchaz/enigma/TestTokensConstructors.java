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

import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByMethod;
import static cuchaz.enigma.TestEntryFactory.newMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class TestTokensConstructors extends TokenChecker {
	public TestTokensConstructors() throws Exception {
		super(Paths.get("build/test-obf/constructors.jar"));
	}

	@Test
	public void baseDeclarations() {
		assertThat(getDeclarationToken(newMethod("a", "<init>", "()V")), is("a"));
		assertThat(getDeclarationToken(newMethod("a", "<init>", "(I)V")), is("a"));
	}

	@Test
	public void subDeclarations() {
		assertThat(getDeclarationToken(newMethod("d", "<init>", "()V")), is("d"));
		assertThat(getDeclarationToken(newMethod("d", "<init>", "(I)V")), is("d"));
		assertThat(getDeclarationToken(newMethod("d", "<init>", "(II)V")), is("d"));
		assertThat(getDeclarationToken(newMethod("d", "<init>", "(III)V")), is("d"));
	}

	@Test
	public void subsubDeclarations() {
		assertThat(getDeclarationToken(newMethod("e", "<init>", "(I)V")), is("e"));
	}

	@Test
	public void defaultDeclarations() {
		assertThat(getDeclarationToken(newMethod("c", "<init>", "()V")), nullValue());
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void baseDefaultReferences() {
		MethodEntry source = newMethod("a", "<init>", "()V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "a", "()V")), containsInAnyOrder("a"));
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "()V")), is(empty()) // implicit call, not decompiled to token
		);
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(III)V")), is(empty()) // implicit call, not decompiled to token
		);
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void baseIntReferences() {
		MethodEntry source = newMethod("a", "<init>", "(I)V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "b", "()V")), containsInAnyOrder("a"));
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void subDefaultReferences() {
		MethodEntry source = newMethod("d", "<init>", "()V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "c", "()V")), containsInAnyOrder("d"));
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(I)V")), containsInAnyOrder("this"));
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void subIntReferences() {
		MethodEntry source = newMethod("d", "<init>", "(I)V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "d", "()V")), containsInAnyOrder("d"));
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(II)V")), containsInAnyOrder("this"));
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "e", "<init>", "(I)V")), containsInAnyOrder("super"));
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void subIntIntReferences() {
		MethodEntry source = newMethod("d", "<init>", "(II)V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "e", "()V")), containsInAnyOrder("d"));
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void subsubIntReferences() {
		MethodEntry source = newMethod("e", "<init>", "(I)V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "f", "()V")), containsInAnyOrder("e"));
	}

	@Test
	@Ignore // TODO needs fixing, broke when compiling against J16
	public void defaultConstructableReferences() {
		MethodEntry source = newMethod("c", "<init>", "()V");
		assertThat(getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "g", "()V")), containsInAnyOrder("c"));
	}
}
