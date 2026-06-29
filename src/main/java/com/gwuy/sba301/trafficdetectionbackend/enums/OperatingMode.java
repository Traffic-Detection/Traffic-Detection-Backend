package com.gwuy.sba301.trafficdetectionbackend.enums;

public enum OperatingMode {

    AI(true),
    MANUAL(false);

    private final boolean aiAllowed;

    OperatingMode(boolean aiAllowed) {
        this.aiAllowed = aiAllowed;
    }

    /**
     * Determines whether AI-based signal processing is permitted in this mode.
     * Only AI allows the scheduler to compute adaptive signals.
     *
     * @return true if AI processing is allowed, false otherwise
     */
    public boolean isAiAllowed() {
        return aiAllowed;
    }
}