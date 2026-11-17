package com.doloreshazze.onesecond;

import java.util.EnumMap;

final class VerifyShots {
    public static void main(String[] args) {
        for (PuzzleRound.Level level : PuzzleRound.Level.values()) {
            EnumMap<PuzzleRound.Outcome, Integer> counts = new EnumMap<>(PuzzleRound.Outcome.class);
            for (int angle = -180; angle < 180; angle++) {
                counts.merge(simulate(level, angle), 1, Integer::sum);
            }
            System.out.println(level + " " + counts);
        }
        expect(PuzzleRound.Level.VASE, 54, PuzzleRound.Outcome.VASE_SAVED);
        expect(PuzzleRound.Level.VASE, 90, PuzzleRound.Outcome.VASE_BROKEN);
        expect(PuzzleRound.Level.CAT, -35, PuzzleRound.Outcome.FISH_SAVED);
        expect(PuzzleRound.Level.CAT, 0, PuzzleRound.Outcome.CAT_AWAKE);
        expect(PuzzleRound.Level.CAT, -90, PuzzleRound.Outcome.FISH_FELL);
    }

    private static PuzzleRound.Outcome simulate(PuzzleRound.Level level, int angle) {
        PuzzleRound round = new PuzzleRound(level);
        double radians = Math.toRadians(angle);
        round.launch((float) Math.cos(radians), (float) Math.sin(radians));
        for (int frame = 0; frame < 160 && round.active(); frame++) {
            round.advance(1f / 60);
        }
        if (round.active() || round.event == null) {
            throw new AssertionError("Unfinished shot: " + level + " " + angle);
        }
        return round.event;
    }

    private static void expect(PuzzleRound.Level level, int angle, PuzzleRound.Outcome expected) {
        PuzzleRound.Outcome actual = simulate(level, angle);
        if (actual != expected) {
            throw new AssertionError(level + " " + angle + " gave " + actual + ", expected " + expected);
        }
        System.out.println(level + " " + angle + " degrees => " + actual);
    }
}
