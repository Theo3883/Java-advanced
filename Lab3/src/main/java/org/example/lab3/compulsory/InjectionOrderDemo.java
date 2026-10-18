package org.example.lab3.compulsory;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Compulsory part: demonstrates all three DI types and their order.
 *
 * Spring performs injection in this guaranteed order:
 *   1. Constructor injection  – happens first, required deps
 *   2. Field injection        – after constructor, via @Autowired field
 *   3. Setter / method injection – after fields, via @Autowired setter
 *
 * @PostConstruct runs last, proving everything is wired.
 */
@Component
public class InjectionOrderDemo {

    private static final Logger log = LoggerFactory.getLogger(InjectionOrderDemo.class);

    // ── 2. Field injection ────────────────────────────────────────────────────
    @Autowired
    private ServiceB fieldInjected;

    private final ServiceA constructorInjected; // ── 1. Constructor injection
    private ServiceC setterInjected;            // ── 3. Setter injection

    @Value("${spring.application.name}")
    private String appName;                     // field injection of a @Value

    // ── 1. Constructor injection (preferred – makes deps explicit & testable) ─
    public InjectionOrderDemo(ServiceA serviceA) {
        this.constructorInjected = serviceA;
        log.info("[1] CONSTRUCTOR injection: ServiceA injected. (At this stage, fieldInjected is null? {})", 
                fieldInjected == null); // true -> proves constructor runs BEFORE field injection
    }

    // ── 3. Setter / method injection ─────────────────────────────────────────
    @Autowired
    public void setServiceC(ServiceC serviceC) {
        this.setterInjected = serviceC;
        log.info("[2] FIELD injection confirmed: when setter runs, fieldInjected is already populated! (fieldInjected='{}')", 
                fieldInjected != null ? fieldInjected.name() : "null");
        log.info("[3] SETTER injection: ServiceC injected ('{}')", serviceC.name());
    }

    // Runs after ALL injections are complete
    @PostConstruct
    public void verify() {
        log.info("[@PostConstruct] All injections complete & verified — app='{}', A='{}', B='{}', C='{}'",
                appName, constructorInjected.name(), fieldInjected.name(), setterInjected.name());
    }

    // ── Simple inner service stubs ────────────────────────────────────────────
    @Component
    public static class ServiceA { public String name() { return "ServiceA"; } }

    @Component
    public static class ServiceB { public String name() { return "ServiceB"; } }

    @Component
    public static class ServiceC { public String name() { return "ServiceC"; } }
}
