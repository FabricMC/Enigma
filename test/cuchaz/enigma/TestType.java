package cuchaz.enigma;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import cuchaz.enigma.mapping.ParameterizedType;
import cuchaz.enigma.mapping.Type;


public class TestType {
	
	@Test
	public void isVoid() {
		assertThat(new Type("V").isVoid(), is(true));
		assertThat(new Type("Z").isVoid(), is(false));
		assertThat(new Type("B").isVoid(), is(false));
		assertThat(new Type("C").isVoid(), is(false));
		assertThat(new Type("I").isVoid(), is(false));
		assertThat(new Type("J").isVoid(), is(false));
		assertThat(new Type("F").isVoid(), is(false));
		assertThat(new Type("D").isVoid(), is(false));
		assertThat(new Type("LFoo;").isVoid(), is(false));
		assertThat(new Type("[I").isVoid(), is(false));
		assertThat(new Type("TFoo;").isVoid(), is(false));
	}
	
	@Test
	public void isPrimitive() {
		assertThat(new Type("V").isPrimitive(), is(false));
		assertThat(new Type("Z").isPrimitive(), is(true));
		assertThat(new Type("B").isPrimitive(), is(true));
		assertThat(new Type("C").isPrimitive(), is(true));
		assertThat(new Type("I").isPrimitive(), is(true));
		assertThat(new Type("J").isPrimitive(), is(true));
		assertThat(new Type("F").isPrimitive(), is(true));
		assertThat(new Type("D").isPrimitive(), is(true));
		assertThat(new Type("LFoo;").isPrimitive(), is(false));
		assertThat(new Type("[I").isPrimitive(), is(false));
		assertThat(new Type("TFoo;").isPrimitive(), is(false));
	}
	
	@Test
	public void getPrimitive() {
		assertThat(new Type("Z").getPrimitive(), is(Type.Primitive.Boolean));
		assertThat(new Type("B").getPrimitive(), is(Type.Primitive.Byte));
		assertThat(new Type("C").getPrimitive(), is(Type.Primitive.Character));
		assertThat(new Type("I").getPrimitive(), is(Type.Primitive.Integer));
		assertThat(new Type("J").getPrimitive(), is(Type.Primitive.Long));
		assertThat(new Type("F").getPrimitive(), is(Type.Primitive.Float));
		assertThat(new Type("D").getPrimitive(), is(Type.Primitive.Double));
	}
	
	@Test
	public void isClass() {
		assertThat(new Type("V").isClass(), is(false));
		assertThat(new Type("Z").isClass(), is(false));
		assertThat(new Type("B").isClass(), is(false));
		assertThat(new Type("C").isClass(), is(false));
		assertThat(new Type("I").isClass(), is(false));
		assertThat(new Type("J").isClass(), is(false));
		assertThat(new Type("F").isClass(), is(false));
		assertThat(new Type("D").isClass(), is(false));
		assertThat(new Type("LFoo;").isClass(), is(true));
		assertThat(new Type("LFoo<LCow;>;").isClass(), is(true));
		assertThat(new Type("LFoo<LCow;LBeer;>;").isClass(), is(true));
		assertThat(new Type("LFoo<LPair<LCow;LBeer;>;>;").isClass(), is(true));
		assertThat(new Type("[I").isClass(), is(false));
		assertThat(new Type("TFoo;").isClass(), is(false));
	}
	
	@Test
	public void getClassEntry() {
		assertThat(new Type("LFoo;").getClassEntry(), is(newClass("Foo")));
		assertThat(new Type("LFoo<Ljava/lang/String;>;").getClassEntry(), is(newClass("Foo")));
		assertThat(new Type("LFoo<LCow;>;").getClassEntry(), is(newClass("Foo")));
		assertThat(new Type("LFoo<LCow;LBeer;>;").getClassEntry(), is(newClass("Foo")));
		assertThat(new Type("LFoo<LPair<LCow;LBeer;>;>;").getClassEntry(), is(newClass("Foo")));
	}
	
	@Test
	public void getArrayClassEntry() {
		assertThat(new Type("[LFoo;").getClassEntry(), is(newClass("Foo")));
		assertThat(new Type("[[[LFoo<Ljava/lang/String;>;").getClassEntry(), is(newClass("Foo")));
	}
	
	@Test
	public void isArray() {
		assertThat(new Type("V").isArray(), is(false));
		assertThat(new Type("Z").isArray(), is(false));
		assertThat(new Type("B").isArray(), is(false));
		assertThat(new Type("C").isArray(), is(false));
		assertThat(new Type("I").isArray(), is(false));
		assertThat(new Type("J").isArray(), is(false));
		assertThat(new Type("F").isArray(), is(false));
		assertThat(new Type("D").isArray(), is(false));
		assertThat(new Type("LFoo;").isArray(), is(false));
		assertThat(new Type("[I").isArray(), is(true));
		assertThat(new Type("TFoo;").isArray(), is(false));
	}
	
