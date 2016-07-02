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
package cuchaz.enigma;

import com.google.common.io.CharStreams;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.Descriptor;

public class Util {

    public static int combineHashesOrdered(Object... objs) {
        return combineHashesOrdered(Arrays.asList(objs));
    }

    public static int combineHashesOrdered(Iterable<Object> objs) {
        final int prime = 67;
        int result = 1;
        for (Object obj : objs) {
            result *= prime;
            if (obj != null) {
                result += obj.hashCode();
            }
        }
        return result;
    }

    public static String readStreamToString(InputStream in) throws IOException {
        return CharStreams.toString(new InputStreamReader(in, "UTF-8"));
    }

    public static String readResourceToString(String path) throws IOException {
        InputStream in = Util.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found! " + path);
        }
        return readStreamToString(in);
    }

    public static void openUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException ex) {
                throw new Error(ex);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }
}
