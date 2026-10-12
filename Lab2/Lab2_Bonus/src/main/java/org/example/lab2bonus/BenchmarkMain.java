package org.example.lab2bonus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Compares Reflection vs ASM component scanning with LIVE, dynamically measured JVM metrics:
 * classes loaded delta, static initializer count, crash errors, and timings.
 */
public class BenchmarkMain {

    static final String PKG      = "org.example.lab2bonus.components";
    static final int    WARMUP   = 3;
    static final int    RUNS     = 10;
    static final String MISS_DEP = PKG + ".ComponentWithMissingDep";

    static ClassLoader freshClassLoader(ClassLoader parent) {
        URL url = BenchmarkMain.class.getProtectionDomain().getCodeSource().getLocation();
        return new URLClassLoader(new URL[]{url}, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith(PKG)) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> c = findLoadedClass(name);
                        if (c == null) {
                            c = findClass(name);
                        }
                        if (resolve) resolveClass(c);
                        return c;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };
    }

    static class RunReport {
        final List<String> found;
        final List<String> errors;
        final long classesLoadedDelta;
        final int staticInitsTriggered;
        final String missingDepBehavior;

        RunReport(List<String> found, List<String> errors, long classesLoadedDelta, int staticInitsTriggered) {
            this.found = found;
            this.errors = errors;
            this.classesLoadedDelta = classesLoadedDelta;
            this.staticInitsTriggered = staticInitsTriggered;

            if (found.contains(MISS_DEP)) {
                this.missingDepBehavior = "Found (OK)";
            } else {
                this.missingDepBehavior = errors.stream()
                        .filter(e -> e.contains("ComponentWithMissingDep"))
                        .findFirst()
                        .map(e -> {
                            int arrow = e.indexOf("→ ");
                            return arrow != -1 ? "Crashed: " + e.substring(arrow + 2) : "Crashed (" + e + ")";
                        })
                        .orElse("Not found");
            }
        }
    }

    @FunctionalInterface
    interface ScannerCall {
        List<String> execute() throws Exception;
    }

    private static RunReport measureSingleRun(ScannerCall call, List<String> errorList) throws Exception {
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

        long startClasses = classBean.getTotalLoadedClassCount();
        List<String> found;
        System.setErr(new PrintStream(capturedErr));
        try {
            found = call.execute();
        } finally {
            System.setErr(originalErr);
        }
        long endClasses = classBean.getTotalLoadedClassCount();

        int staticInits = (int) capturedErr.toString().lines()
                .filter(l -> l.contains("[STATIC-INIT]"))
                .count();

        return new RunReport(found, List.copyOf(errorList), endClasses - startClasses, staticInits);
    }

    public static void main(String[] args) throws Exception {
        ClassLoader appCl = BenchmarkMain.class.getClassLoader();

        // Ensure compile-time stub is absent at runtime (even when launched from IntelliJ's target/classes)
        URL stub = appCl.getResource("com/example/nonexistent/NonExistentClass.class");
        if (stub != null && "file".equals(stub.getProtocol())) {
            try { new java.io.File(stub.toURI()).delete(); } catch (Exception ignored) {}
        }

        // Warm up scanner infrastructure classes (ASM bytecode reader, ZipFileSystem, etc.)
        // so internal JVM harness classes don't skew the live metrics
        new AsmScanner().scan(PKG, appCl);

        // ── 1. Live behavioral measurement ───────────────────────────────────
        // A) ASM scan on component classes
        AsmScanner asm = new AsmScanner();
        RunReport asmReport = measureSingleRun(() -> asm.scan(PKG, appCl), asm.errors);

        // B) Reflection with already-cached classes
        // Load classes once into appCl
        new ReflectionScanner().scan(PKG, appCl);
        ReflectionScanner refCached = new ReflectionScanner();
        RunReport refCachedReport = measureSingleRun(() -> refCached.scan(PKG, appCl), refCached.errors);

        // C) Reflection with fresh ClassLoader (Uncached / Cold startup)
        ReflectionScanner refUncached = new ReflectionScanner();
        ClassLoader fresh = freshClassLoader(appCl);
        RunReport refUncachedReport = measureSingleRun(() -> refUncached.scan(PKG, fresh), refUncached.errors);

        System.out.println("=== Live Diagnostic Check ===");
        System.out.println("ASM found               : " + asmReport.found.size() + " @MyComponent classes");
        System.out.println("Reflection (Cached)     : " + refCachedReport.found.size() + " @MyComponent classes");
        System.out.println("Reflection (Uncached)   : " + refUncachedReport.found.size() + " @MyComponent classes");
        System.out.println("ASM missing-dep status  : " + asmReport.missingDepBehavior);
        System.out.println("Ref missing-dep status  : " + refUncachedReport.missingDepBehavior);

        // 2. Warm-up
        for (int i = 0; i < WARMUP; i++) {
            new AsmScanner().scan(PKG, appCl);
            new ReflectionScanner().scan(PKG, appCl);
        }

        // 3. Measured timing runs
        long[] asmMs = new long[RUNS];
        long[] refCachedMs = new long[RUNS];
        long[] refUncachedMs = new long[RUNS];

        // Mute [STATIC-INIT] logging during timing loop to avoid console write overhead
        PrintStream sysErr = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        try {
            for (int i = 0; i < RUNS; i++) {
                long t = System.nanoTime();
                new AsmScanner().scan(PKG, appCl);
                asmMs[i] = System.nanoTime() - t;

                t = System.nanoTime();
                new ReflectionScanner().scan(PKG, appCl);
                refCachedMs[i] = System.nanoTime() - t;

                ClassLoader freshCl = freshClassLoader(appCl);
                t = System.nanoTime();
                new ReflectionScanner().scan(PKG, freshCl);
                refUncachedMs[i] = System.nanoTime() - t;
            }
        } finally {
            System.setErr(sysErr);
        }

        System.out.println("\n=== Timing (" + RUNS + " runs) ===");
        for (int i = 0; i < RUNS; i++) {
            System.out.printf("  run %2d  ASM=%5.1f ms   Ref(Cached)=%5.1f ms   Ref(Uncached/Cold)=%6.1f ms%n",
                    i + 1, asmMs[i] / 1e6, refCachedMs[i] / 1e6, refUncachedMs[i] / 1e6);
        }

        // 4. Summary table with dynamic runtime data
        double asmMean = mean(asmMs);
        double refCachedMean = mean(refCachedMs);
        double refUncachedMean = mean(refUncachedMs);
        long totalLoaded = ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount();

        System.out.println("\n┌─────────────────────────────────────────┬──────────────┬──────────────┬────────────────────────────────────────────────────────┐");
        System.out.println("│ Metric                                  │     ASM      │ Ref (Cached) │ Ref (Uncached / Cold)                                  │");
        System.out.println("├─────────────────────────────────────────┼──────────────┼──────────────┼────────────────────────────────────────────────────────┤");
        row("Mean scan time (ms)",
                String.format("%.1f", asmMean / 1e6),
                String.format("%.1f", refCachedMean / 1e6),
                String.format("%.1f", refUncachedMean / 1e6));
        row("@MyComponent classes found",
                String.valueOf(asmReport.found.size()),
                String.valueOf(refCachedReport.found.size()),
                String.valueOf(refUncachedReport.found.size()));
        row("Classes loaded (measured delta)",
                asmReport.classesLoadedDelta + " classes",
                refCachedReport.classesLoadedDelta + " classes",
                refUncachedReport.classesLoadedDelta + " classes");
        row("Static inits triggered (count)",
                asmReport.staticInitsTriggered + " executed",
                refCachedReport.staticInitsTriggered + " executed",
                refUncachedReport.staticInitsTriggered + " executed");
        row("Missing-dep class behavior",
                asmReport.missingDepBehavior,
                refCachedReport.missingDepBehavior,
                refUncachedReport.missingDepBehavior);
        System.out.println("└─────────────────────────────────────────┴──────────────┴──────────────┴────────────────────────────────────────────────────────┘");
        System.out.printf("Total JVM classes loaded in process lifetime: %,d%n", totalLoaded);
    }

    private static void row(String label, String a, String b, String c) {
        System.out.printf("│ %-39s │ %-12s │ %-12s │ %-54s │%n", label, a, b, c);
    }

    private static double mean(long[] arr) {
        long s = 0; for (long v : arr) s += v; return (double) s / arr.length;
    }
}
