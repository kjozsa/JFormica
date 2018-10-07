package org.cowboycoders.ant.examples.demos.kt

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.cowboycoders.ant.Channel
import org.cowboycoders.ant.Node
import org.cowboycoders.ant.events.BroadcastListener
import org.cowboycoders.ant.examples.NetworkKeys
import org.cowboycoders.ant.examples.Utils
import org.cowboycoders.ant.interfaces.AntTransceiver
import org.cowboycoders.ant.messages.SlaveChannelType
import org.cowboycoders.ant.messages.data.BroadcastDataMessage
import org.slf4j.LoggerFactory


class Stick {
    val logger = LoggerFactory.getLogger(javaClass)
    val antchip = AntTransceiver(0)
    val node = Node(antchip)
    val channel: Channel

    init {
        node.start()
        node.reset()

        channel = node.freeChannel
        channel.name = "C:HRM"
        channel.assign(NetworkKeys.ANT_SPORT, SlaveChannelType())
        channel.registerRxListener(Listener(), BroadcastDataMessage::class.java)
        channel.setId(HRM_DEVICE_ID, HRM_DEVICE_TYPE, HRM_TRANSMISSION_TYPE, HRM_PAIRING_FLAG)
        channel.setFrequency(HRM_CHANNEL_FREQ)
        channel.setPeriod(HRM_CHANNEL_PERIOD)
        channel.setSearchTimeout(10)
    }

    suspend fun start() {
        try {
            channel.open()
            delay(10_000)

        } finally {
            logger.info("End of sleep, closing channel...")
            channel.close()
            Utils.printChannelConfig(channel)
            channel.unassign()
            node.freeChannel(channel)
            node.stop()
            logger.info("Exiting.")
        }
    }

    class Listener : BroadcastListener<BroadcastDataMessage> {
        val logger = LoggerFactory.getLogger(javaClass)

        override fun receiveMessage(message: BroadcastDataMessage?) {
            logger.warn("## received {}", message)
        }
    }
}

fun main(args: Array<String>) {
    val stick = Stick()
    GlobalScope.launch {
        stick.start()
    }
}
