package org.cowboycoders.turbotrainers.bushido.brake;

import org.cowboycoders.utils.SimpleCsvLogger;

public class ConstantResistanceController extends AbstractController {

	private static final String ACTUAL_SPEED_HEADING = "Actual speed";
	private static final String POWER_HEADING = "power";
	private static final String ABSOLUTE_RESISTANCE_HEADING = "absolute resistance";
	private SimpleCsvLogger logger;
	private int resistance = 0;


	@Override
	public void onSpeedChange(double speed) {
		synchronized (this) {
			if (logger != null) {
				getDataModel().setAbsoluteResistance(resistance);
				logger.update(ABSOLUTE_RESISTANCE_HEADING, getDataModel().getAbsoluteResistance());
				logger.update(ACTUAL_SPEED_HEADING, speed);
			}
		}
		
	}

	@Override
	public void onPowerChange(double power) {
		synchronized (this) {
			if (logger != null) {
				logger.update(POWER_HEADING, power);
			}
		}
		
	}

	@Override
	public void onCadenceChange(double cadence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDistanceChange(double distance) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onHeartRateChange(double heartRate) {
		// TODO Auto-generated method stub
		
	}
	
	
	public synchronized void enableLogging(String dir, String filename) {
		this.logger = new SimpleCsvLogger(dir,filename,ACTUAL_SPEED_HEADING,POWER_HEADING,ABSOLUTE_RESISTANCE_HEADING);
		this.logger.addTime(false);
		this.logger.append(true);
	}
	
	public void setAbsoluteResistance(int newVal) {
		this.resistance = newVal;
	}
	
	@Override
	public void onStart() {
		getDataModel().setAbsoluteResistance(resistance);
	}

}