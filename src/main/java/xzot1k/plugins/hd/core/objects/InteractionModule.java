/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.objects;

public class InteractionModule {
    private double passedChargeAmount;
    private String interactionId, interactionValue;

    public InteractionModule(String interactionId, String interactionValue, double passedChargeAmount) {
        setInteractionId(interactionId);
        setInteractionValue(interactionValue);
        setPassedChargeAmount(passedChargeAmount);
    }

    // getters & setters
    public double getPassedChargeAmount() {
        return passedChargeAmount;
    }

    public void setPassedChargeAmount(double passedChargeAmount) {
        this.passedChargeAmount = passedChargeAmount;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
    }

    public String getInteractionValue() {
        return interactionValue;
    }

    public void setInteractionValue(String interactionValue) {
        this.interactionValue = interactionValue;
    }
}
