/**
 * Copyright (c) 2013, Will Szumski
 * <p>
 * This file is part of formicidae.
 * <p>
 * formicidae is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * formicidae is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with formicidae.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cowboycoders.ant;

import org.cowboycoders.ant.AntLogger.Direction;
import org.cowboycoders.ant.events.*;
import org.cowboycoders.ant.interfaces.AntChipInterface;
import org.cowboycoders.ant.interfaces.AntStatusUpdate;
import org.cowboycoders.ant.messages.ChannelMessage;
import org.cowboycoders.ant.messages.MessageId;
import org.cowboycoders.ant.messages.MessageMetaWrapper;
import org.cowboycoders.ant.messages.StandardMessage;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage;
import org.cowboycoders.ant.messages.commands.ResetMessage;
import org.cowboycoders.ant.messages.config.EnableExtendedMessagesMessage;
import org.cowboycoders.ant.messages.config.LibConfigMessage;
import org.cowboycoders.ant.messages.config.NetworkKeyMessage;
import org.cowboycoders.ant.messages.config.TxPowerMessage;
import org.cowboycoders.ant.messages.notifications.StartupMessage;
import org.cowboycoders.ant.messages.responses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

public class Node {
    private static final Logger log = LoggerFactory.getLogger(Node.class);
    //	private Set<AntLogger> antLoggers = Collections.newSetFromMap( new WeakHashMap<AntLogger, Boolean>() );
    private volatile boolean running = false;
    private EventMachine evm;
    private Channel[] channels = new Channel[0];
    private Network[] networks = new Network[0];
    private CapabilityResponse capabilities;
    private AntChipInterface antChipInterface;
    private final MessageSender nodeSender = new MessageSender() {

        @Override
        public List<MessageMetaWrapper<? extends StandardMessage>> send(StandardMessage msg) {
            MessageMetaWrapper<StandardMessage> sentMeta = Node.this.send(msg);
            List<MessageMetaWrapper<? extends StandardMessage>> rtn = new ArrayList<>(1);
            rtn.add(sentMeta);
            return rtn;
        }

    };
    private BroadcastMessenger<AntStatusUpdate> mStatusMessenger = new BroadcastMessenger<>();
    /**
     * flags that we did the reset, as oppose to someone else (externally)
     */
    private boolean weReset;
    private NetworkListener networkListener = new NetworkListenerImpl();
    /**
     * We wrap listeners to look for message of specific class - this is a map
     * from the original to the new one
     */
    private Map<Object, BroadcastListener<StandardMessage>> mAdapterListenerMap = new HashMap<>();

    public Node(AntChipInterface antchip) {
        antChipInterface = antchip;
        evm = new EventMachine(antchip);
    }

    /**
     * Returns the antchip. Note: you should never bypass the node and send
     * messages using its send function.
     *
     * @return
     */
    public AntChipInterface getAntChip() {
        return antChipInterface;
    }

    /**
     * Listener for status updates from the ant chip
     *
     * @param listener
     */
    public synchronized void registerStatusListener(BroadcastListener<AntStatusUpdate> listener) {
        mStatusMessenger.addBroadcastListener(listener);
    }

    /**
     * Remove listener for status updates from the ant chip
     *
     * @param listener
     */
    public synchronized void removeStatusListener(BroadcastListener<AntStatusUpdate> listener) {
        mStatusMessenger.removeBroadcastListener(listener);
    }

    /**
     * All capabilities
     *
     * @return capabilities of antnode
     */
    public synchronized List<Capability> getCapabiltites() {
        if (capabilities == null) {
            return null;
        }
        List<Capability> rtn = capabilities.getCapabilitiesList(CapabilityCategory.STANDARD);
        rtn.addAll(capabilities.getCapabilitiesList(CapabilityCategory.ADVANCED));
        rtn.addAll(capabilities.getCapabilitiesList(CapabilityCategory.ADVANCED2));
        rtn.addAll(capabilities.getCapabilitiesList(CapabilityCategory.ADVANCED3));
        return rtn;
    }

    public synchronized void start() throws AntError {
        if (running) {
            return;// throw new AntError("already started");
        }
        evm.start();
        antChipInterface.start();
        antChipInterface.registerStatusMessenger(mStatusMessenger);
        evm.registerRxListener(new MessageListener());
        init();

        running = true;
        log.info("Node started.");
    }

    public synchronized Channel getFreeChannel() {

        for (Channel c : channels) {
            if (c.isFree()) {
                c.setFree(false);
                return c;
            }
        }

        return null;
    }

    /**
     * should be called to free and return channel to pool. Channel should then
     * be nulled or reassigned
     *
     * @param channel
     */
    public synchronized void freeChannel(Channel channel) {
        // need to hold synchronisation in case someone is
        // calling getFreeChannel)
        channel.setFree(true);
    }

    /**
     * @param network must be below that given by getMaxNetworks()
     * @param key     to set
     * @deprecated doesn't ensure network keys won't changed whilst in use
     * <p/>
     * Do not set key until after node started
     */
    @Deprecated
    public synchronized void setNetworkKey(int network, NetworkKey key) {
        this.setNetworkKey(network, key, true);
    }

    /**
     * Gets Network object associated with this key. If key not associated with
     * a network it will attempt to find a network not in use an create a new
     * association.
     *
     * @param key
     * @return
     */
    public synchronized Network getNetworkForKey(NetworkKey key) {
        int count = 0;
        Integer freeIndex = null;

        for (Network network : networks) {
            NetworkKey currentNetworkKey = network.getNetworkKey();

            // if network not associated with a key already
            if (currentNetworkKey == null) {
                if (freeIndex == null) {
                    freeIndex = count;
                }
            }
            // if key already mapped
            else if (currentNetworkKey.equals(key)) {
                return new NetworkHandle(network);
            }

            count++;
        }

        if (freeIndex == null) {
            throw new NetworkAllocationException("No free networks available");
        }
        setNetworkKey(freeIndex, key, true);
        return new NetworkHandle(networks[freeIndex]);
    }

    /**
     * @return max number of networks this node supports
     */
    public synchronized int getMaxNetworks() {
        return capabilities.getMaxNetworks();
    }

    /**
     * @return max number of channels this node supports
     */
    public synchronized int getMaxChannels() {
        return capabilities.getMaxChannels();
    }

    public StandardMessage sendAndWaitWithAdapter(WaitAdapter waitAdapter,
                                                  StandardMessage msg,
                                                  LockExchangeContainer lockContainer) throws InterruptedException, TimeoutException {
        return sendAndWaitWithAdapter(waitAdapter, msg, lockContainer, nodeSender, null);
    }

    public StandardMessage sendAndWaitWithAdapter(WaitAdapter waitAdapter,
                                                  StandardMessage msg,
                                                  LockExchangeContainer lockContainer,
                                                  MessageSender sender,
                                                  Receipt receipt) throws InterruptedException, TimeoutException {
        Callable<MessageMetaWrapper<? extends StandardMessage>> waitTask = new ThreadedWait(waitAdapter);
        ExecutorService threadPool = SharedThreadPool.getThreadPool();
        Future<MessageMetaWrapper<? extends StandardMessage>> future = threadPool.submit(waitTask);
        MessageMetaWrapper<? extends StandardMessage> receivedMeta = null;
        List<MessageMetaWrapper<? extends StandardMessage>> sentMeta = null;

        sender = sender == null ? nodeSender : sender;

        // Thread.sleep(100);
        // now we are waiting for a response send message
        if (lockContainer != null) {
            try {
                LockExchanger exchanger = new LockExchanger(lockContainer, 5L, TimeUnit.SECONDS);
                Future<Lock> lockFuture = threadPool.submit(exchanger);
                Lock lock = lockFuture.get();
                try {
                    lock.lock();
                    // Thread.sleep(100
                    sentMeta = sender.send(msg);
                } finally {
                    lock.unlock();
                }

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException) {
                    throw new InterruptedException("interrupted waiting for lock exchange");
                }
                if (cause instanceof TimeoutException) {
                    throw new TimeoutException("Timeout waiting for lock exchange for MsgId: " +
                            String.format("0x%02x", msg.getId().getMessageID() & 0xFF));
                }
                throw new RuntimeException(cause);
            }
        } else {
            sentMeta = sender.send(msg);
        }

        try {
            receivedMeta = future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof InterruptedException) {
                throw new InterruptedException("interrupted waiting for message");
            }
            if (cause instanceof TimeoutException) {
                throw new TimeoutException("Timeout waiting for lock exchange for MsgId: " +
                        String.format("0x%02x", msg.getId().getMessageID() & 0xFF));
            }

            throw new RuntimeException(cause);
        }

        // message is in charge of updating sent messages
        if (receipt != null) {
            receipt.addReceived(receivedMeta);
            receipt.addSent(sentMeta);
        }

        return receivedMeta.unwrap();
    }

    public synchronized <V extends StandardMessage> void registerRxListener(final BroadcastListener<V> listener, final Class<V> clazz) {

        BroadcastListener<StandardMessage> adapter = new BroadcastListener<StandardMessage>() {

            @Override
            public void receiveMessage(StandardMessage message) {
                if (clazz.isInstance(message)) {
                    listener.receiveMessage(clazz.cast(message));
                }

            }

        };

        mAdapterListenerMap.put(listener, adapter);
        evm.registerRxListener(adapter);

    }

    public synchronized <V extends StandardMessage> void removeRxListener(final BroadcastListener<V> listener) {

        BroadcastListener<StandardMessage> adapter = mAdapterListenerMap.get(listener);
        if (adapter != null) {
            evm.removeRxListener(adapter);
        } else {
            log.warn("removeRxListener: ignoring unknown listener");
        }

    }

    public StandardMessage sendAndWaitForMessage(final StandardMessage msg,
                                                 final MessageCondition condition,
                                                 final Long timeout,
                                                 final TimeUnit timeoutUnit,
                                                 final MessageSender sender,
                                                 final Receipt receipt) throws InterruptedException, TimeoutException {
        return this.sendAndWaitForMessage(msg, condition, timeout, timeoutUnit, sender, receipt, null);

    }

    /**
     * @param msg                 the message to send or null if sent from a
     *                            {@link MessageSender}
     * @param condition           indicates that a receive message is the one you were waiting
     *                            for
     * @param timeout             combined with {@code timeoutUnit} to determine maximum period
     *                            of time to wait for a response satisfying {@code condition}
     * @param timeoutUnit         unit of time used for timeouts
     * @param sender              used to customise the send method or send multiple messages.
     *                            Can be null.
     * @param receipt             stamped with meta information
     * @param errorCheckCondition {@link MessageCondition#test(StandardMessage)} should return
     *                            true if message should checked for against list of error
     *                            codes. A null value will match all messages.
     * @return message satisfying {@code condition}
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public StandardMessage sendAndWaitForMessage(final StandardMessage msg,
                                                 final MessageCondition condition,
                                                 final Long timeout,
                                                 final TimeUnit timeoutUnit,
                                                 final MessageSender sender,
                                                 final Receipt receipt,
                                                 final MessageCondition errorCheckCondition) throws InterruptedException,
            TimeoutException {
        final LockExchangeContainer lockContainer = new LockExchangeContainer();
        final TransmissionErrorCondition errorCondition = new TransmissionErrorCondition(msg);

        final MessageCondition conditionWithChecks = new MessageCondition() {

            @Override
            public boolean test(StandardMessage msg) {
                // null will match all (should we allow this?)
                if (condition == null) {
                    return true;
                }
                if (condition.test(msg)) {
                    return true;
                }

                // this allows addition check to made e.g channel number matches
                if (errorCheckCondition == null || errorCheckCondition.test(msg)) {
                    // throws an exception on failure
                    errorCondition.test(msg);
                }

                return false;
            }

        };

        WaitAdapter responseAdapter = new WaitAdapter() {

            @Override
            public MessageMetaWrapper<StandardMessage> execute() throws InterruptedException, TimeoutException {
                MessageMetaWrapper<StandardMessage> response = evm.waitForCondition(conditionWithChecks,
                        timeout,
                        timeoutUnit,
                        lockContainer);
                return response;
            }

        };

        return sendAndWaitWithAdapter(responseAdapter, msg, lockContainer, sender, receipt);

    }

    public synchronized void reset() {
        weReset = true;

        StandardMessage resetMsg = new ResetMessage();
        log.info("Sending Reset() message to device...");
        send(resetMsg);

        try {
            log.info("Waiting for 500ms to give device time to process the reset() - Ant Spec 9.5.4.1");
            Thread.sleep(550);
        } catch (InterruptedException e) {
        }
    }

    public synchronized MessageMetaWrapper<StandardMessage> send(StandardMessage msg) {
        log.debug("Sending: {} " + msg.toString() + " to chip", String.format("0x%02x", msg.getId().getMessageID()));
        byte[] encodedMsg = msg.encode();
        antChipInterface.send(encodedMsg);
        // now that we have sent, inform the loggers
        logMessage(Direction.SENT, msg);
        return new MessageMetaWrapper<>(msg);
    }

    public synchronized void stop() {
        if (!running) {
            return; // throw new AntError("already stopped");
        }
        evm.stop();
        antChipInterface.stop();
        running = false;
    }

    public synchronized boolean isRunning() {
        // sync with ant chip
        if (!this.antChipInterface.isRunning()) {
            ;
        }
        {
            stop();
        }
        return running;
    }

    /**
     * Registers an event listener. To remove user {@link Node#removeRxListener(BroadcastListener));
     *
     * @param handler event handler
     */
    public void registerEventHandler(NodeEventHandler handler) {
        this.registerRxListener(handler, Response.class);
    }

    /**
     * Waits for response NO_ERROR for a maximum of 1 second
     *
     * @param msg to send
     * @throws AntError     can be caused by TimeoutException, InterruptedException or
     *                      thrown outright as a response to an error
     * @throws ChannelError on error condition sending a {@link ChannelMessage}
     */
    public void sendAndWaitForResponseNoError(StandardMessage msg) throws AntError {
        MessageCondition condition = MessageConditionFactory.newResponseCondition(msg.getId(), ResponseCode.RESPONSE_NO_ERROR);
        try {
            sendAndWaitForMessage(msg, condition, 1L, TimeUnit.SECONDS, null, null);
        } catch (InterruptedException | TimeoutException e) {
            handleTimeOutException(e, msg);
        }
    }

    /**
     * Sets chip wide transmit power See table 9.4.3 in ANT protocol as this is
     * chip dependent. Maximum powerLevel 4, minimum 0. Some chips only support
     * up to level 3.
     *
     * @param powerLevel newPowerLevel
     */
    public void setTransmitPower(int powerLevel) {
        StandardMessage msg = new TxPowerMessage(powerLevel);
        sendAndWaitForResponseNoError(msg);
    }

    /**
     * Request extra info in extended message bytes. Not all chips support all
     * of the options (especially rssi) if any.
     *
     * @param enableChannelId
     * @param enableRssi
     * @param enableTimestamps
     */
    public void setLibConfig(boolean enableChannelId, boolean enableRssi, boolean enableTimestamps) {
        StandardMessage msg = new LibConfigMessage(enableChannelId, enableRssi, enableTimestamps);
        sendAndWaitForResponseNoError(msg);

    }

    /**
     * Include channelId with data messages (legacy) na duspport chip dependent.
     *
     * @param enable
     */
    public void enableExtendedMessages(boolean enable) {
        StandardMessage msg = new EnableExtendedMessagesMessage(enable);
        sendAndWaitForResponseNoError(msg);
    }

    /**
     * @return the evm
     */
    protected synchronized EventMachine getEvm() {
        return evm;
    }

    private CapabilityResponse getCapabilityResponse(final int maxRetries) throws InterruptedException, TimeoutException {
        StandardMessage capabilitiesMessage = new ChannelRequestMessage(0, ChannelRequestMessage.Request.CAPABILITIES);
        StandardMessage capabilitiesResponse = null;
        int retries = 0;
        while (capabilitiesResponse == null) {
            MessageCondition condition = MessageConditionFactory.newInstanceOfCondition(CapabilityResponse.class);
            try {
                capabilitiesResponse = sendAndWaitForMessage(capabilitiesMessage, condition, 10L, TimeUnit.SECONDS, null, null);
            } catch (TimeoutException e) {
                e.printStackTrace();
                log.warn("getCapabilityResponse : timeout");
                retries++;
                if (retries >= maxRetries) {
                    throw e;
                }
            }
        }

        return (CapabilityResponse) capabilitiesResponse;
    }

    private void init() {
        log.trace("entering init");
        reset();
        try {
            // android interface automatically requests ant version response.
            // Can't wait for it here, as it may have already been fired.
            // so we use retries until we get a response (which was not the case
            // if sent before version was requested)
            capabilities = getCapabilityResponse(5);
        } catch (InterruptedException e) {
            throw new AntError(e);
        } catch (TimeoutException e) {
            throw new AntError(e);
        }

        networks = new Network[capabilities.getMaxNetworks()];
        for (int i = 0; i < networks.length; i++) {
            freeNetwork(i);
        }

        channels = new Channel[getMaxChannels()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Channel(this, i);
        }

        log.trace("exiting init");
    }

    private void freeNetwork(int i) {
        setNetworkKey(i, null, false);
    }

    /**
     * @param network to assign this key to
     * @param key     to assign
     * @param send    if true, send to ant chip
     */
    private synchronized void setNetworkKey(int network, NetworkKey key, boolean send) {
        networks[network] = new Network(network, key, networkListener);
        if (send) {
            StandardMessage msg = new NetworkKeyMessage(network, key.getKey());
            MessageCondition condition = MessageConditionFactory.newResponseCondition(msg.getId(), ResponseCode.RESPONSE_NO_ERROR);
            try {
                sendAndWaitForMessage(msg, condition, 5L, TimeUnit.SECONDS, null, null);
            } catch (InterruptedException | TimeoutException e) {
                handleTimeOutException(e, msg);
            }
        }
    }

    private void logMessage(AntLogger.Direction direction, StandardMessage msg) {
        log.debug(direction.name() + " : " + String.format("0x%02x", msg.getId().getMessageID()));
    }

    private void handleTimeOutException(Exception e, StandardMessage msg) {
        if (e instanceof InterruptedException) {
            throw new AntError("Interrupted whilst waiting for message / reply on Msg: " + (msg.getId().getMessageID() & 0XFF), e);
        }
        if (e instanceof TimeoutException) {
            throw new AntError("Timeout whilst waiting for message / reply on Msg: " + (msg.getId().getMessageID() & 0XFF), e);
        }
    }

    class ThreadedWait implements Callable<MessageMetaWrapper<? extends StandardMessage>> {

        private WaitAdapter waitAdapter;

        public ThreadedWait(WaitAdapter waitAdapter) {
            this.waitAdapter = waitAdapter;
        }

        @Override
        public MessageMetaWrapper<? extends StandardMessage> call() throws InterruptedException, TimeoutException {
            return waitAdapter.execute();
        }

    }

    private class StatusListener implements BroadcastListener<AntStatusUpdate> {

        @Override
        public void receiveMessage(AntStatusUpdate message) {
        }
    }

    private class MessageListener implements BroadcastListener<StandardMessage> {
        @Override
        public void receiveMessage(StandardMessage message) {
            logMessage(Direction.RECEIVED, message);
            if (message instanceof StartupMessage) {
                if (!weReset) {
                    // we don't actually get a reset intent if we send a raw
                    // message
                    // NOTE: should we use the reset ANtInterface method?
                    log.warn("Node: external reset");
                }
                weReset = false;
            }
        }
    }

    private class NetworkListenerImpl implements NetworkListener {
        @Override
        public void onFree(Network network) {
            // null network key indicates network not mapped to key
            freeNetwork(network.getNumber());
        }

    }

    private static class TransmissionErrorCondition implements MessageCondition {

        private StandardMessage transmittedMessage;

        /**
         * Throws an {@link RumtimeException} if an error is raised trying to
         * send message
         *
         * @param msg message that is being sent
         */
        public TransmissionErrorCondition(StandardMessage msg) {
            transmittedMessage = msg;
        }

        @Override
        public boolean test(StandardMessage msg) {
            // for message senders we look at all message ids
            MessageId id = null;

            // else we look for particular id
            if (transmittedMessage != null) {
                id = transmittedMessage.getId();
            }

            // make sure it a response/event
            if (!MessageConditionFactory.newResponseCondition(id, null).test(msg)) {
                return false;
            }
            Response response = (Response) msg;

            // throw an exception if response is a known error condition (this
            // will be re-thrown on waiting thread)
            ResponseExceptionFactory.getFactory().throwOnError(response.getResponseCode());

            // this condition only throw exceptions on errors (always return
            // false)
            return false;
        }

    }

}