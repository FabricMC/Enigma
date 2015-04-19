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
package cuchaz.enigma.convert;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import cuchaz.enigma.Util;
import cuchaz.enigma.mapping.ClassEntry;


public class ClassMatch {
	
	public Set<ClassEntry> sourceClasses;
	public Set<ClassEntry> destClasses;
	
	public ClassMatch(Collection<ClassEntry> sourceClasses, Collection<ClassEntry> destClasses) {
		this.sourceClasses = Sets.newHashSet(sourceClasses);
		this.destClasses = Sets.newHashSet(destClasses);
	}
	
	public ClassMatch(ClassEntry sourceClass, ClassEntry destClass) {
		sourceClasses = Sets.newHashSet();
		if (sourceClass != null) {
			sourceClasses.add(sourceClass);
		}
		destClasses = Sets.newHashSet();
		if (destClass != null) {
			destClasses.add(destClass);
		}
	}
	
	public boolean isMatched() {
		return sourceClasses.size() > 0 && destClasses.size() > 0;
	}

	public boolean isAmbiguous() {
		return sourceClasses.size() > 1 || destClasses.size() > 1;
	}
	
	public ClassEntry getUniqueSource() {
		if (sourceClasses.size() != 1) {
			throw new IllegalStateException("Match has ambiguous source!");
		}
		return sourceClasses.iterator().next();
	}
	
	public ClassEntry getUniqueDest() {
		if (destClasses.size() != 1) {
			throw new IllegalStateException("Match has ambiguous source!");
		}
		return destClasses.iterator().next();
	}

	public Set<ClassEntry> intersectSourceClasses(Set<ClassEntry> classes) {
		Set<ClassEntry> intersection = Sets.newHashSet(sourceClasses);
		intersection.retainAll(classes);
		return intersection;
	}
	
	@Override
	public int hashCode() {
		return Util.combineHashesOrdered(sourceClasses, destClasses);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassMatch) {
			return equals((ClassMatch)other);
		}
		return false;
	}
	
	public boolean equals(ClassMatch other) {
		return this.sourceClasses.equals(other.sourceClasses)
			&& this.destClasses.equals(other.destClasses);
	}
}
