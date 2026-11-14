package com.minecraftin.clone.game;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SaveControllerTest {
    @Test
    void failedAutosaveRemainsVisibleUntilSuccessfulRetry() {
        SaveController state = new SaveController();
        assertFalse(state.attempt(() -> { throw new IOException("disk full"); }, false));
        assertTrue(state.hasFailure());
        assertFalse(state.exitPromptOpen());
        assertFalse(state.exitReady());
        assertTrue(state.attempt(() -> { }, false));
        assertFalse(state.hasFailure());
        assertFalse(state.exitReady());
    }

    @Test
    void failedExitCanRetryOrReturnWithoutDiscardingFailure() {
        SaveController state = new SaveController();
        state.attempt(() -> { throw new IOException("read only"); }, true);
        assertTrue(state.exitPromptOpen());
        assertFalse(state.exitReady());
        state.cancel();
        assertEquals(SaveController.ExitState.RUNNING, state.exitState());
        assertTrue(state.hasFailure());
        state.attempt(() -> { }, true);
        assertTrue(state.exitReady());
        assertFalse(state.discarded());
        assertFalse(state.hasFailure());
    }

    @Test
    void discardRequiresExplicitSecondConfirmationAndCanBeCancelled() {
        SaveController state = new SaveController();
        state.attempt(() -> { throw new IOException("cannot replace"); }, true);
        state.confirmDiscard();
        assertFalse(state.exitReady());
        state.requestDiscard();
        assertEquals(SaveController.ExitState.CONFIRM_DISCARD, state.exitState());
        state.cancel();
        assertEquals(SaveController.ExitState.FAILED, state.exitState());
        assertFalse(state.exitReady());
        state.requestDiscard();
        state.confirmDiscard();
        assertTrue(state.exitReady());
        assertTrue(state.discarded());
    }
}
