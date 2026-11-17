package com.doloreshazze.onesecond;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import static com.doloreshazze.onesecond.PuzzleRound.*;

final class VerifyShots {
    public static void main(String[] args) throws IOException {
        EnumMap<Level, PuzzleRound[]> byLevel = new EnumMap<>(Level.class);
        for (Level level : Level.values()) {
            PuzzleRound[] shots = new PuzzleRound[360];
            EnumMap<Outcome, Integer> counts = new EnumMap<>(Outcome.class);
            for (int angle = -180; angle < 180; angle++) {
                PuzzleRound shot = simulate(level, angle);
                shots[angle + 180] = shot;
                counts.merge(shot.event, 1, Integer::sum);
            }
            byLevel.put(level, shots);
            System.out.println(level + " " + counts);
        }
        expect(byLevel, Level.VASE, 54, Outcome.VASE_SAVED);
        expect(byLevel, Level.CAT, -35, Outcome.FISH_SAVED);
        expect(byLevel, Level.CAT, -90, Outcome.FISH_FELL);

        List<String> script = new ArrayList<>();
        add(script, byLevel, Level.VASE, Outcome.VASE_BROKEN, "advance");
        add(script, byLevel, Level.VASE, Outcome.VASE_SAVED, "advance");
        add(script, byLevel, Level.CAT, Outcome.CAT_AWAKE, "advance");
        add(script, byLevel, Level.CAT, Outcome.FISH_SAVED, "advance");
        add(script, byLevel, Level.DROP, Outcome.INK_BLOT, "advance");
        add(script, byLevel, Level.DROP, Outcome.DROP_CAUGHT, "replay-3");
        add(script, byLevel, Level.DROP, Outcome.PAPER_SAVED, "advance");
        add(script, byLevel, Level.KEY, Outcome.KEY_LOST, "advance");
        add(script, byLevel, Level.KEY, Outcome.KEY_SAVED, "advance");
        add(script, byLevel, Level.GLIDER, Outcome.GLIDER_WET, "advance");
        add(script, byLevel, Level.GLIDER, Outcome.GLIDER_LANDED, "advance");
        add(script, byLevel, Level.TRAIN, Outcome.TRAIN_DERAILED, "advance");
        add(script, byLevel, Level.TRAIN, Outcome.TRAIN_CROSSED, "advance");
        add(script, byLevel, Level.NOTES, Outcome.NOTES_MISSED, "advance");
        add(script, byLevel, Level.NOTES, Outcome.NOTES_PLAYED, "advance");
        add(script, byLevel, Level.PHOTO, Outcome.PHOTO_MISSED, "advance");
        add(script, byLevel, Level.PHOTO, Outcome.PHOTO_TAKEN, "advance");
        add(script, byLevel, Level.CLOCK, Outcome.CLOCK_JAMMED, "advance");
        add(script, byLevel, Level.CLOCK, Outcome.CLOCK_STARTED, "advance");
        add(script, byLevel, Level.WATER, Outcome.BOOT_FILLED, "advance");
        add(script, byLevel, Level.WATER, Outcome.FLAG_RAISED, "advance");
        add(script, byLevel, Level.MAGNET, Outcome.CLIPS_CAUGHT, "advance");
        add(script, byLevel, Level.MAGNET, Outcome.TOKEN_CAUGHT, "advance");
        add(script, byLevel, Level.SCALE, Outcome.WEIGHT_ADDED, "replay-12");
        add(script, byLevel, Level.SCALE, Outcome.WEIGHT_REMOVED, "advance");
        add(script, byLevel, Level.POST, Outcome.PARCEL_IN_HAT, "advance");
        add(script, byLevel, Level.POST, Outcome.PARCEL_DELIVERED, "advance");
        add(script, byLevel, Level.THEATRE, Outcome.DRAGON_SHADOW, "replay-14");
        add(script, byLevel, Level.THEATRE, Outcome.HARE_SHADOW, "advance");
        add(script, byLevel, Level.LOCKS, Outcome.ONE_TRUMPET, "advance");
        add(script, byLevel, Level.LOCKS, Outcome.BAND_PLAYED, "advance");
        add(script, byLevel, Level.MOUSE, Outcome.MOUSE_CUDDLED, "advance");
        add(script, byLevel, Level.MOUSE, Outcome.CAT_LIT_SIGN, "advance");
        add(script, byLevel, Level.KITE, Outcome.KITE_FLEW, "advance");
        add(script, byLevel, Level.CANAL, Outcome.BOAT_DOCKED, "advance");
        add(script, byLevel, Level.ECHO, Outcome.FAIR_SHUT, "advance");
        add(script, byLevel, Level.ECHO, Outcome.FAIR_OPEN, "advance");
        Files.write(Path.of(args[0]), script);
    }

    private static PuzzleRound simulate(Level level, int angle) {
        PuzzleRound round = new PuzzleRound(level);
        double radians = Math.toRadians(angle);
        round.launch((float) Math.cos(radians), (float) Math.sin(radians));
        for (int frame = 0; frame < 200 && round.active(); frame++) {
            round.advance(1f / 60);
        }
        if (round.active() || round.event == null) {
            throw new AssertionError("Unfinished shot: " + level + " " + angle);
        }
        return round;
    }

    private static void expect(EnumMap<Level, PuzzleRound[]> byLevel, Level level, int angle, Outcome expected) {
        Outcome actual = byLevel.get(level)[angle + 180].event;
        if (actual != expected) throw new AssertionError(level + " " + angle + " gave " + actual);
    }

    private static void add(List<String> script, EnumMap<Level, PuzzleRound[]> byLevel,
                            Level level, Outcome outcome, String action) {
        int angle = stableAngle(byLevel.get(level), outcome);
        double radians = Math.toRadians(angle);
        int x = 300 + (int) Math.round(Math.cos(radians) * 160);
        int y = 900 + (int) Math.round(Math.sin(radians) * 160);
        System.out.println(level + " " + angle + " degrees => " + outcome);
        script.add(String.join("\t", level.name().toLowerCase(Locale.ROOT), level.title,
                outcome.name().toLowerCase(Locale.ROOT), outcome.title,
                Integer.toString(x), Integer.toString(y), action));
    }

    private static int stableAngle(PuzzleRound[] shots, Outcome expected) {
        int bestLength = 0, bestEnd = 0, length = 0;
        for (int i = 0; i < shots.length * 2; i++) {
            PuzzleRound shot = shots[i % shots.length];
            boolean matches = shot.event == expected;
            if (expected == Outcome.KEY_SAVED) matches &= shot.workshop.has(WorkshopRound.Action.PLATE);
            length = matches ? length + 1 : 0;
            if (length > bestLength) {
                bestLength = length;
                bestEnd = i;
            }
        }
        if (bestLength < 3) throw new AssertionError("No reliable swipe for " + expected);
        return (bestEnd - (bestLength - 1) / 2) % shots.length - 180;
    }
}
