package dev.priyan.cut;

/** Non-destructive edit decisions. Source media is never modified. */
public final class Clip {
    public String uri, name, text = "";
    public long durationMs, inMs, outMs;
    public int rotation;
    public boolean muted, grayscale;
    public Clip(String uri, String name, long durationMs) {
        if (durationMs <= 0) throw new IllegalArgumentException("Video duration must be positive");
        this.uri = uri; this.name = name; this.durationMs = durationMs; this.outMs = durationMs;
    }
    public long lengthMs() { return outMs - inMs; }
    public void trim(long start, long end) {
        if (start < 0 || end > durationMs || end - start < 100) throw new IllegalArgumentException("Keep at least 0.1 seconds within the source duration");
        inMs = start; outMs = end;
    }
    public Clip copy() {
        Clip c = new Clip(uri, name, durationMs); c.inMs = inMs; c.outMs = outMs;
        c.rotation = rotation; c.muted = muted; c.grayscale = grayscale; c.text = text; return c;
    }
    public Clip split(long relativeMs) {
        if (relativeMs < 100 || lengthMs() - relativeMs < 100) throw new IllegalArgumentException("Split must leave 0.1 seconds on each side");
        Clip right = copy(); right.inMs = inMs + relativeMs; outMs = right.inMs; return right;
    }
}
