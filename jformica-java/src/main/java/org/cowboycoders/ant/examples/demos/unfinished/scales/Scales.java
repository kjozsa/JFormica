package org.cowboycoders.ant.examples.demos.unfinished.scales;

import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.examples.NetworkKeys;
import org.cowboycoders.ant.examples.Utils;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;

/**
 * Connects to ANT+ enabled scales e.g tanita (untested)
 *
 * @author will
 */
public class Scales {

    /*
     * See ANT+ data sheet for explanation
     */
    private static final int SCALES_CHANNEL_PERIOD = 8192;
    /*
     * See ANT+ data sheet for explanation
     */
    private static final int ANTPLUS_CHANNEL_FREQ = 57;
    /*
     * This should match the device you are connecting with.
     * Some devices are put into pairing mode (which sets this bit).
     *
     * Note: Many ANT+ sport devices do not set this bit (eg. HRM strap).
     *
     * See ANT+ docs.
     */
    private static final boolean SCALES_PAIRING_FLAG = false;
    /*
     * Should match device transmission id (0-255). Special rules
     * apply for shared channels. See ANT+ protocol.
     *
     * 0: wildcard, matches any value (slave only)
     */
    private static final int SCALES_TRANSMISSION_TYPE = 0;
    /*
     * device type for ANT+ heart rate monitor
     */
    private static final int SCALES_DEVICE_TYPE = 119;
    /*
     * You should make a note of the device id and use it in preference to the wild card
     * to pair to a specific device.
     *
     * 0: wild card, matches all device ids
     * any other number: match specific device id
     */
    private static final int SCALES_DEVICE_ID = 0;

    public static void main(String[] args) throws InterruptedException {

        /*
         * Choose driver: AndroidAntTransceiver or AntTransceiver
         *
         * AntTransceiver(int deviceNumber)
         * deviceNumber : 0 ... number of usb sticks plugged in
         * 0: first usb ant-stick
         */
        AntTransceiver antchip = new AntTransceiver(0);

        // initialises node with chosen driver
        Node node = new Node(antchip);


        /* must be called before any configuration takes place */
        node.start();

        /* sends reset request : resets channels to default state */
        node.reset();

        Channel channel = node.getFreeChannel();

        // Arbitrary name : useful for identifying channel
        channel.setName("C:HRM");

        // choose slave or master type. Constructors exist to set two-way/one-way and shared/non-shared variants.
        ChannelType channelType = new SlaveChannelType();

        // use ant network key "N:ANT+"
        channel.assign(NetworkKeys.ANT_SPORT, channelType);

        // registers an instance of our callback with the channel
        //channel.registerRxListener(new Listener(), DataMessage.class);

        /******* start device specific configuration ******/

        channel.setId(SCALES_DEVICE_ID, SCALES_DEVICE_TYPE, SCALES_TRANSMISSION_TYPE, SCALES_PAIRING_FLAG);

        channel.setFrequency(ANTPLUS_CHANNEL_FREQ);

        channel.setPeriod(SCALES_CHANNEL_PERIOD);

        /******* end device specific configuration ******/

        // timeout before we give up looking for device
        channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

        // start listening
        channel.open();

        // Listen for 60 seconds
        Thread.sleep(60000);

        // stop listening
        channel.close();

        // optional : demo requesting of channel configuration. If device connected
        // this will reflect actual device id, transmission type etc. This info will allow
        // you to only connect to this device in the future.
        Utils.printChannelConfig(channel);

        // resets channel configuration
        channel.unassign();

        //return the channel to the pool of available channels
        node.freeChannel(channel);

        // cleans up : gives up control of usb device etc.
        node.stop();

    }

    private static class Listener implements BroadcastListener<BroadcastDataMessage> {

        /*
         * Once an instance of this class is registered with a channel,
         * this is called every time a broadcast message is received
         * on that channel.
         *
         * (non-Javadoc)
         * @see org.cowboycoders.ant.events.BroadcastListener#receiveMessage(java.lang.Object)
         */
        @Override
        public void receiveMessage(BroadcastDataMessage message) {
            /*
             * getData() returns the 8 byte payload. The current heart rate
             * is contained in the last byte.
             *
             * Note: remember the lack of unsigned bytes in java, so unsigned values
             * should be converted to ints for any arithmetic / display - getUnsignedData()
             * is a utility method to do this.
             */
            System.out.println("Heart rate: " + message.getUnsignedData()[7]);
        }

    }

}
