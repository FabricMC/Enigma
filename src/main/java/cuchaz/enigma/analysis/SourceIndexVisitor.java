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

package cuchaz.enigma.analysis;

import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;

public class SourceIndexVisitor extends DepthFirstAstVisitor<SourceIndex, Void> {
	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassDefEntry classEntry = ClassDefEntry.parse(def);
		index.addDeclaration(node.getNameToken(), classEntry);

		return node.acceptVisitor(new SourceIndexClassVisitor(classEntry), index);
	}

	@Override
	protected Void visitChildren(AstNode node, SourceIndex index) {
		for (final AstNode child : node.getChildren()) {
			child.acceptVisitor(this, index);
		}
		return null;
	}
}
