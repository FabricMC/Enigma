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

import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestMethodDescriptor {

	@Test
	public void easiest() {
		final MethodDescriptor sig = new MethodDescriptor("()V");
		assertThat(sig.getArgumentDescs(), is(empty()));
		assertThat(sig.getReturnDesc(), is(new TypeDescriptor("V")));
	}

	@Test
	public void primitives() {
		{
			final MethodDescriptor sig = new MethodDescriptor("(I)V");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("I")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("V")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("(I)I");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("I")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("I")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("(IBCJ)Z");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("I"),
					new TypeDescriptor("B"),
					new TypeDescriptor("C"),
					new TypeDescriptor("J")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("Z")));
		}
	}

	@Test
	public void classes() {
		{
			final MethodDescriptor sig = new MethodDescriptor("([LFoo;)V");
			assertThat(sig.getArgumentDescs().size(), is(1));
			assertThat(sig.getArgumentDescs().get(0), is(new TypeDescriptor("[LFoo;")));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("V")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("(LFoo;)LBar;");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("LFoo;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("LBar;")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("(LFoo;LMoo;LZoo;)LBar;");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("LFoo;"),
					new TypeDescriptor("LMoo;"),
					new TypeDescriptor("LZoo;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("LBar;")));
		}
	}

	@Test
	public void arrays() {
		{
			final MethodDescriptor sig = new MethodDescriptor("([I)V");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("[I")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("V")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("([I)[J");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("[I")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("[J")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("([I[Z[F)[D");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("[I"),
					new TypeDescriptor("[Z"),
					new TypeDescriptor("[F")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("[D")));
		}
	}

	@Test
	public void mixed() {
		{
			final MethodDescriptor sig = new MethodDescriptor("(I[JLFoo;)Z");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("I"),
					new TypeDescriptor("[J"),
					new TypeDescriptor("LFoo;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("Z")));
		}
		{
			final MethodDescriptor sig = new MethodDescriptor("(III)[LFoo;");
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("I"),
					new TypeDescriptor("I"),
					new TypeDescriptor("I")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("[LFoo;")));
		}
	}

	@Test
	public void replaceClasses() {
		{
			final MethodDescriptor oldSig = new MethodDescriptor("()V");
			final MethodDescriptor sig = oldSig.remap(s -> null);
			assertThat(sig.getArgumentDescs(), is(empty()));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("V")));
		}
		{
			final MethodDescriptor oldSig = new MethodDescriptor("(IJLFoo;)V");
			final MethodDescriptor sig = oldSig.remap(s -> null);
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("I"),
					new TypeDescriptor("J"),
					new TypeDescriptor("LFoo;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("V")));
		}
		{
			final MethodDescriptor oldSig = new MethodDescriptor("(LFoo;LBar;)LMoo;");
			final MethodDescriptor sig = oldSig.remap(s -> {
				if (s.equals("Foo")) {
					return "Bar";
				}
				return null;
			});
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("LBar;"),
					new TypeDescriptor("LBar;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("LMoo;")));
		}
		{
			final MethodDescriptor oldSig = new MethodDescriptor("(LFoo;LBar;)LMoo;");
			final MethodDescriptor sig = oldSig.remap(s -> {
				if (s.equals("Moo")) {
					return "Cow";
				}
				return null;
			});
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("LFoo;"),
					new TypeDescriptor("LBar;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("LCow;")));
		}
	}

	@Test
	public void replaceArrayClasses() {
		{
			final MethodDescriptor oldSig = new MethodDescriptor("([LFoo;)[[[LBar;");
			final MethodDescriptor sig = oldSig.remap(s -> {
				if (s.equals("Foo")) {
					return "Food";
				} else if (s.equals("Bar")) {
					return "Beer";
				}
				return null;
			});
			assertThat(sig.getArgumentDescs(), contains(
					new TypeDescriptor("[LFood;")
			));
			assertThat(sig.getReturnDesc(), is(new TypeDescriptor("[[[LBeer;")));
		}
	}

	@Test
	public void equals() {

		// base
		assertThat(new MethodDescriptor("()V"), is(new MethodDescriptor("()V")));

		// arguments
		assertThat(new MethodDescriptor("(I)V"), is(new MethodDescriptor("(I)V")));
		assertThat(new MethodDescriptor("(ZIZ)V"), is(new MethodDescriptor("(ZIZ)V")));
		assertThat(new MethodDescriptor("(LFoo;)V"), is(new MethodDescriptor("(LFoo;)V")));
		assertThat(new MethodDescriptor("(LFoo;LBar;)V"), is(new MethodDescriptor("(LFoo;LBar;)V")));
		assertThat(new MethodDescriptor("([I)V"), is(new MethodDescriptor("([I)V")));
		assertThat(new MethodDescriptor("([[D[[[J)V"), is(new MethodDescriptor("([[D[[[J)V")));

		assertThat(new MethodDescriptor("()V"), is(not(new MethodDescriptor("(I)V"))));
		assertThat(new MethodDescriptor("(I)V"), is(not(new MethodDescriptor("()V"))));
		assertThat(new MethodDescriptor("(IJ)V"), is(not(new MethodDescriptor("(JI)V"))));
		assertThat(new MethodDescriptor("([[Z)V"), is(not(new MethodDescriptor("([[LFoo;)V"))));
		assertThat(new MethodDescriptor("(LFoo;LBar;)V"), is(not(new MethodDescriptor("(LFoo;LCow;)V"))));
		assertThat(new MethodDescriptor("([LFoo;LBar;)V"), is(not(new MethodDescriptor("(LFoo;LCow;)V"))));

		// return desc
		assertThat(new MethodDescriptor("()I"), is(new MethodDescriptor("()I")));
		assertThat(new MethodDescriptor("()Z"), is(new MethodDescriptor("()Z")));
		assertThat(new MethodDescriptor("()[D"), is(new MethodDescriptor("()[D")));
		assertThat(new MethodDescriptor("()[[[Z"), is(new MethodDescriptor("()[[[Z")));
		assertThat(new MethodDescriptor("()LFoo;"), is(new MethodDescriptor("()LFoo;")));
		assertThat(new MethodDescriptor("()[LFoo;"), is(new MethodDescriptor("()[LFoo;")));

		assertThat(new MethodDescriptor("()I"), is(not(new MethodDescriptor("()Z"))));
		assertThat(new MethodDescriptor("()Z"), is(not(new MethodDescriptor("()I"))));
		assertThat(new MethodDescriptor("()[D"), is(not(new MethodDescriptor("()[J"))));
		assertThat(new MethodDescriptor("()[[[Z"), is(not(new MethodDescriptor("()[[Z"))));
		assertThat(new MethodDescriptor("()LFoo;"), is(not(new MethodDescriptor("()LBar;"))));
		assertThat(new MethodDescriptor("()[LFoo;"), is(not(new MethodDescriptor("()[LBar;"))));
	}

	@Test
	public void testToString() {
		assertThat(new MethodDescriptor("()V").toString(), is("()V"));
		assertThat(new MethodDescriptor("(I)V").toString(), is("(I)V"));
		assertThat(new MethodDescriptor("(ZIZ)V").toString(), is("(ZIZ)V"));
		assertThat(new MethodDescriptor("(LFoo;)V").toString(), is("(LFoo;)V"));
		assertThat(new MethodDescriptor("(LFoo;LBar;)V").toString(), is("(LFoo;LBar;)V"));
		assertThat(new MethodDescriptor("([I)V").toString(), is("([I)V"));
		assertThat(new MethodDescriptor("([[D[[[J)V").toString(), is("([[D[[[J)V"));
	}
}
