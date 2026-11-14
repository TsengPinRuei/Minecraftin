package com.minecraftin.clone.game;

// Keep exit decisions separate from GLFW so failures and deliberate discard can be tested.
final class SaveController {
    enum ExitState { RUNNING, FAILED, CONFIRM_DISCARD, READY }

    @FunctionalInterface
    interface SaveOperation {
        void save() throws Exception;
    }

    private ExitState exitState = ExitState.RUNNING;
    private Exception failure;
    private boolean discarded;
    private double lastSaveMillis;

    boolean attempt(SaveOperation operation, boolean exiting) {
        long started = System.nanoTime();
        try {
            operation.save();
            failure = null;
            if (exiting) exitState = ExitState.READY;
            return true;
        } catch (Exception error) {
            failure = error;
            if (exiting) exitState = ExitState.FAILED;
            return false;
        } finally {
            lastSaveMillis = (System.nanoTime() - started) / 1_000_000.0;
        }
    }

    boolean hasFailure() { return failure != null; }
    Exception failure() { return failure; }
    double lastSaveMillis() { return lastSaveMillis; }
    ExitState exitState() { return exitState; }
    boolean discarded() { return discarded; }
    boolean exitReady() { return exitState == ExitState.READY; }
    boolean exitPromptOpen() { return exitState == ExitState.FAILED || exitState == ExitState.CONFIRM_DISCARD; }

    void requestDiscard() {
        if (exitState == ExitState.FAILED) exitState = ExitState.CONFIRM_DISCARD;
    }

    void confirmDiscard() {
        if (exitState == ExitState.CONFIRM_DISCARD) {
            discarded = true;
            exitState = ExitState.READY;
        }
    }

    void cancel() {
        exitState = exitState == ExitState.CONFIRM_DISCARD ? ExitState.FAILED : ExitState.RUNNING;
    }
}
