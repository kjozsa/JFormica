package com.ftl.beats

import org.cowboycoders.ant.events.BroadcastListener
import org.cowboycoders.ant.messages.data.BroadcastDataMessage
import org.slf4j.LoggerFactory

class Listener : BroadcastListener<BroadcastDataMessage> {
    val logger = LoggerFactory.getLogger(javaClass)

    override fun receiveMessage(message: BroadcastDataMessage?) {
        if (message == null || message.unsignedData == null) return

        val rawData = message.unsignedData
        val pageNumber = rawData[0] and 0x7F

        val data = when (pageNumber) {
            1 -> OperatingTime(pageNumber, rawData)
            2 -> ManufacturerInfo(pageNumber, rawData)
            3 -> ProductInfo(pageNumber, rawData)
            4 -> HeartBeat(pageNumber, rawData)
            7 -> BatteryStatus(pageNumber, rawData)
            else -> throw NotImplementedError();
        }

        when {
            data.pageNumber != 4 -> logger.info(data.toString())
            else -> logger.debug(data.toString())
        }
    }
}
