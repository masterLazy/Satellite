package masterlazy.satellite.client.remote;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;

public class RateCounter {
    record Sample (Instant moment, long capacity) {}
    private final Queue<Sample> samples = new ArrayDeque<>();
    private long total;
    private Instant begin;

    public static final Duration WINDOW = Duration.ofSeconds(3);

    public void submit(long capacity) {
        samples.add(new Sample(Instant.now(), capacity));
        total += capacity;
        if (begin == null) {
            begin = Instant.now();
        }
    }

    public long getTotal() {
        return total;
    }

    public long getPerSecond() {
        if (begin == null) return 0;
        Instant now = Instant.now();
        while (!samples.isEmpty()) {
            if (samples.peek().moment.isBefore(now.minus(WINDOW))) {
                samples.poll();
            } else {
                break;
            }
        }
        long total = 0;
        for (Sample s : samples) {
            total += s.capacity;
        }
        if (begin.isAfter(now.minus(WINDOW))) {
            return Math.round(total * 1.0 / (Duration.between(begin, now).toMillis() / 1000.0));
        }
        return Math.round(total * 1.0 / (WINDOW.toMillis() / 1000.0));
    }
}
