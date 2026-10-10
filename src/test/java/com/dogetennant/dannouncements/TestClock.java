package com.dogetennant.dannouncements;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the test moves by hand. */
public final class TestClock extends Clock {

    private Instant now;

    public TestClock(String instant) {
        this.now = Instant.parse(instant);
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
}
