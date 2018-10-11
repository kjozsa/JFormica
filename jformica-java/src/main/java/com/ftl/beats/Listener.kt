package com.ftl.beats

import org.cowboycoders.ant.events.BroadcastListener
import org.cowboycoders.ant.messages.data.BroadcastDataMessage
import org.slf4j.LoggerFactory

class Listener : BroadcastListener<BroadcastDataMessage> {
    val logger = LoggerFactory.getLogger(javaClass)
    var count = 0

    override fun receiveMessage(message: BroadcastDataMessage?) {
        if (message == null || message.unsignedData == null) return
        count += 1

        val data = message.unsignedData
        val pageNumber = data[0] and 0x7F

        val dataPage = when (pageNumber) {
            1 -> OperatingTime(data)
            2 -> ManufacturerInfo(data)
            3 -> ProductInfo(data)
            4 -> HeartBeat(data)
            7 -> BatteryStatus(data)
            else -> null
        }

        if (dataPage !is HeartBeat) {
            logger.info(dataPage.toString())
        } else {
            logger.debug(dataPage.toString())
        }
    }
}
