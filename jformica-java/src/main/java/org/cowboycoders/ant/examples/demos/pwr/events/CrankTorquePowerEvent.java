package org.cowboycoders.ant.examples.demos.pwr.events;

public class CrankTorquePowerEvent {
    private final double deviceId;
    private final int pwr;
    private final int cadence;

    public CrankTorquePowerEvent(double deviceId, int pwr, int cadence) {
        this.deviceId = deviceId;
        this.pwr = pwr;
        this.cadence = cadence;
    }

    public double getDeviceId() {
        return deviceId;
    }

    public int getPwr() {
        return pwr;
    }

    public int getCadence() {
        return cadence;
    }
}
