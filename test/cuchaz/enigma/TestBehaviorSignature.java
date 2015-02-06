package cuchaz.enigma;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import cuchaz.enigma.mapping.BehaviorSignature;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Type;


public class TestBehaviorSignature {
	
	@Test
	public void easiest() {
		final BehaviorSignature sig = new BehaviorSignature("()V");
		assertThat(sig.getArgumentTypes(), is(empty()));
		assertThat(sig.getReturnType(), is(new Type("V")));
	}
	
	@Test
	public void primitives() {
		{
			final BehaviorSignature sig = new BehaviorSignature("(I)V");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I")
			));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("(I)I");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I")
			));
			assertThat(sig.getReturnType(), is(new Type("I")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("(IBCJ)Z");
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
			final BehaviorSignature sig = new BehaviorSignature("([LFoo;)V");
			assertThat(sig.getArgumentTypes().size(), is(1));
			assertThat(sig.getArgumentTypes().get(0), is(new Type("[LFoo;")));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("(LFoo;)LBar;");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LFoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("LBar;")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("(LFoo;LMoo;LZoo;)LBar;");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LFoo;"),
				new Type("LMoo;"),
				new Type("LZoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("LBar;")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("(LFoo<LParm;>;LMoo<LParm;>;)LBar<LParm;>;");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("LFoo<LParm;>;"),
				new Type("LMoo<LParm;>;")
			));
			assertThat(sig.getReturnType(), is(new Type("LBar<LParm;>;")));
		}
	}
	
	@Test
	public void arrays() {
		{
			final BehaviorSignature sig = new BehaviorSignature("([I)V");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("[I")
			));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("([I)[J");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("[I")
			));
			assertThat(sig.getReturnType(), is(new Type("[J")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("([I[Z[F)[D");
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
			final BehaviorSignature sig = new BehaviorSignature("(I[JLFoo;)Z");
			assertThat(sig.getArgumentTypes(), contains(
				new Type("I"),
				new Type("[J"),
				new Type("LFoo;")
			));
			assertThat(sig.getReturnType(), is(new Type("Z")));
		}
		{
			final BehaviorSignature sig = new BehaviorSignature("(III)[LFoo;");
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
			final BehaviorSignature oldSig = new BehaviorSignature("()V");
			final BehaviorSignature sig = new BehaviorSignature(oldSig, new BehaviorSignature.ClassReplacer() {
				@Override
				public ClassEntry replace(ClassEntry entry) {
					return null;
				}
			});
			assertThat(sig.getArgumentTypes(), is(empty()));
			assertThat(sig.getReturnType(), is(new Type("V")));
		}
		{
			final BehaviorSignature oldSig = new BehaviorSignature("(IJLFoo;)V");
			final BehaviorSignature sig = new BehaviorSignature(oldSig, new BehaviorSignature.ClassReplacer() {
				@Override
				public ClassEntry replace(ClassEntry entry) {
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
			final BehaviorSignature oldSig = new BehaviorSignature("(LFoo;LBar;)LMoo;");
			final BehaviorSignature sig = new BehaviorSignature(oldSig, new BehaviorSignature.ClassReplacer() {
				@Override
				public ClassEntry replace(ClassEntry entry) {
					if (entry.getName().equals("Foo")) {
						return new ClassEntry("Bar");
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
			final BehaviorSignature oldSig = new BehaviorSignature("(LFoo;LBar;)LMoo;");
			final BehaviorSignature sig = new BehaviorSignature(oldSig, new BehaviorSignature.ClassReplacer() {
				@Override
				public ClassEntry replace(ClassEntry entry) {
					if (entry.getName().equals("Moo")) {
						return new ClassEntry("Cow");
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
}
