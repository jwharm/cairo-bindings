/* cairo-java-bindings - Java language bindings for cairo
 * Copyright (C) 2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.cairobindings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * The LibLoad class is used internally to load native libraries by name
 */
public final class LibLoad {

    static {
        String javagiPath = System.getProperty("javagi.path");
        String javaPath = System.getProperty("java.library.path");
        if (javagiPath != null) {
            if (javaPath == null) {
                System.setProperty("java.library.path", javagiPath);
            } else {
                System.setProperty("java.library.path", javaPath + File.pathSeparator + javagiPath);
            }
        }
    }

    // Prevent instantiation
    private LibLoad() {}

    /**
     * Load the native library with the provided name
     * @param name the name of the library
     */
    public static void loadLibrary(String name) {
        RuntimeException fail = new RuntimeException("Could not load library " + name);
        try {
            System.loadLibrary(name);
            return;
        } catch (Throwable t) {
            fail.addSuppressed(t);
        }
        for (String s : System.getProperty("java.library.path").split(File.pathSeparator)) {
            if (s.isBlank()) {
                continue;
            }

            Path pk = Path.of(s).toAbsolutePath().normalize();
            if (!Files.isDirectory(pk)) {
                continue;
            }

            Path[] paths;
            try (Stream<Path> p = Files.list(pk)) {
                paths = p.toArray(Path[]::new);
            } catch (Throwable t) {
                fail.addSuppressed(t);
                continue;
            }

            for (Path path : paths) {
                try {
                    String fn = path.getFileName().toString();
                    if (fn.equals(name)) {
                        System.load(path.toString());
                        return;
                    }
                } catch (Throwable t) {
                    fail.addSuppressed(t);
                }
            }
        }
        throw fail;
    }
}
