package com.example.vitality.ui.activity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StepSessionTest {
    @Test
    public void countsOnlyStepsAfterTheCounterBaseline() {
        StepSession session = new StepSession(0, 100, true);

        assertFalse(session.isReady());
        assertTrue(session.record(90, 6350));
        assertTrue(session.isReady());
        assertEquals(0, session.steps());
        assertFalse(session.record(95, 6351));
        assertTrue(session.record(110, 6354));
        assertFalse(session.record(115, 6354));
        assertTrue(session.record(120, 6362));
        assertEquals(11, session.steps());
    }

    @Test
    public void aCounterResetPreservesStepsAlreadyRecorded() {
        StepSession session = new StepSession(0, 100, true);
        session.record(100, 200);
        session.record(110, 204);

        assertFalse(session.record(105, 201));
        assertFalse(session.record(120, Float.NaN));
        assertFalse(session.record(120, 0));
        assertTrue(session.record(130, 3));
        assertEquals(7, session.steps());
    }

    @Test
    public void theStepDetectorCountsEachNewStepOnce() {
        StepSession session = new StepSession(0, 100, false);

        assertTrue(session.isReady());
        assertFalse(session.record(99, 1));
        assertTrue(session.record(100, 1));
        assertFalse(session.record(100, 1));
        assertFalse(session.record(110, 0));
        assertTrue(session.record(120, 1));
        assertEquals(2, session.steps());
    }
}
