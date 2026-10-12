package org.example.lab2bonus;

import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Scans a package by reading raw .class bytes with ASM's {@link ClassReader}.
 * Never loads a class into the JVM — no static blocks run, no Metaspace is
 * allocated, and missing dependencies are irrelevant (descriptors stay as strings).
 */
public class AsmScanner {

    private static final String MY_COMPONENT = "Lorg/example/lab2bonus/annotation/MyComponent;";

    final List<String> errors = new ArrayList<>();
    int visited;

    public List<String> scan(String pkg, ClassLoader cl) throws IOException {
        errors.clear();
        visited = 0;
        List<String> found = new ArrayList<>();

        for (Path path : ReflectionScanner.classPaths(pkg, cl)) {
            visited++;
            String name = ReflectionScanner.pathToClassName(path, pkg);
            try (InputStream in = Files.newInputStream(path)) {
                if (hasAnnotation(in)) found.add(name);
            } catch (IOException e) {
                errors.add(name + " → " + e.getMessage());
            }
        }

        Collections.sort(found);
        return found;
    }

    private boolean hasAnnotation(InputStream in) throws IOException {
        boolean[] found = {false};
        new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (MY_COMPONENT.equals(desc)) found[0] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found[0];
    }
}
