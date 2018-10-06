package org.cowboycoders.ant.examples.demos.pwr.events;

public class WheelTorquePowerEvent {
    private final int deviceId;
    private final int pwr;
    private final int cadence;

    public WheelTorquePowerEvent(int deviceId, int pwr, int cadence) {
        this.deviceId = deviceId;
        this.pwr = pwr;
        this.cadence = cadence;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getPwr() {
        return pwr;
    }

    public int getCadence() {
        return cadence;
    }
}
