package org.example.lab3.drools.model;

/**
 * Drools fact representing the outcome of a borrow evaluation.
 *
 * Inserted into the KieSession as a mutable OUTPUT fact.
 */
public class BorrowingDecision {

    private boolean allowed            = false;
    private String  reason             = "No matching rule found";
    private int     borrowingPeriodDays = 0;
    private boolean decided            = false;   // internal guard flag

    public void setAllowed(boolean allowed)                  { this.allowed = allowed; }
    public void setReason(String reason)                     { this.reason = reason; }
    public void setBorrowingPeriodDays(int borrowingPeriodDays) { this.borrowingPeriodDays = borrowingPeriodDays; }
    public void setDecided(boolean decided)                  { this.decided = decided; }

    public boolean isAllowed()             { return allowed; }
    public String  getReason()             { return reason; }
    public int     getBorrowingPeriodDays(){ return borrowingPeriodDays; }
    public boolean isDecided()             { return decided; }

    @Override public String toString() {
        return "BorrowingDecision{allowed=%s, period=%d days, reason='%s'}"
                .formatted(allowed, borrowingPeriodDays, reason);
    }
}