	@Test
	public void getArrayDimension() {
		assertThat(new Type("[I").getArrayDimension(), is(1));
		assertThat(new Type("[[I").getArrayDimension(), is(2));
		assertThat(new Type("[[[I").getArrayDimension(), is(3));
	}
	
	@Test
	public void getArrayType() {
		assertThat(new Type("[I").getArrayType(), is(new Type("I")));
		assertThat(new Type("[[I").getArrayType(), is(new Type("I")));
		assertThat(new Type("[[[I").getArrayType(), is(new Type("I")));
		assertThat(new Type("[Ljava/lang/String;").getArrayType(), is(new Type("Ljava/lang/String;")));
	}
	
	@Test
	public void isTemplate() {
		assertThat(new Type("V").isTemplate(), is(false));
		assertThat(new Type("Z").isTemplate(), is(false));
		assertThat(new Type("B").isTemplate(), is(false));
		assertThat(new Type("C").isTemplate(), is(false));
		assertThat(new Type("I").isTemplate(), is(false));
		assertThat(new Type("J").isTemplate(), is(false));
		assertThat(new Type("F").isTemplate(), is(false));
		assertThat(new Type("D").isTemplate(), is(false));
		assertThat(new Type("LFoo;").isTemplate(), is(false));
		assertThat(new Type("LFoo<LCow;>;").isTemplate(), is(false));
		assertThat(new Type("LFoo<LCow;LBeer;>;").isTemplate(), is(false));
		assertThat(new Type("LFoo<LPair<LCow;LBeer;>;>;").isTemplate(), is(false));
		assertThat(new Type("[I").isTemplate(), is(false));
		assertThat(new Type("TFoo;").isTemplate(), is(true));
	}
	
	@Test
	public void getTemplate() {
		assertThat(new Type("TT;").getTemplate(), is("T"));
		assertThat(new Type("TFoo;").getTemplate(), is("Foo"));
	}
	
	@Test
	public void hasClass() {
		assertThat(new Type("LFoo;").hasClass(), is(true));
		assertThat(new Type("LCow<LCheese;>;").hasClass(), is(true));
		assertThat(new Type("[LBar;").hasClass(), is(true));
		assertThat(new Type("[[[LCat;").hasClass(), is(true));

		assertThat(new Type("V").hasClass(), is(false));
		assertThat(new Type("[I").hasClass(), is(false));
		assertThat(new Type("[[[I").hasClass(), is(false));
		assertThat(new Type("Z").hasClass(), is(false));
		assertThat(new Type("TFoo;").hasClass(), is(false));
	}
	
	@Test
	public void parameters() {
		assertThat(new Type("LFoo<I>;").parameters(), contains(
			new Type("I")
		));
		assertThat(new Type("LFoo<IIII>;").parameters(), contains(
			new Type("I"),
			new Type("I"),
			new Type("I"),
			new Type("I")
		));
		assertThat(new Type("LFoo<LBar;>;").parameters(), contains(
			new Type("LBar;")
		));
		assertThat(new Type("LFoo<LBar;LCow;LCheese;>;").parameters(), contains(
			new Type("LBar;"),
			new Type("LCow;"),
			new Type("LCheese;")
		));
		
		assertThat(new Type("LFoo<LBar<LCow;LCheese;>;>;").parameters(), contains(
			new Type("LBar<LCow;LCheese;>;")
		));
		
		assertThat(new Type("LFoo<LBar<LCow;LCheese;>;>;").parameters().iterator().next().parameters(), contains(
			new Type("LCow;"),
			new Type("LCheese;")
		));
	}
	
	@Test
	public void parseVoid() {
		final String answer = "V";
		assertThat(Type.parseFirst("V"), is(answer));
		assertThat(Type.parseFirst("VVV"), is(answer));
		assertThat(Type.parseFirst("VIJ"), is(answer));
		assertThat(Type.parseFirst("V[I"), is(answer));
		assertThat(Type.parseFirst("VLFoo;"), is(answer));
		assertThat(Type.parseFirst("V[LFoo;"), is(answer));
	}
	
	@Test
	public void parsePrimitive() {
		final String answer = "I";
		assertThat(Type.parseFirst("I"), is(answer));
		assertThat(Type.parseFirst("III"), is(answer));
		assertThat(Type.parseFirst("IJZ"), is(answer));
		assertThat(Type.parseFirst("I[I"), is(answer));
		assertThat(Type.parseFirst("ILFoo;"), is(answer));
		assertThat(Type.parseFirst("I[LFoo;"), is(answer));
	}
	
