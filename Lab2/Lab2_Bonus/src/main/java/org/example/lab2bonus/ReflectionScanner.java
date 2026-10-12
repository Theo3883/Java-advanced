package org.example.lab2bonus;

import org.example.lab2bonus.annotation.MyComponent;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Scans a package by calling {@link Class#forName} on every .class file.
 * Loads and initialises every class — triggers static blocks, allocates Metaspace,
 * and crashes on any class whose dependencies are missing at runtime.
 */
public class ReflectionScanner {

    final List<String> errors = new ArrayList<>();
    int visited;

    public List<String> scan(String pkg, ClassLoader cl) throws IOException {
        errors.clear();
        visited = 0;
        List<String> found = new ArrayList<>();

        for (Path path : classPaths(pkg, cl)) {
            visited++;
            String name = pathToClassName(path, pkg);
            try {
                if (Class.forName(name, true, cl).isAnnotationPresent(MyComponent.class))
                    found.add(name);
            } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                errors.add(name + " → " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        Collections.sort(found);
        return found;
    }

    // ── shared helpers (also used by AsmScanner) ─────────────────────────────

    static List<Path> classPaths(String pkg, ClassLoader cl) throws IOException {
        String resourcePath = pkg.replace('.', '/');
        Map<String, Path> uniqueFiles = new LinkedHashMap<>();

        Enumeration<URL> roots = cl.getResources(resourcePath);
        while (roots.hasMoreElements()) {
            URI root;
            try { root = roots.nextElement().toURI(); }
            catch (Exception e) { throw new RuntimeException(e); }

            boolean isJar = root.getScheme().equals("jar");
            FileSystem fs;
            if (isJar) {
                try { fs = FileSystems.newFileSystem(root, Map.of()); }
                catch (FileSystemAlreadyExistsException e) { fs = FileSystems.getFileSystem(root); }
            } else {
                fs = FileSystems.getDefault();
            }

            Path dir = isJar ? fs.getPath("/" + resourcePath) : fs.getPath(root.getPath());
            try (var walk = Files.walk(dir, 1)) {
                walk.filter(p -> p.toString().endsWith(".class") && !p.toString().contains("$"))
                    .forEach(p -> uniqueFiles.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        return new ArrayList<>(uniqueFiles.values());
    }

    static String pathToClassName(Path path, String pkg) {
        String fileName = path.getFileName().toString();
        return pkg + "." + fileName.substring(0, fileName.length() - 6); // strip .class
    }
}
