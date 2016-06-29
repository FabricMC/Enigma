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

import org.junit.Test;

import cuchaz.enigma.mapping.ClassNameReplacer;
import cuchaz.enigma.mapping.Signature;
import cuchaz.enigma.mapping.Type;


public class TestSignature {
	
	@Test
	public void easiest() {
		final Signature sig = new Signature("()V");
		assertThat(sig.getArgumentTypes(), is(empty()));
		assertThat(sig.getReturnType(), is(new Type("V")));
	}
	
	@Test
	public void primitives() {
		{
			final Signature sig = new Signature("(I)V");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I")
			));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final Signature sig = new Signature("(I)I");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I")
			));
			assertThat(sig.getReturnType(), is(new Type("I")));
		}
		{
			final Signature sig = new Signature("(IBCJ)Z");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I"),
				new Type("B"),
				new Type("C"),
				new Type("J")
			));
			assertThat(sig.getReturnType(), is(new Type("Z")));
		}
	}
	
	@Test
	public void classes() {
		{
			final Signature sig = new Signature("([LFoo;)V");
			assertThat(sig.getArgumentTypes().size(), is(1));
			assertThat(sig.getArgumentTypes().get(0), is(new Type("[LFoo;")));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final Signature sig = new Signature("(LFoo;)LBar;");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LFoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("LBar;")));
		}
		{
			final Signature sig = new Signature("(LFoo;LMoo;LZoo;)LBar;");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LFoo;"),
				new Type("LMoo;"),
				new Type("LZoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("LBar;")));
		}
	}
	
	@Test
	public void arrays() {
		{
			final Signature sig = new Signature("([I)V");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("[I")
			));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final Signature sig = new Signature("([I)[J");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("[I")
			));
			assertThat(sig.getReturnType(), is(new Type("[J")));
		}
		{
			final Signature sig = new Signature("([I[Z[F)[D");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("[I"),
				new Type("[Z"),
				new Type("[F")
			));
			assertThat(sig.getReturnType(), is(new Type("[D")));
		}
	}
	
	@Test
	public void mixed() {
		{
			final Signature sig = new Signature("(I[JLFoo;)Z");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I"),
				new Type("[J"),
				new Type("LFoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("Z")));
		}
		{
			final Signature sig = new Signature("(III)[LFoo;");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I"),
				new Type("I"),
				new Type("I")
			));
			assertThat(sig.getReturnType(), is(new Type("[LFoo;")));
		}
	}
	
	@Test
	public void replaceClasses() {
		{
			final Signature oldSig = new Signature("()V");
			final Signature sig = new Signature(oldSig, new ClassNameReplacer() {
				@Override
				public String replace(String val) {
					return null;
				}
			});
			assertThat(sig.getArgumentTypes(), is(empty()));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final Signature oldSig = new Signature("(IJLFoo;)V");
			final Signature sig = new Signature(oldSig, new ClassNameReplacer() {
				@Override
				public String replace(String val) {
					return null;
				}
			});
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I"),
				new Type("J"),
				new Type("LFoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final Signature oldSig = new Signature("(LFoo;LBar;)LMoo;");
			final Signature sig = new Signature(oldSig, new ClassNameReplacer() {
				@Override
				public String replace(String val) {
					if (val.equals("Foo")) {
						return "Bar";
					}
					return null;
				}
			});
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LBar;"),
				new Type("LBar;")
			));
			assertThat(sig.getReturnType(), is(new Type("LMoo;")));
		}
		{
			final Signature oldSig = new Signature("(LFoo;LBar;)LMoo;");
			final Signature sig = new Signature(oldSig, new ClassNameReplacer() {
				@Override
				public String replace(String val) {
					if (val.equals("Moo")) {
						return "Cow";
					}
					return null;
				}
			});
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LFoo;"),
				new Type("LBar;")
			));
			assertThat(sig.getReturnType(), is(new Type("LCow;")));
		}
	}
	
	@Test
	public void replaceArrayClasses() {
		{
			final Signature oldSig = new Signature("([LFoo;)[[[LBar;");
			final Signature sig = new Signature(oldSig, new ClassNameReplacer() {
				@Override
				public String replace(String val) {
					if (val.equals("Foo")) {
						return "Food";
					} else if (val.equals("Bar")) {
						return "Beer";
					}
					return null;
				}
			});
			assertThat(sig.getArgumentTypes(), contains(
				new Type("[LFood;")
			));
			assertThat(sig.getReturnType(), is(new Type("[[[LBeer;")));
		}
	}
	
	@Test
	public void equals() {
		
		// base
		assertThat(new Signature("()V"), is(new Signature("()V")));
		
		// arguments
		assertThat(new Signature("(I)V"), is(new Signature("(I)V")));
		assertThat(new Signature("(ZIZ)V"), is(new Signature("(ZIZ)V")));
		assertThat(new Signature("(LFoo;)V"), is(new Signature("(LFoo;)V")));
		assertThat(new Signature("(LFoo;LBar;)V"), is(new Signature("(LFoo;LBar;)V")));
		assertThat(new Signature("([I)V"), is(new Signature("([I)V")));
		assertThat(new Signature("([[D[[[J)V"), is(new Signature("([[D[[[J)V")));

		assertThat(new Signature("()V"), is(not(new Signature("(I)V"))));
		assertThat(new Signature("(I)V"), is(not(new Signature("()V"))));
		assertThat(new Signature("(IJ)V"), is(not(new Signature("(JI)V"))));
		assertThat(new Signature("([[Z)V"), is(not(new Signature("([[LFoo;)V"))));
		assertThat(new Signature("(LFoo;LBar;)V"), is(not(new Signature("(LFoo;LCow;)V"))));
		assertThat(new Signature("([LFoo;LBar;)V"), is(not(new Signature("(LFoo;LCow;)V"))));
		
		// return type
		assertThat(new Signature("()I"), is(new Signature("()I")));
		assertThat(new Signature("()Z"), is(new Signature("()Z")));
		assertThat(new Signature("()[D"), is(new Signature("()[D")));
		assertThat(new Signature("()[[[Z"), is(new Signature("()[[[Z")));
		assertThat(new Signature("()LFoo;"), is(new Signature("()LFoo;")));
		assertThat(new Signature("()[LFoo;"), is(new Signature("()[LFoo;")));
		
		assertThat(new Signature("()I"), is(not(new Signature("()Z"))));
		assertThat(new Signature("()Z"), is(not(new Signature("()I"))));
		assertThat(new Signature("()[D"), is(not(new Signature("()[J"))));
		assertThat(new Signature("()[[[Z"), is(not(new Signature("()[[Z"))));
		assertThat(new Signature("()LFoo;"), is(not(new Signature("()LBar;"))));
		assertThat(new Signature("()[LFoo;"), is(not(new Signature("()[LBar;"))));
	}
	
	@Test
	public void testToString() {
		assertThat(new Signature("()V").toString(), is("()V"));
		assertThat(new Signature("(I)V").toString(), is("(I)V"));
		assertThat(new Signature("(ZIZ)V").toString(), is("(ZIZ)V"));
		assertThat(new Signature("(LFoo;)V").toString(), is("(LFoo;)V"));
		assertThat(new Signature("(LFoo;LBar;)V").toString(), is("(LFoo;LBar;)V"));
		assertThat(new Signature("([I)V").toString(), is("([I)V"));
		assertThat(new Signature("([[D[[[J)V").toString(), is("([[D[[[J)V"));
	}
}
