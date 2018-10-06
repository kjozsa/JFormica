package org.cowboycoders.ant.examples.demos;

import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.ChannelId;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.TransferException;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.examples.NetworkKeys;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.DeviceInfoQueryable;
import org.cowboycoders.ant.messages.MasterChannelType;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.cowboycoders.ant.messages.nonstandard.CombinedBurst;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends / receives a burst and prints channel id from extended bytes. Requires 2 ant chips!
 *
 * @author will
 */
public class BurstDemo {

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
    private static final boolean HRM_PAIRING_FLAG = true;
    /*
     * Should match device transmission id (0-255). Special rules
     * apply for shared channels. See ANT+ protocol.
     *
     * 0: wildcard, matches any value (slave only)
     */
    private static final int HRM_TRANSMISSION_TYPE = 1;
    /*
     * device type for ANT+ heart rate monitor
     */
    private static final int HRM_DEVICE_TYPE = 120;
    private static final int MASTER_ID = 1234;
    private static final ChannelId HRM_WILDCARD_CHANNEL_ID = ChannelId.Builder.newInstance()
            .setDeviceNumber(ChannelId.WILDCARD)
            .setDeviceType(HRM_DEVICE_TYPE)
            .setTransmissonType(ChannelId.WILDCARD)
            .setPairingFlag(HRM_PAIRING_FLAG)
            .build();
    private static final ChannelId HRM_MASTER_CHANNEL_ID = ChannelId.Builder.newInstance()
            .setDeviceNumber(MASTER_ID)
            .setDeviceType(HRM_DEVICE_TYPE)
            .setTransmissonType(HRM_TRANSMISSION_TYPE)
            .setPairingFlag(HRM_PAIRING_FLAG)
            .build();
    private static final ChannelType MASTER_TYPE = new MasterChannelType();
    private static final ChannelType SLAVE_TYPE = new SlaveChannelType();

    public static void main(String[] args) throws InterruptedException, TransferException, TimeoutException {

        AntTransceiver antchip = new AntTransceiver(0);
        AntTransceiver antchip2 = new AntTransceiver(1);

        Node node = new Node(antchip);
        Node node2 = new Node(antchip2);



        /* must be called before any configuration takes place */
        node.start();
        node2.start();

        /* sends reset request : resets channels to default state */
        node.reset();

        node.setLibConfig(true, false, false);

        final Channel master = node2.getFreeChannel();

        setupHrmChannel(master, MASTER_TYPE, "master", HRM_MASTER_CHANNEL_ID);

        Channel slave = node.getFreeChannel();

        setupHrmChannel(slave, SLAVE_TYPE, "slave", HRM_WILDCARD_CHANNEL_ID);

        slave.registerRxListener(new Listener(master), BroadcastDataMessage.class);

        slave.registerBurstListener(new BurstListener());

        master.open();

        slave.open();

        Thread.sleep(2000);

        node2.freeChannel(master);
        node.freeChannel(slave);

        node2.stop();
        node.stop();

    }

    public static void setupHrmChannel(Channel channel, ChannelType type, String name, ChannelId id) {
        // Arbitrary name : useful for identifying channel
        channel.setName(name);

        // use ant network key "N:ANT+"
        channel.assign(NetworkKeys.ANT_SPORT, type);

        /******* start device specific configuration ******/

        channel.setId(id);

        channel.setFrequency(HRM_CHANNEL_FREQ);

        channel.setPeriod(HRM_CHANNEL_PERIOD);

        /******* end device specific configuration ******/

        if (type instanceof SlaveChannelType) {
            // timeout before we give up looking for device
            channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);
        }

    }

    private static class Listener implements BroadcastListener<BroadcastDataMessage> {

        boolean doOnce = true;
        boolean printed = false;
        private Channel master;

        Listener(Channel master) {
            this.master = master;
        }

        @Override
        public void receiveMessage(BroadcastDataMessage message) {
            if (doOnce) {
                doOnce = false;
                new Thread() {
                    public void run() {
                        burst();
                    }
                }.start();
            }
            if (message instanceof DeviceInfoQueryable) {
                if (!printed) {
                    printed = true;
                    DeviceInfoQueryable d = (DeviceInfoQueryable) message;
                    System.out.println();
                    System.out.println("Ex. id:" + d.getDeviceNumber());
                    System.out.println("Ex. tran:" + d.getTransmissionType());
                    System.out.println("Ex. type:" + (d.getDeviceType()));
                    System.out.println();
                }

            }
        }

        public void burst() {
            try {
                // any left other bytes will be filled with zeros
                master.sendBurst(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, (byte) 0xff},
                        5L,
                        TimeUnit.SECONDS);
                System.out.println("sent burst ..");
            } catch (TransferException | InterruptedException | TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private static class BurstListener implements BroadcastListener<CombinedBurst> {

        @Override
        public void receiveMessage(CombinedBurst message) {
            if (message.isComplete()) {
                System.out.println("received burst ..");
                for (byte b : message.getData()) {
                    System.out.printf("%2x:", b);
                }
                System.out.println();

            } else {
                System.out.println("incomplete burst");
            }

        }

    }
}
