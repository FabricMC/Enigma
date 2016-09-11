/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping;

import com.strobel.assembler.metadata.*;

import java.util.List;

public class ProcyonEntryFactory {

    private static String getErasedSignature(MemberReference def)
    {
        if (!(def instanceof MethodReference))
            return def.getErasedSignature();
        MethodReference methodReference = (MethodReference) def;
        StringBuilder builder = new StringBuilder("(");
        for (ParameterDefinition param : methodReference.getParameters())
        {
            TypeReference paramType = param.getParameterType();
            if (paramType.getErasedSignature().equals("Ljava/lang/Object;") && paramType.hasExtendsBound() && paramType.getExtendsBound() instanceof  CompoundTypeReference)
            {
                List<TypeReference> interfaces = ((CompoundTypeReference) paramType.getExtendsBound()).getInterfaces();
                interfaces.forEach((inter) -> builder.append(inter.getErasedSignature()));
            }
            else
                builder.append(paramType.getErasedSignature());
        }
        builder.append(")");

        // TODO: Fix Procyon render
        TypeReference returnType = methodReference.getReturnType();
        if (returnType.getErasedSignature().equals("Ljava/lang/Object;") && returnType.hasExtendsBound() && returnType.getExtendsBound() instanceof  CompoundTypeReference)
        {
            List<TypeReference> interfaces = ((CompoundTypeReference) returnType.getExtendsBound()).getInterfaces();
            interfaces.forEach((inter) -> builder.append(inter.getErasedSignature()));
        }
        else
            builder.append(returnType.getErasedSignature());
        return builder.toString();
    }

    public static FieldEntry getFieldEntry(MemberReference def) {
        return new FieldEntry(new ClassEntry(def.getDeclaringType().getInternalName()), def.getName(), new Type(def.getErasedSignature()));
    }

    public static MethodEntry getMethodEntry(MemberReference def) {
        return new MethodEntry(new ClassEntry(def.getDeclaringType().getInternalName()), def.getName(), new Signature(getErasedSignature(def)));
    }

    public static ConstructorEntry getConstructorEntry(MethodReference def) {
        if (def.isTypeInitializer()) {
            return new ConstructorEntry(new ClassEntry(def.getDeclaringType().getInternalName()));
        } else {
            return new ConstructorEntry(new ClassEntry(def.getDeclaringType().getInternalName()), new Signature(def.getErasedSignature()));
        }
    }

    public static BehaviorEntry getBehaviorEntry(MethodReference def) {
        if (def.isConstructor() || def.isTypeInitializer()) {
            return getConstructorEntry(def);
        } else {
            return getMethodEntry(def);
        }
    }
}
