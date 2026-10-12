package com.example.nonexistent;

/**
 * Compile-time stub for NonExistentClass.
 *
 * This class is compiled alongside ComponentWithMissingDep so the build succeeds,
 * but it is intentionally EXCLUDED from the final JAR by the maven-shade-plugin's
 * exclusion filter below.
 *
 * At runtime the JVM cannot resolve com.example.nonexistent.NonExistentClass, so:
 *   - Class.forName("...ComponentWithMissingDep") → NoClassDefFoundError
 *   - ASM bytecode scan                           → succeeds (never loads the class)
 */
public class NonExistentClass {
    // intentionally empty stub – excluded from runtime classpath
}
