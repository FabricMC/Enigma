/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.translation.mapping.serde;

import java.io.File;
import java.nio.file.Path;

public class MappingParseException extends Exception {
	private int line;
	private String message;
	private Path filePath;

	public MappingParseException(File file, int line, String message) {
		this(file.toPath(), line, message);
	}

	public MappingParseException(Path filePath, int line, String message) {
		this.line = line;
		this.message = message;
		this.filePath = filePath;
	}

	public MappingParseException(Path filePath, int line, Throwable cause) {
		super(cause);
		this.line = line;
		this.message = cause.toString();
		this.filePath = filePath;
	}

	@Override
	public String getMessage() {
		return "Line " + line + ": " + message + " in file " + filePath.toAbsolutePath().toString();
	}
}
