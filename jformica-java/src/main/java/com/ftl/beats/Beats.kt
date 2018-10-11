package com.ftl.beats

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.cowboycoders.ant.Channel
import org.cowboycoders.ant.Node
import org.cowboycoders.ant.examples.NetworkKeys
import org.cowboycoders.ant.examples.Utils
import org.cowboycoders.ant.interfaces.AntTransceiver
import org.cowboycoders.ant.messages.SlaveChannelType
import org.cowboycoders.ant.messages.data.BroadcastDataMessage
import org.slf4j.LoggerFactory

/**
 * Garmin HRM1G
 *
 * manufacturer: 2, serial: 13 : 24
 * hardware: 3, software: 127, model: 3
 * operating time LSB: 1, 35, MSB: 11
 */
class Beats {
    val logger = LoggerFactory.getLogger(javaClass)
    val antchip = AntTransceiver(0)
    val node = Node(antchip)
    val channel: Channel

    init {
        node.start()
        node.setLibConfig(true, false, false)
        node.reset()

        channel = node.freeChannel
        channel.name = "C:HRM"
        channel.assign(NetworkKeys.ANT_SPORT, SlaveChannelType())
        channel.registerRxListener(Listener(), BroadcastDataMessage::class.java)
        channel.setId(HRM_DEVICE_ID, HRM_DEVICE_TYPE, HRM_TRANSMISSION_TYPE, HRM_PAIRING_FLAG)
        channel.setFrequency(HRM_CHANNEL_FREQ)
        channel.setPeriod(HRM_CHANNEL_PERIOD_MIN)
        channel.setSearchTimeout(10)
    }

    suspend fun start() {
        try {
//            channel.openInRxScanMode()
            channel.open()
            delay(60_000)

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

}

fun main(args: Array<String>) {
    val stick = Beats()
    GlobalScope.launch {
        stick.start()
    }
}
