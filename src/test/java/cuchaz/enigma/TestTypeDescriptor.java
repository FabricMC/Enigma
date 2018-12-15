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

import cuchaz.enigma.mapping.TypeDescriptor;
import org.junit.Test;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestTypeDescriptor {

	@Test
	public void isVoid() {
		assertThat(new TypeDescriptor("V").isVoid(), is(true));
		assertThat(new TypeDescriptor("Z").isVoid(), is(false));
		assertThat(new TypeDescriptor("B").isVoid(), is(false));
		assertThat(new TypeDescriptor("C").isVoid(), is(false));
		assertThat(new TypeDescriptor("I").isVoid(), is(false));
		assertThat(new TypeDescriptor("J").isVoid(), is(false));
		assertThat(new TypeDescriptor("F").isVoid(), is(false));
		assertThat(new TypeDescriptor("D").isVoid(), is(false));
		assertThat(new TypeDescriptor("LFoo;").isVoid(), is(false));
		assertThat(new TypeDescriptor("[I").isVoid(), is(false));
	}

	@Test
	public void isPrimitive() {
		assertThat(new TypeDescriptor("V").isPrimitive(), is(false));
		assertThat(new TypeDescriptor("Z").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("B").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("C").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("I").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("J").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("F").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("D").isPrimitive(), is(true));
		assertThat(new TypeDescriptor("LFoo;").isPrimitive(), is(false));
		assertThat(new TypeDescriptor("[I").isPrimitive(), is(false));
	}

	@Test
	public void getPrimitive() {
		assertThat(new TypeDescriptor("Z").getPrimitive(), is(TypeDescriptor.Primitive.BOOLEAN));
		assertThat(new TypeDescriptor("B").getPrimitive(), is(TypeDescriptor.Primitive.BYTE));
		assertThat(new TypeDescriptor("C").getPrimitive(), is(TypeDescriptor.Primitive.CHARACTER));
		assertThat(new TypeDescriptor("I").getPrimitive(), is(TypeDescriptor.Primitive.INTEGER));
		assertThat(new TypeDescriptor("J").getPrimitive(), is(TypeDescriptor.Primitive.LONG));
		assertThat(new TypeDescriptor("F").getPrimitive(), is(TypeDescriptor.Primitive.FLOAT));
		assertThat(new TypeDescriptor("D").getPrimitive(), is(TypeDescriptor.Primitive.DOUBLE));
	}

	@Test
	public void isClass() {
		assertThat(new TypeDescriptor("V").isType(), is(false));
		assertThat(new TypeDescriptor("Z").isType(), is(false));
		assertThat(new TypeDescriptor("B").isType(), is(false));
		assertThat(new TypeDescriptor("C").isType(), is(false));
		assertThat(new TypeDescriptor("I").isType(), is(false));
		assertThat(new TypeDescriptor("J").isType(), is(false));
		assertThat(new TypeDescriptor("F").isType(), is(false));
		assertThat(new TypeDescriptor("D").isType(), is(false));
		assertThat(new TypeDescriptor("LFoo;").isType(), is(true));
		assertThat(new TypeDescriptor("[I").isType(), is(false));
	}

	@Test
	public void getClassEntry() {
		assertThat(new TypeDescriptor("LFoo;").getTypeEntry(), is(newClass("Foo")));
		assertThat(new TypeDescriptor("Ljava/lang/String;").getTypeEntry(), is(newClass("java/lang/String")));
	}

	@Test
	public void getArrayClassEntry() {
		assertThat(new TypeDescriptor("[LFoo;").getTypeEntry(), is(newClass("Foo")));
		assertThat(new TypeDescriptor("[[[Ljava/lang/String;").getTypeEntry(), is(newClass("java/lang/String")));
	}

	@Test
	public void isArray() {
		assertThat(new TypeDescriptor("V").isArray(), is(false));
		assertThat(new TypeDescriptor("Z").isArray(), is(false));
		assertThat(new TypeDescriptor("B").isArray(), is(false));
		assertThat(new TypeDescriptor("C").isArray(), is(false));
		assertThat(new TypeDescriptor("I").isArray(), is(false));
		assertThat(new TypeDescriptor("J").isArray(), is(false));
		assertThat(new TypeDescriptor("F").isArray(), is(false));
		assertThat(new TypeDescriptor("D").isArray(), is(false));
		assertThat(new TypeDescriptor("LFoo;").isArray(), is(false));
		assertThat(new TypeDescriptor("[I").isArray(), is(true));
	}

	@Test
	public void getArrayDimension() {
		assertThat(new TypeDescriptor("[I").getArrayDimension(), is(1));
		assertThat(new TypeDescriptor("[[I").getArrayDimension(), is(2));
		assertThat(new TypeDescriptor("[[[I").getArrayDimension(), is(3));
	}

	@Test
	public void getArrayType() {
		assertThat(new TypeDescriptor("[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[[[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[Ljava/lang/String;").getArrayType(), is(new TypeDescriptor("Ljava/lang/String;")));
	}

	@Test
	public void hasClass() {
		assertThat(new TypeDescriptor("LFoo;").containsType(), is(true));
		assertThat(new TypeDescriptor("Ljava/lang/String;").containsType(), is(true));
		assertThat(new TypeDescriptor("[LBar;").containsType(), is(true));
		assertThat(new TypeDescriptor("[[[LCat;").containsType(), is(true));

		assertThat(new TypeDescriptor("V").containsType(), is(false));
		assertThat(new TypeDescriptor("[I").containsType(), is(false));
		assertThat(new TypeDescriptor("[[[I").containsType(), is(false));
		assertThat(new TypeDescriptor("Z").containsType(), is(false));
	}

	@Test
	public void parseVoid() {
		final String answer = "V";
		assertThat(TypeDescriptor.parseFirst("V"), is(answer));
		assertThat(TypeDescriptor.parseFirst("VVV"), is(answer));
		assertThat(TypeDescriptor.parseFirst("VIJ"), is(answer));
		assertThat(TypeDescriptor.parseFirst("V[I"), is(answer));
		assertThat(TypeDescriptor.parseFirst("VLFoo;"), is(answer));
		assertThat(TypeDescriptor.parseFirst("V[LFoo;"), is(answer));
	}

	@Test
	public void parsePrimitive() {
		final String answer = "I";
		assertThat(TypeDescriptor.parseFirst("I"), is(answer));
		assertThat(TypeDescriptor.parseFirst("III"), is(answer));
		assertThat(TypeDescriptor.parseFirst("IJZ"), is(answer));
		assertThat(TypeDescriptor.parseFirst("I[I"), is(answer));
		assertThat(TypeDescriptor.parseFirst("ILFoo;"), is(answer));
		assertThat(TypeDescriptor.parseFirst("I[LFoo;"), is(answer));
	}

	@Test
	public void parseClass() {
		{
			final String answer = "LFoo;";
			assertThat(TypeDescriptor.parseFirst("LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;JZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;[LFoo;"), is(answer));
		}
		{
			final String answer = "Ljava/lang/String;";
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;JZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;[LFoo;"), is(answer));
		}
	}

	@Test
	public void parseArray() {
		{
			final String answer = "[I";
			assertThat(TypeDescriptor.parseFirst("[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[III"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[IJZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[I[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[ILFoo;"), is(answer));
		}
		{
			final String answer = "[[I";
			assertThat(TypeDescriptor.parseFirst("[[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[III"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[IJZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[I[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[ILFoo;"), is(answer));
		}
		{
			final String answer = "[LFoo;";
			assertThat(TypeDescriptor.parseFirst("[LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;II"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;JZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;LFoo;"), is(answer));
		}
	}

	@Test
	public void equals() {
		assertThat(new TypeDescriptor("V"), is(new TypeDescriptor("V")));
		assertThat(new TypeDescriptor("Z"), is(new TypeDescriptor("Z")));
		assertThat(new TypeDescriptor("B"), is(new TypeDescriptor("B")));
		assertThat(new TypeDescriptor("C"), is(new TypeDescriptor("C")));
		assertThat(new TypeDescriptor("I"), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("J"), is(new TypeDescriptor("J")));
		assertThat(new TypeDescriptor("F"), is(new TypeDescriptor("F")));
		assertThat(new TypeDescriptor("D"), is(new TypeDescriptor("D")));
		assertThat(new TypeDescriptor("LFoo;"), is(new TypeDescriptor("LFoo;")));
		assertThat(new TypeDescriptor("[I"), is(new TypeDescriptor("[I")));
		assertThat(new TypeDescriptor("[[[I"), is(new TypeDescriptor("[[[I")));
		assertThat(new TypeDescriptor("[LFoo;"), is(new TypeDescriptor("[LFoo;")));

		assertThat(new TypeDescriptor("V"), is(not(new TypeDescriptor("I"))));
		assertThat(new TypeDescriptor("I"), is(not(new TypeDescriptor("J"))));
		assertThat(new TypeDescriptor("I"), is(not(new TypeDescriptor("LBar;"))));
		assertThat(new TypeDescriptor("I"), is(not(new TypeDescriptor("[I"))));
		assertThat(new TypeDescriptor("LFoo;"), is(not(new TypeDescriptor("LBar;"))));
		assertThat(new TypeDescriptor("[I"), is(not(new TypeDescriptor("[Z"))));
		assertThat(new TypeDescriptor("[[[I"), is(not(new TypeDescriptor("[I"))));
		assertThat(new TypeDescriptor("[LFoo;"), is(not(new TypeDescriptor("[LBar;"))));
	}

	@Test
	public void testToString() {
		assertThat(new TypeDescriptor("V").toString(), is("V"));
		assertThat(new TypeDescriptor("Z").toString(), is("Z"));
		assertThat(new TypeDescriptor("B").toString(), is("B"));
		assertThat(new TypeDescriptor("C").toString(), is("C"));
		assertThat(new TypeDescriptor("I").toString(), is("I"));
		assertThat(new TypeDescriptor("J").toString(), is("J"));
		assertThat(new TypeDescriptor("F").toString(), is("F"));
		assertThat(new TypeDescriptor("D").toString(), is("D"));
		assertThat(new TypeDescriptor("LFoo;").toString(), is("LFoo;"));
		assertThat(new TypeDescriptor("[I").toString(), is("[I"));
		assertThat(new TypeDescriptor("[[[I").toString(), is("[[[I"));
		assertThat(new TypeDescriptor("[LFoo;").toString(), is("[LFoo;"));
	}
}
