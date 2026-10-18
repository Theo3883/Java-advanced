package org.example.lab3.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Listens for {@link HeavyBorrowerEvent} and logs a warning. */
@Component
public class HeavyBorrowerEventListener {

    private static final Logger log = LoggerFactory.getLogger(HeavyBorrowerEventListener.class);

    @EventListener
    public void onHeavyBorrower(HeavyBorrowerEvent event) {
        log.warn("[EVENT] Heavy borrower detected! Member='{}' (role={}) has borrowed {} books.",
                event.getMember().getName(),
                event.getMember().getRole(),
                event.getBookCount());
    }
}
