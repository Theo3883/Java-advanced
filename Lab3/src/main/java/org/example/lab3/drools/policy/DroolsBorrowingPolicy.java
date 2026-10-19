package org.example.lab3.drools.policy;

import org.example.lab3.domain.Book;
import org.example.lab3.domain.Member;
import org.example.lab3.drools.model.BorrowingDecision;
import org.example.lab3.drools.model.BorrowingRequest;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Borrowing policy backed by the Drools rule engine.
 *
 * Compared to the plain Java policies in {@code org.example.lab3.policy},
 * this implementation has NO business logic in code. All rules live in
 * {@code src/main/resources/rules/borrowing-rules.drl}.
 */
@Component
public class DroolsBorrowingPolicy {

    private static final Logger log = LoggerFactory.getLogger(DroolsBorrowingPolicy.class);

    private final KieContainer kieContainer;

    public DroolsBorrowingPolicy(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    /**
     * Evaluates whether a member may borrow a book by firing all matching Drools rules.
     */
    public BorrowingDecision evaluate(Member member, Book book) {
        BorrowingRequest  request  = new BorrowingRequest(member, book);
        BorrowingDecision decision = new BorrowingDecision();

        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(request);
            session.insert(decision);
            int rulesFired = session.fireAllRules();
            log.debug("[Drools] {} rule(s) fired for: {}", rulesFired, request);
        } finally {
            session.dispose();   // always release resources
        }

        return decision;
    }

    public String policyName() {
        return "DroolsRuleEngine(borrowing-rules.drl)";
    }
}
