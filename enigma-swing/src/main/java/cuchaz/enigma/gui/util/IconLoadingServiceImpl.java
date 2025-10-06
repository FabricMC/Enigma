package cuchaz.enigma.gui.util;

import java.io.IOException;
import java.io.InputStream;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import cuchaz.enigma.api.EnigmaIcon;
import cuchaz.enigma.utils.IconLoadingService;

public class IconLoadingServiceImpl implements IconLoadingService {
	@Override
	public EnigmaIcon loadIcon(InputStream in) throws IOException {
		SVGDocument document = new SVGLoader().load(in);

		if (document == null) {
			throw new IOException("Could not load SVG document");
		}

		return new EnigmaIconImpl(document);
	}
}
