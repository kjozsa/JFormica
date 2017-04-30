package org.cowboycoders.ant.examples.demos.pwr;

import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.ChannelId;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.examples.NetworkKeys;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.DeviceInfoQueryable;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuousScanModePwr
{
	private static final Logger log = LoggerFactory.getLogger( ContinuousScanModePwr.class );
	/*
	 * See ANT+ data sheet for explanation
	 */
	private static final int PWR_CHANNEL_PERIOD = 8182;
	/*
	 * See ANT+ data sheet for explanation
	 */
	private static final int PWR_CHANNEL_FREQ = 57;
	/*
	 * This should match the device you are connecting with.
	 * Some devices are put into pairing mode (which sets this bit).
	 *
	 * Note: Many ANT+ sport devices do not set this bit (eg. HRM strap).
	 *
	 * See ANT+ docs.
	 */
	private static final boolean PWR_PAIRING_FLAG = false;
	/*
	 * Should match device transmission id (0-255). Special rules
	 * apply for shared channels. See ANT+ protocol.
	 *
	 * 0: wildcard, matches any value (slave only)
	 */
	private static final int PWR_TRANSMISSION_TYPE = 0;
	/*
	 * device type for ANT+ heart rate monitor
	 */
	private static final int PWR_DEVICE_TYPE = 11;
	/*
	 * You should make a note of the device id and use it in preference to the wild card
	 * to pair to a specific device.
	 *
	 * 0: wild card, matches all device ids
	 * any other number: match specific device id
	 */
	private static final int PWR_DEVICE_ID = 0;

	public static void main( String[] args ) throws InterruptedException
	{
		/*
		 * Choose driver: AndroidAntTransceiver or AntTransceiver
		 *
		 * AntTransceiver(int deviceNumber)
		 * deviceNumber : 0 ... number of usb sticks plugged in
		 * 0: first usb ant-stick
		 */
		AntTransceiver antchip = new AntTransceiver( 0 );

		// initialises node with chosen driver
		Node node = new Node( antchip );

		/* must be called before any configuration takes place */
		node.start();

		node.setLibConfig( true, false, false );

		Thread.sleep( 3000 );
//		Thread.sleep( 10000 );
		Channel channel = node.getFreeChannel();

		// Arbitrary name : useful for identifying channel
		channel.setName( "C:PWR" );

		// choose slave or master type. Constructors exist to set two-way/one-way and shared/non-shared variants.
		ChannelType channelType = new SlaveChannelType();

		// use ant network key "N:ANT+"
		channel.assign( NetworkKeys.ANT_SPORT, channelType );

		// registers an instance of our callback with the channel
		channel.registerRxListener( new PWRListener(), BroadcastDataMessage.class );

		/******* start device specific configuration ******/

		channel.setId( PWR_DEVICE_ID, PWR_DEVICE_TYPE, PWR_TRANSMISSION_TYPE, PWR_PAIRING_FLAG );

		channel.setFrequency( PWR_CHANNEL_FREQ );

		channel.setPeriod( PWR_CHANNEL_PERIOD );

		/******* end device specific configuration ******/

		// timeout before we give up looking for device
		channel.setSearchTimeout( Channel.SEARCH_TIMEOUT_NEVER );

		// start listening
		channel.openInRxScanMode();

		// Listen for 600 seconds
		Thread.sleep( 600000 );

		log.info( "Closing channel..." );
		// stop listening
		channel.close();

		// resets channel configuration
		channel.unassign();

		//return the channel to the pool of available channels
		node.freeChannel( channel );

		// cleans up : gives up control of usb device etc.
		node.stop();

	}

	private static int getNibble( int[] data, int lsb, int msb )
	{
		int lsbVal = data[lsb];
		int msbVal = data[msb];
		return (msbVal << 8) | lsbVal;
	}

	private static class PWRListener implements BroadcastListener<BroadcastDataMessage>
	{
		private PowerCalculator calc1 = new PowerCalculator();
		private PowerCalculator calc2 = new PowerCalculator();

		@Override
		public void receiveMessage( BroadcastDataMessage message )
		{
			int[] data = message.getUnsignedData();
			int pageNumber = data[0];

			log.info( "pageNumber: 0x{} : {}", Integer.toHexString( pageNumber ), data );

			// 6.3 Byte Order
			// Standard ANT messages are little endian for multi -byte fields; an exception is the Crank Torque-Frequency message format,
			// which is big endian. All byte fields are explicitly defined in each message format.

			// 0x10 is power only page
			// 0x11 is torque at wheel page

			switch( pageNumber )
			{
				case 0x10: // Standard Power-Only Main Data Page (0x10)
				{
					// The standard power-only page is used to transmit power output directly in Watts. There are no conversions,
					// calibrations, or calculations required. An accumulated power field is provided for greater reliability in degraded
					// RF conditions. All fields in this message shall [MD_0010] be set as described in Table 8-1.
					// All power sensors (except CTF sensors) are required to support the standard power-only message in addition to any
					// torque- based messages. Power-Only messages shall [MD_0008] be interleaved at least once in every 9 messages, but
					// interleaving at least once in every 5 messages is preferred. Byte 2 is an optional data field that shall
					// [MD_PWR_007] be set to an invalid value when the sensor can only determine power on one pedal. Byte 2 is also a
					// newer feature and may not be supported by all displays.
					int eventCount = data[1];
					int cadence = data[3];
					int pwr = getNibble( data, 6, 7 );

					log.info( "PWR: {}, Cadence: {}, count: {}", pwr, cadence, eventCount );
					break;
				}
				case 0x11: // Standard Wheel Torque Main Data Page (0x11)
				{
					// KICKR sends this.

					// The standard wheel torque page is used to send event timing information and torque values from a power sensor that
					// measures torque on the rear wheel. Timing is based on a 2048Hz clock and torque is transmitted in Newton meters.
					// All fields in this message shall [MD_0010] be set as described in Table 9 - 1.
					int eventCount = data[1];
					int wheelTicks = data[2];
					int cadence = data[3];
					int wheelPeriod = getNibble( data, 4, 5 );
					int accumTorque = getNibble( data, 6, 7 );

					int pwr = calc1.calculate( wheelPeriod, eventCount, accumTorque, wheelTicks );

					log.info( "PWR: {}, Cadence: {}, count: {}, wTicks: {}, wPeriod: {}, accumTorque: {}",
					          pwr,
					          cadence,
					          eventCount,
					          wheelTicks,
					          wheelPeriod,
					          accumTorque );
					break;
				}
				case 0x12: // Standard Crank Torque Main Data Page (0x12)
				{
					// Stages sends this.

					// The standard crank torque page is used to send event timing information and torque values from a power sensor that
					// measures torque at the crank. Timing is based on a 2048Hz clock and torque is transmitted in Newton meters. All
					// fields in this message shall [MD_0010] be set as described in Table 10-1.
					int eventCount = data[1];
					int crankTicks = data[2];
					int cadence = data[3]; // 0XFF if unavailable
					int crankPeriod = getNibble( data, 4, 5 );
					int accumTorque = getNibble( data, 6, 7 );

					int pwr = calc1.calculate( crankPeriod, eventCount, accumTorque, crankTicks );

					log.info( "PWR: {}, Cadence: {}, count: {}, wTicks: {}, wPeriod: {}, accumTorque: {}",
					          pwr,
					          cadence,
					          eventCount,
					          crankTicks,
					          crankPeriod,
					          accumTorque );

					break;
				}
				case 0x50:
				{
					// Required 0x50 Manufacturer’s Information Minimum: Interleave every 121 Common Data Page messages (30.25s)
					break;
				}
				case 0x51:
				{
					// Required 0x51 Product Information Minimum: Interleave every 121 Common Data Page messages (30.25s)
					break;
				}
				case 0x52:
				{
					// Optional 0x52 Battery Voltage Minimum: Interleave every 61 Common Data Page messages (15.25s)
					break;
				}
				default:
					break;
			}

			if( message instanceof DeviceInfoQueryable )
			{
				DeviceInfoQueryable deviceInfoQueryable = (DeviceInfoQueryable) message;
				ChannelId channelId = deviceInfoQueryable.getChannelId();
				log.info( "deviceID: " + channelId.getDeviceNumber() );
			}
		}

	}

}
