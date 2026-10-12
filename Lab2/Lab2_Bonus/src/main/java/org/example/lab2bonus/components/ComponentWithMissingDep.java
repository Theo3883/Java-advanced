package org.example.lab2bonus.components;

import org.example.lab2bonus.annotation.MyComponent;

/**
 * Annotated component that forces a {@link NoClassDefFoundError} at class-initialization
 * time when loaded via Reflection, because its static initializer instantiates a type
 * ({@code com.example.nonexistent.NonExistentClass}) that is absent from the runtime
 * classpath (excluded from the fat JAR by the maven-shade-plugin filter).
 *
 * <p>Behaviour comparison:
 * <ul>
 *   <li><b>Reflection scanner</b> – {@link Class#forName} triggers the static block
 *       → JVM tries to resolve {@code NonExistentClass} → throws
 *       {@link ExceptionInInitializerError} wrapping {@link NoClassDefFoundError}.
 *       The scanner records an error and does NOT add this class to results.</li>
 *   <li><b>ASM scanner</b> – reads raw bytecode as a stream of bytes, parses the
 *       annotation descriptor as a {@link String}, never resolves any type references.
 *       Finds {@code @MyComponent} and adds this class to results with no error.</li>
 * </ul>
 */
@MyComponent("ComponentWithMissingDep")
public class ComponentWithMissingDep {

    /**
     * Static initializer forces resolution of {@code NonExistentClass} at
     * class-loading time. Since the class is absent from the runtime classpath,
     * the JVM throws {@link NoClassDefFoundError} here.
     *
     * ASM never executes this block – it only reads the bytecode.
     */
    static {
        // This triggers class loading of NonExistentClass at <clinit> time.
        // Reflection-based scanning will crash here; ASM-based scanning is unaffected.
        @SuppressWarnings("unused")
        Object dep = new com.example.nonexistent.NonExistentClass();
    }

    private com.example.nonexistent.NonExistentClass dependency;
}
