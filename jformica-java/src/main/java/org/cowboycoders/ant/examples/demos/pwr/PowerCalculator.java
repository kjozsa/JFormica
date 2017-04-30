package org.cowboycoders.ant.examples.demos.pwr;

public class PowerCalculator
{
	private int lastPeriod = 0;
	private int lastTorque = 0;
	private int lastEventCount = 0;
	private boolean tickTock = false;

	public int calculate( int period, int eventCount, int accumTorque, int ticks )
	{
		int eventDelta = eventCount - lastEventCount;
		int periodDelta = period - lastPeriod;
		int torqueDelta = accumTorque - lastTorque;

		double avgAngularVelocity = (2.0 * Math.PI * eventDelta) / ( periodDelta / 2048.0); // rads/s
		double avgTorque = torqueDelta / (32.0 * eventDelta); // Nm

		int pwr = (int) (avgTorque * avgAngularVelocity); // W

		lastPeriod = period;
		lastEventCount = eventCount;
		lastTorque = accumTorque;

		return pwr;
	}
}
