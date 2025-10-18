package cuchaz.enigma.gui.util;

import java.io.IOException;
import java.io.InputStream;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import cuchaz.enigma.api.EnigmaIcon;
import cuchaz.enigma.utils.IconLoadingService;

public class IconLoadingServiceImpl implements IconLoadingService {
	@Override
	public EnigmaIcon loadIcon(InputStream in) throws IOException {
		return new EnigmaIconImpl(new FlatSVGIcon(in));
	}
}