	@Test
	public void parseClass() {
		{
			final String answer = "LFoo;";
			assertThat(Type.parseFirst("LFoo;"), is(answer));
			assertThat(Type.parseFirst("LFoo;I"), is(answer));
			assertThat(Type.parseFirst("LFoo;JZ"), is(answer));
			assertThat(Type.parseFirst("LFoo;[I"), is(answer));
			assertThat(Type.parseFirst("LFoo;LFoo;"), is(answer));
			assertThat(Type.parseFirst("LFoo;[LFoo;"), is(answer));
		}
		{
			final String answer = "LFoo<LFoo;>;";
			assertThat(Type.parseFirst("LFoo<LFoo;>;"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;>;I"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;>;JZ"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;>;[I"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;>;LFoo;"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;>;[LFoo;"), is(answer));
		}
		{
			final String answer = "LFoo<LFoo;,LBar;>;";
			assertThat(Type.parseFirst("LFoo<LFoo;,LBar;>;"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;,LBar;>;I"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;,LBar;>;JZ"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;,LBar;>;[I"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;,LBar;>;LFoo;"), is(answer));
			assertThat(Type.parseFirst("LFoo<LFoo;,LBar;>;[LFoo;"), is(answer));
		}
	}

	@Test
	public void parseTemplate() {
		final String answer = "TFoo;";
		assertThat(Type.parseFirst("TFoo;"), is(answer));
		assertThat(Type.parseFirst("TFoo;I"), is(answer));
		assertThat(Type.parseFirst("TFoo;JZ"), is(answer));
		assertThat(Type.parseFirst("TFoo;[I"), is(answer));
		assertThat(Type.parseFirst("TFoo;LFoo;"), is(answer));
		assertThat(Type.parseFirst("TFoo;[LFoo;"), is(answer));
	}

	@Test
	public void parseArray() {
		{
			final String answer = "[I";
			assertThat(Type.parseFirst("[I"), is(answer));
			assertThat(Type.parseFirst("[III"), is(answer));
			assertThat(Type.parseFirst("[IJZ"), is(answer));
			assertThat(Type.parseFirst("[I[I"), is(answer));
			assertThat(Type.parseFirst("[ILFoo;"), is(answer));
		}
		{
			final String answer = "[[I";
			assertThat(Type.parseFirst("[[I"), is(answer));
			assertThat(Type.parseFirst("[[III"), is(answer));
			assertThat(Type.parseFirst("[[IJZ"), is(answer));
			assertThat(Type.parseFirst("[[I[I"), is(answer));
			assertThat(Type.parseFirst("[[ILFoo;"), is(answer));
		}
		{
			final String answer = "[LFoo;";
			assertThat(Type.parseFirst("[LFoo;"), is(answer));
			assertThat(Type.parseFirst("[LFoo;II"), is(answer));
			assertThat(Type.parseFirst("[LFoo;JZ"), is(answer));
			assertThat(Type.parseFirst("[LFoo;[I"), is(answer));
			assertThat(Type.parseFirst("[LFoo;LFoo;"), is(answer));
		}
	}
	
	@Test
	public void equals() {
		assertThat(new Type("V"), is(new Type("V")));
		assertThat(new Type("Z"), is(new Type("Z")));
		assertThat(new Type("B"), is(new Type("B")));
		assertThat(new Type("C"), is(new Type("C")));
		assertThat(new Type("I"), is(new Type("I")));
		assertThat(new Type("J"), is(new Type("J")));
		assertThat(new Type("F"), is(new Type("F")));
		assertThat(new Type("D"), is(new Type("D")));
		assertThat(new Type("LFoo;"), is(new Type("LFoo;")));
		assertThat(new Type("[I"), is(new Type("[I")));
		assertThat(new Type("[[[I"), is(new Type("[[[I")));
		assertThat(new Type("[LFoo;"), is(new Type("[LFoo;")));
		assertThat(new Type("LFoo<LBar;>;"), is(new Type("LFoo<LBar;>;")));
		assertThat(new Type("LFoo<LBar;>;"), is(new Type("LFoo<LCow;>;")));
		assertThat(new Type("TFoo;"), is(new Type("TFoo;")));
		
		assertThat(new Type("V"), is(not(new Type("I"))));
		assertThat(new Type("I"), is(not(new Type("J"))));
		assertThat(new Type("I"), is(not(new Type("LBar;"))));
		assertThat(new Type("I"), is(not(new Type("[I"))));
		assertThat(new Type("LFoo;"), is(not(new Type("LBar;"))));
		assertThat(new Type("[I"), is(not(new Type("[Z"))));
		assertThat(new Type("[[[I"), is(not(new Type("[I"))));
		assertThat(new Type("[LFoo;"), is(not(new Type("[LBar;"))));
		assertThat(new Type("TFoo;"), is(not(new Type("TBar;"))));
	}
	
	@Test
	public void testToString() {
		assertThat(new Type("V").toString(), is("V"));
		assertThat(new Type("Z").toString(), is("Z"));
		assertThat(new Type("B").toString(), is("B"));
		assertThat(new Type("C").toString(), is("C"));
		assertThat(new Type("I").toString(), is("I"));
		assertThat(new Type("J").toString(), is("J"));
		assertThat(new Type("F").toString(), is("F"));
		assertThat(new Type("D").toString(), is("D"));
		assertThat(new Type("LFoo;").toString(), is("LFoo;"));
		assertThat(new Type("[I").toString(), is("[I"));
		assertThat(new Type("[[[I").toString(), is("[[[I"));
		assertThat(new Type("[LFoo;").toString(), is("[LFoo;"));
		assertThat(new Type("LFoo<LBar;>;").toString(), is("LFoo;"));
		assertThat(new Type("LFoo<LCow;LCheese;>;").toString(), is("LFoo;"));
		assertThat(new Type("LFoo<LPair<LCow;LCheese;>;>;").toString(), is("LFoo;"));
		assertThat(new Type("TFoo;").toString(), is("TFoo;"));
	}
	
	private ParameterizedType ptype(String name) {
		return new ParameterizedType(new Type(name));
	}
	
	@Test
	public void equalsWithParameters() {
		assertThat(ptype("V"), is(ptype("V")));
		assertThat(ptype("Z"), is(ptype("Z")));
		assertThat(ptype("B"), is(ptype("B")));
		assertThat(ptype("C"), is(ptype("C")));
		assertThat(ptype("I"), is(ptype("I")));
		assertThat(ptype("J"), is(ptype("J")));
		assertThat(ptype("F"), is(ptype("F")));
		assertThat(ptype("D"), is(ptype("D")));
		assertThat(ptype("LFoo;"), is(ptype("LFoo;")));
		assertThat(ptype("[I"), is(ptype("[I")));
		assertThat(ptype("[[[I"), is(ptype("[[[I")));
		assertThat(ptype("[LFoo;"), is(ptype("[LFoo;")));
		assertThat(ptype("LFoo<LBar;>;"), is(ptype("LFoo<LBar;>;")));
		assertThat(ptype("TFoo;"), is(ptype("TFoo;")));
		
		assertThat(ptype("V"), is(not(ptype("I"))));
		assertThat(ptype("I"), is(not(ptype("J"))));
		assertThat(ptype("I"), is(not(ptype("LBar;"))));
		assertThat(ptype("I"), is(not(ptype("[I"))));
		assertThat(ptype("LFoo;"), is(not(ptype("LBar;"))));
		assertThat(ptype("[I"), is(not(ptype("[Z"))));
		assertThat(ptype("[[[I"), is(not(ptype("[I"))));
		assertThat(ptype("[LFoo;"), is(not(ptype("[LBar;"))));
		assertThat(ptype("LFoo<LBar;>;"), is(not(ptype("LFoo<LCow;>;"))));
		assertThat(ptype("TFoo;"), is(not(ptype("TBar;"))));
	}
	
	@Test
	public void testToStringWithParams() {
		assertThat(ptype("V").toString(), is("V"));
		assertThat(ptype("Z").toString(), is("Z"));
		assertThat(ptype("B").toString(), is("B"));
		assertThat(ptype("C").toString(), is("C"));
		assertThat(ptype("I").toString(), is("I"));
		assertThat(ptype("J").toString(), is("J"));
		assertThat(ptype("F").toString(), is("F"));
		assertThat(ptype("D").toString(), is("D"));
		assertThat(ptype("LFoo;").toString(), is("LFoo;"));
		assertThat(ptype("[I").toString(), is("[I"));
		assertThat(ptype("[[[I").toString(), is("[[[I"));
		assertThat(ptype("[LFoo;").toString(), is("[LFoo;"));
		assertThat(ptype("LFoo<LBar;>;").toString(), is("LFoo<LBar;>;"));
		assertThat(ptype("LFoo<LCow;LCheese;>;").toString(), is("LFoo<LCow;LCheese;>;"));
		assertThat(ptype("LFoo<LPair<LCow;LCheese;>;>;").toString(), is("LFoo<LPair<LCow;LCheese;>;>;"));
		assertThat(ptype("TFoo;").toString(), is("TFoo;"));
	}
}
