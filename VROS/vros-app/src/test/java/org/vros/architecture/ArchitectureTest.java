package org.vros.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit rules enforcing clean-architecture layer dependencies:
 * <ul>
 *   <li>domain must not depend on application, infrastructure, or api</li>
 *   <li>application must not depend on infrastructure or api</li>
 *   <li>api must not depend on infrastructure</li>
 *   <li>domain.port classes must be interfaces</li>
 *   <li>security must not leak into domain or application</li>
 * </ul>
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.vros");
    }

    @Test
    void domain_should_not_depend_on_outer_layers() {
        ArchRule rule = noClasses().that().resideInAPackage("org.vros.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.vros.application..",
                        "org.vros.infrastructure..",
                        "org.vros.api.."
                );
        rule.check(classes);
    }

    @Test
    void application_should_not_depend_on_infrastructure_or_api() {
        ArchRule rule = noClasses().that().resideInAPackage("org.vros.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.vros.infrastructure..",
                        "org.vros.api.."
                );
        rule.check(classes);
    }

    @Test
    void api_should_not_depend_on_infrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("org.vros.api..")
                .should().dependOnClassesThat().resideInAPackage("org.vros.infrastructure..");
        rule.check(classes);
    }

    @Test
    void domain_ports_should_be_interfaces() {
        ArchRule rule = classes().that().resideInAPackage("org.vros.domain.port")
                .should().beInterfaces();
        rule.check(classes);
    }

    @Test
    void security_classes_must_not_leak_into_domain_or_application() {
        ArchRule rule = noClasses().that().resideInAnyPackage("org.vros.domain..", "org.vros.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.security..",
                        "org.vros.security.."
                );
        rule.check(classes);
    }

    @Test
    void domain_must_not_depend_on_any_spring_framework() {
        ArchRule rule = noClasses().that().resideInAPackage("org.vros.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");
        rule.check(classes);
    }
}
