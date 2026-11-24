package com.bookstore.app;

import com.bookstore.sales.application.OrderEventRelay;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Runs the outbox relay at regular intervals on the server (ADR-0022), on the container's managed
 * scheduler, so order events left behind (the server stopped right after an order, or a notifier
 * was down) still go out. The interval is {@code bookstore.outbox.interval-seconds}.
 */
@ApplicationScoped
public class OutboxRelayTimer {

    /** The container's default scheduler (Jakarta Concurrency). */
    static final String SCHEDULER = "java:comp/DefaultManagedScheduledExecutorService";

    private static final System.Logger LOG = System.getLogger(OutboxRelayTimer.class.getName());

    private final OrderEventRelay relay;
    private final Duration interval;
    private ScheduledFuture<?> schedule;

    /**
     * Creates the timer.
     *
     * @param relay           the relay to run
     * @param intervalSeconds the seconds between two runs
     */
    @Inject
    public OutboxRelayTimer(OrderEventRelay relay,
                            @ConfigProperty(name = "bookstore.outbox.interval-seconds", defaultValue = "30")
                            long intervalSeconds) {
        this(relay, Duration.ofSeconds(intervalSeconds));
    }

    OutboxRelayTimer(OrderEventRelay relay, Duration interval) {
        this.relay = relay;
        this.interval = interval;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OutboxRelayTimer() {
        this(null, Duration.ZERO);
    }

    /**
     * Starts the runs on the container's scheduler once the application is up.
     *
     * @param startup the container's startup event
     */
    void onStart(@Observes Startup startup) {
        try {
            start(InitialContext.doLookup(SCHEDULER));
        } catch (NamingException e) {
            LOG.log(Level.WARNING, "No managed scheduler; order events are relayed only after each change", e);
        }
    }

    /**
     * Runs the relay on a scheduler, the first time after one interval.
     *
     * @param scheduler where to run it
     */
    synchronized void start(ScheduledExecutorService scheduler) {
        long millis = interval.toMillis();
        schedule = scheduler.scheduleWithFixedDelay(relay::relayQuietly, millis, millis, TimeUnit.MILLISECONDS);
        LOG.log(Level.INFO, "Order events are relayed from the outbox every {0}", interval);
    }

    /** Stops the runs when the application stops. */
    @PreDestroy
    synchronized void stop() {
        if (schedule != null) {
            schedule.cancel(false);
        }
    }
}
