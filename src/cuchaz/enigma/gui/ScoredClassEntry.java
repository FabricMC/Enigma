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
package cuchaz.enigma.gui;

import cuchaz.enigma.mapping.ClassEntry;


public class ScoredClassEntry extends ClassEntry {

	private static final long serialVersionUID = -8798725308554217105L;
	
	private float m_score;
	
	public ScoredClassEntry(ClassEntry other, float score) {
		super(other);
		m_score = score;
	}
	
	public float getScore() {
		return m_score;
	}
}
