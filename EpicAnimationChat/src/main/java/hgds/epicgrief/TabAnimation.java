package hgds.epicgrief;

import java.util.List;

public record TabAnimation(String name, long intervalMillis, List<String> frames) {
    public TabAnimation {
        frames = List.copyOf(frames);
    }

    public String currentFrame(long elapsedMillis) {
        if (frames.isEmpty()) {
            return "";
        }

        long frame = Math.floorDiv(elapsedMillis, Math.max(1L, intervalMillis));
        return frames.get((int) Math.floorMod(frame, frames.size()));
    }
}
