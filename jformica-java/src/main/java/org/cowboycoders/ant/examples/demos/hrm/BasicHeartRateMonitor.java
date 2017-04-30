package org.cowboycoders.ant.examples.demos.hrm;

import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.examples.NetworkKeys;
import org.cowboycoders.ant.examples.Utils;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BasicHeartRateMonitor
{
	private static final Logger log = LoggerFactory.getLogger( BasicHeartRateMonitor.class );

	/*
	 * See ANT+ data sheet for explanation
	 */
	private static final int HRM_CHANNEL_PERIOD = 8070;
	/*
	 * See ANT+ data sheet for explanation
	 */
	private static final int HRM_CHANNEL_FREQ = 57;
	/*
	 * This should match the device you are connecting with.
	 * Some devices are put into pairing mode (which sets this bit).
	 *
	 * Note: Many ANT+ sport devices do not set this bit (eg. HRM strap).
	 *
	 * See ANT+ docs.
	 */
	private static final boolean HRM_PAIRING_FLAG = false;
	/*
	 * Should match device transmission id (0-255). Special rules
	 * apply for shared channels. See ANT+ protocol.
	 *
	 * 0: wildcard, matches any value (slave only)
	 */
	private static final int HRM_TRANSMISSION_TYPE = 0;
	/*
	 * device type for ANT+ heart rate monitor
	 */
	private static final int HRM_DEVICE_TYPE = 120;
	/*
	 * You should make a note of the device id and use it in preference to the wild card
	 * to pair to a specific device.
	 *
	 * 0: wild card, matches all device ids
	 * any other number: match specific device id
	 */
	private static final int HRM_DEVICE_ID = 0;

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

		/* sends reset request : resets channels to default state */
		node.reset();

		Channel channel = node.getFreeChannel();

		// Arbitrary name : useful for identifying channel
		channel.setName( "C:HRM" );

		// choose slave or master type. Constructors exist to set two-way/one-way and shared/non-shared variants.
		ChannelType channelType = new SlaveChannelType();

		// use ant network key "N:ANT+"
		channel.assign( NetworkKeys.ANT_SPORT, channelType );

		// registers an instance of our callback with the channel
		channel.registerRxListener( new HRMListener(), BroadcastDataMessage.class );

		/******* start device specific configuration ******/

		channel.setId( HRM_DEVICE_ID, HRM_DEVICE_TYPE, HRM_TRANSMISSION_TYPE, HRM_PAIRING_FLAG );
		channel.setFrequency( HRM_CHANNEL_FREQ );
		channel.setPeriod( HRM_CHANNEL_PERIOD );

		/******* end device specific configuration ******/

		// timeout before we give up looking for device
		channel.setSearchTimeout( Channel.SEARCH_TIMEOUT_NEVER );

		// start listening
		channel.open();

		// Listen for 60 seconds
		Thread.sleep( 20000 );
		log.info( "End of sleep, closing channel..." );

		// stop listening
		channel.close();

		// optional : demo requesting of channel configuration. If device connected
		// this will reflect actual device id, transmission type etc. This info will allow
		// you to only connect to this device in the future.
		Utils.printChannelConfig( channel );

		// resets channel configuration
		channel.unassign();

		//return the channel to the pool of available channels
		node.freeChannel( channel );

		// cleans up : gives up control of usb device etc.
		node.stop();

	}

	static class HRMListener implements BroadcastListener<BroadcastDataMessage>
	{
		static final int flagAllOff = 0;  //         000...00000000 (empty mask)
		static final int flagbit1 = 1;    // 2^^0    000...00000001
		static final int flagbit2 = 2;    // 2^^1    000...00000010
		static final int flagbit3 = 4;    // 2^^2    000...00000100
		static final int flagbit4 = 8;    // 2^^3    000...00001000
		static final int flagbit5 = 16;   // 2^^4    000...00010000
		static final int flagbit6 = 32;   // 2^^5    000...00100000
		static final int flagbit7 = 64;   // 2^^6    000...01000000
		static final int flagbit8 = 128;  // 2^^7    000...10000000

		private Set<Integer> pagesSeen = new HashSet<>();

		@Override
		public void receiveMessage( BroadcastDataMessage message )
		{
			/*
			 * getData() returns the 8 byte payload. The current heart rate
			 * is contained in the last byte.
			 *
			 * Note: remember the lack of unsigned bytes in java, so unsigned values
			 * should be converted to ints for any arithmetic / display - getUnsignedData()
			 * is a utility method to do this.
			 */
			log.info( "message: {}", message.getClass().getSimpleName() );
			int[] unsignedData = message.getUnsignedData();
			String s = intToString( unsignedData[0], 4 );
			log.info( "s: {}", s );

			int pageNumber = unsignedData[0] & 0x7F;
			boolean added = pagesSeen.add( pageNumber );
			if( added )
			{
				log.info( "#### New Page: {}", pageNumber );
			}
			if( pageNumber == 2 )
			{
				int serialNumLSB = unsignedData[2];
				int serialNumMSB = unsignedData[3];

				log.info( "{} {} : {} {}", serialNumMSB, serialNumLSB, intToString( serialNumMSB, 4 ), intToString( serialNumLSB, 4 ) );

				serialNumLSB = serialNumMSB << 8;

				int serialNum = 0;
				serialNum = serialNumMSB | serialNumLSB;
				log.info( "serialNum: {} : {}", serialNum, intToString( serialNum, 4 ) );
			}
//			boolean pageChange = ((unsignedData[0] & 0x80) != 0);
//			log.info( "Data Page: {}", pageNumber );

			log.info( "Heart rate: {} : {}", unsignedData[7], Arrays.toString(unsignedData) );
		}

		public static String intToString(int number, int groupSize) {
			StringBuilder result = new StringBuilder();

			for(int i = 7; i >= 0 ; i--) {
				int mask = 1 << i;
				result.append((number & mask) != 0 ? "1" : "0");

				if (i % groupSize == 0)
					result.append(" ");
			}
			result.replace(result.length() - 1, result.length(), "");

			return result.toString();
		}
	}

}
