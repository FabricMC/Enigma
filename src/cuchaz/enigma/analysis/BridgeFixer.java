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

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import com.beust.jcommander.internal.Lists;

public class BridgeFixer
{
	public void fixBridges( CtClass c )
	{
		// bridge methods are scrubbed and marked as synthetic methods by the obfuscator
		// need to figure out which synthetics are bridge methods and restore them
		for( CtMethod method : c.getDeclaredMethods() )
		{
			// skip non-synthetic methods
			if( ( method.getModifiers() & AccessFlag.SYNTHETIC ) == 0 )
			{
				continue;
			}
			
			CtMethod bridgedMethod = getBridgedMethod( method );
			if( bridgedMethod != null )
			{
				bridgedMethod.setName( method.getName() );
				method.setModifiers( method.getModifiers() | AccessFlag.BRIDGE );
			}
		}
	}

	private CtMethod getBridgedMethod( CtMethod method )
	{
		// bridge methods just call another method, cast it to the return type, and return the result
		// let's see if we can detect this scenario
		
		// get all the called methods
		final List<MethodCall> methodCalls = Lists.newArrayList();
		try
		{
			method.instrument( new ExprEditor( )
			{
				@Override
				public void edit( MethodCall call )
				{
					methodCalls.add( call );
				}
			} );
		}
		catch( CannotCompileException ex )
		{
			// this is stupid... we're not even compiling anything
			throw new Error( ex );
		}
		
		// is there just one?
		if( methodCalls.size() != 1 )
		{
			return null;
		}
		MethodCall call = methodCalls.get( 0 );
		
		try
		{
			// we have a bridge method!
			return call.getMethod();
		}
		catch( NotFoundException ex )
		{
			// can't find the type? not a bridge method
			ex.printStackTrace( System.err );
			return null;
		}
	}
}
