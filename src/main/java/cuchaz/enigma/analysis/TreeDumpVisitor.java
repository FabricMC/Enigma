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

import com.strobel.componentmodel.Key;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Pattern;

import java.io.*;
import java.nio.charset.Charset;

public class TreeDumpVisitor extends DepthFirstAstVisitor<Void, Void> {

	private File file;
	private Writer out;

	public TreeDumpVisitor(File file) {
		this.file = file;
	}

	@Override
	public Void visitCompilationUnit(CompilationUnit node, Void ignored) {
		try {
			out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
			visitChildren(node, ignored);
			out.close();
			return null;
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

	@Override
	protected Void visitChildren(AstNode node, Void ignored) {
		// show the tree
		try {
			out.write(getIndent(node) + node.getClass().getSimpleName() + " " + getText(node) + " " + dumpUserData(node) + " " + node.getRegion() + "\n");
		} catch (IOException ex) {
			throw new Error(ex);
		}

		// recurse
		for (final AstNode child : node.getChildren()) {
			child.acceptVisitor(this, ignored);
		}
		return null;
	}

	private String getText(AstNode node) {
		if (node instanceof Identifier) {
			return "\"" + ((Identifier) node).getName() + "\"";
		}
		return "";
	}

	private String dumpUserData(AstNode node) {
		StringBuilder buf = new StringBuilder();
		for (Key<?> key : Keys.ALL_KEYS) {
			Object val = node.getUserData(key);
			if (val != null) {
				buf.append(String.format(" [%s=%s]", key, val));
			}
		}
		return buf.toString();
	}

	private String getIndent(AstNode node) {
		StringBuilder buf = new StringBuilder();
		int depth = getDepth(node);
		for (int i = 0; i < depth; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}

	private int getDepth(AstNode node) {
		int depth = -1;
		while (node != null) {
			depth++;
			node = node.getParent();
		}
		return depth;
	}
}
