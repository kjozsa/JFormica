package com.ftl.beats

interface DataPage

fun unsigned(i: Int): Int = i and 0x7F

data class BatteryStatus(val data: IntArray) : DataPage {
    val level = unsigned(data[0])
    val voltage = unsigned(data[1])
    val descriptive = unsigned(data[2])

    override fun toString() = "battery level: $level, voltage: $voltage, descriptive: $descriptive)"
}

data class HeartBeat(val data: IntArray) : DataPage {
    val heartBeat = unsigned(data[7])
    override fun toString() = "heart beat: $heartBeat"
}

data class OperatingTime(val data: IntArray) : DataPage {
    val timeLSB = unsigned(data[0])
    val time = unsigned(data[1])
    val timeMSB = unsigned(data[2])
    override fun toString() = "operating time LSB: $timeLSB, $time, MSB: $timeMSB"
}

data class ManufacturerInfo(val data: IntArray) : DataPage {
    val manufacturer = unsigned(data[0])
    val serialLSB = unsigned(data[1])
    val serialMSB = unsigned(data[2])
    override fun toString() = "manufacturer: $manufacturer, serial: $serialLSB : $serialMSB"
}

data class ProductInfo(val data: IntArray) : DataPage {
    val hardware = unsigned(data[0])
    val software = unsigned(data[1])
    val model = unsigned(data[2])
    override fun toString(): String = "hardware: $hardware, software: $software, model: $model"
}
