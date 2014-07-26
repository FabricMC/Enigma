/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import jsyntaxpane.Token;
import jsyntaxpane.TokenType;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import cuchaz.enigma.mapping.ArgumentEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

class TreeVisitor extends TreeScanner<CompilationUnitTree, SourcedAst>
{
	private SourceIndex m_index;
	
	public TreeVisitor( SourceIndex index )
	{
		m_index = index;
	}
	
	@Override
	public CompilationUnitTree visitClass( ClassTree classTree, SourcedAst ast )
	{
		ClassEntry classEntry = indexClass( classTree, ast );
		
		// look at the class members
		for( Tree memberTree : classTree.getMembers() )
		{
			if( memberTree.getKind() == Kind.VARIABLE )
			{
				indexField( (VariableTree)memberTree, ast, classEntry );
			}
			else if( memberTree.getKind() == Kind.METHOD )
			{
				MethodTree methodTree = (MethodTree)memberTree;
				MethodEntry methodEntry = indexMethod( methodTree, ast, classEntry );
				
				// look at method arguments
				int argNum = 0;
				for( VariableTree variableTree : methodTree.getParameters() )
				{
					indexArgument( variableTree, ast, methodEntry, argNum++ );
				}
			}
		}
		
		return super.visitClass( classTree, ast );
	}
	
	private ClassEntry indexClass( ClassTree classTree, SourcedAst ast )
	{
		// build the entry
		ClassEntry entry = new ClassEntry( ast.getFullClassName( classTree.getSimpleName().toString() ) );
		
		// lex the source at this tree node
		for( Token token : new Lexer( ast.getSource( classTree ).toString() ) )
		{
			// scan until we get the first identifier
			if( token.type == TokenType.IDENTIFIER )
			{
				m_index.add( entry, offsetToken( token, ast.getStart( classTree ) ) );
				break;
			}
		}
		
		return entry;
	}
	
	private FieldEntry indexField( VariableTree variableTree, SourcedAst ast, ClassEntry classEntry )
	{
		// build the entry
		FieldEntry entry = new FieldEntry( classEntry, variableTree.getName().toString() );
		
		// lex the source at this tree node
		Lexer lexer = new Lexer( ast.getSource( variableTree ).toString() );
		for( Token token : lexer )
		{
			// scan until we find an identifier that matches the field name
			if( token.type == TokenType.IDENTIFIER && lexer.getText( token ).equals( entry.getName() ) )
			{
				m_index.add( entry, offsetToken( token, ast.getStart( variableTree ) ) );
				break;
			}
		}
		
		return entry;
	}
	
	private MethodEntry indexMethod( MethodTree methodTree, SourcedAst ast, ClassEntry classEntry )
	{
		// build the entry
		StringBuilder signature = new StringBuilder();
		signature.append( "(" );
		for( VariableTree variableTree : methodTree.getParameters() )
		{
			signature.append( toJvmType( variableTree.getType(), ast ) );
		}
		signature.append( ")" );
		if( methodTree.getReturnType() != null )
		{
			signature.append( toJvmType( methodTree.getReturnType(), ast ) );
		}
		else
		{
			signature.append( "V" );
		}
		MethodEntry entry = new MethodEntry( classEntry, methodTree.getName().toString(), signature.toString() );
		
		// lex the source at this tree node
		Lexer lexer = new Lexer( ast.getSource( methodTree ).toString() );
		for( Token token : lexer )
		{
			// scan until we find an identifier that matches the method name
			if( token.type == TokenType.IDENTIFIER && lexer.getText( token ).equals( entry.getName() ) )
			{
				m_index.add( entry, offsetToken( token, ast.getStart( methodTree ) ) );
				break;
			}
		}
		
		return entry;
	}
	
	private void indexArgument( VariableTree variableTree, SourcedAst ast, MethodEntry methodEntry, int index )
	{
		// build the entry
		ArgumentEntry entry = new ArgumentEntry( methodEntry, index, variableTree.getName().toString() );
		
		// lex the source at this tree node
		Lexer lexer = new Lexer( ast.getSource( variableTree ).toString() );
		for( Token token : lexer )
		{
			// scan until we find an identifier that matches the variable name
			if( token.type == TokenType.IDENTIFIER && lexer.getText( token ).equals( entry.getName() ) )
			{
				m_index.add( entry, offsetToken( token, ast.getStart( variableTree ) ) );
				break;
			}
		}
	}
	
	private Token offsetToken( Token in, int offset )
	{
		return new Token( in.type, in.start + offset, in.length );
	}
	
	private String toJvmType( Tree tree, SourcedAst ast )
	{
		switch( tree.getKind() )
		{
			case PRIMITIVE_TYPE:
			{
				PrimitiveTypeTree primitiveTypeTree = (PrimitiveTypeTree)tree;
				switch( primitiveTypeTree.getPrimitiveTypeKind() )
				{
					case BOOLEAN: return "Z";
					case BYTE: return "B";
					case CHAR: return "C";
					case DOUBLE: return "D";
					case FLOAT: return "F";
					case INT: return "I";
					case LONG: return "J";
					case SHORT: return "S";
					case VOID: return "V";

					default:
						throw new Error( "Unsupported primitive type: " + primitiveTypeTree.getPrimitiveTypeKind() );
				}
			}
			
			case IDENTIFIER:
			{
				IdentifierTree identifierTree = (IdentifierTree)tree;
				String className = identifierTree.getName().toString();
				className = ast.getFullClassName( className );
				return "L" + className.replace( ".", "/" ) + ";";
			}
			
			case ARRAY_TYPE:
			{
				ArrayTypeTree arrayTree = (ArrayTypeTree)tree;
				return "[" + toJvmType( arrayTree.getType(), ast );
			}
			
			
			default:
				throw new Error( "Unsupported type kind: " + tree.getKind() );
		}
	}
}

public class Analyzer
{
	public static SourceIndex analyze( String className, String source )
	{
		SourceIndex index = new SourceIndex();
		SourcedAst ast = getAst( className, source );
		ast.visit( new TreeVisitor( index ) );
		return index;
	}
	
	private static SourcedAst getAst( String className, String source )
	{
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
		JavaSourceFromString unit = new JavaSourceFromString( className, source );
		JavacTask task = (JavacTask)compiler.getTask( null, fileManager, null, null, null, Arrays.asList( unit ) );
		
		try
		{
			return new SourcedAst(
				task.parse().iterator().next(),
				Trees.instance( task )
			);
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
}
