package aluviz;

import java.awt.*;

/**
 * One in-flight wire animation: a value-blob traveling from src to dst.
 * Progress goes 0.0 → 1.0 over `durationMs` milliseconds with ease-in-out.
 */
public class WireAnimation {

    public final Point src;
    public final Point dst;
    public final String label;
    public final Color color;
    public final int durationMs;

    private long startNanos = -1;
    private boolean done = false;

    public WireAnimation(Point src, Point dst, String label, Color color, int durationMs) {
        this.src = src;
        this.dst = dst;
        this.label = label;
        this.color = color;
        this.durationMs = Math.max(50, durationMs);
    }

    public void start() {
        this.startNanos = System.nanoTime();
        this.done = false;
    }

    /** Current linear progress 0..1. */
    public double linearProgress() {
        if (startNanos < 0) return 0;
        if (done) return 1.0;
        double elapsed = (System.nanoTime() - startNanos) / 1_000_000.0;
        if (elapsed >= durationMs) { done = true; return 1.0; }
        return elapsed / durationMs;
    }

    /** Eased progress for smoother movement (ease-in-out). */
    public double easedProgress() {
        double t = linearProgress();
        return 0.5 - Math.cos(Math.PI * t) / 2.0;
    }

    public boolean isDone() {
        linearProgress();   // updates done flag
        return done;
    }

    /** Current interpolated position along the path. */
    public Point currentPos() {
        double t = easedProgress();
        int x = (int) (src.x + (dst.x - src.x) * t);
        int y = (int) (src.y + (dst.y - src.y) * t);
        return new Point(x, y);
    }
}
