package com.ftl.beats

interface DataPage {
    fun unsigned(i: Int): Int = i and 0x7F
}

open class HeartBeat(val pageNumber: Int, val data: IntArray) : DataPage {
    val heartBeat = unsigned(data[7])
    override fun toString() = "[$pageNumber] heart beat: $heartBeat"
}

class BatteryStatus(pageNumber: Int, data: IntArray) : HeartBeat(pageNumber, data) {
    val level = unsigned(data[0])
    val voltage = unsigned(data[1])

    val descriptive = unsigned(data[2])
    override fun toString() = super.toString() + " [$pageNumber] battery level: $level, voltage: $voltage, descriptive: $descriptive)"
}

class OperatingTime(pageNumber: Int, data: IntArray) : HeartBeat(pageNumber, data) {
    val timeLSB = unsigned(data[0])
    val time = unsigned(data[1])
    val timeMSB = unsigned(data[2])
    override fun toString() = super.toString() + " [$pageNumber] operating time LSB: $timeLSB, $time, MSB: $timeMSB"
}

class ManufacturerInfo(pageNumber: Int, data: IntArray) : HeartBeat(pageNumber, data) {
    val manufacturer = unsigned(data[0])
    val serialLSB = unsigned(data[1])
    val serialMSB = unsigned(data[2])
    override fun toString() = super.toString() + " [$pageNumber] manufacturer: $manufacturer, serial: $serialLSB : $serialMSB"
}

class ProductInfo(pageNumber: Int, data: IntArray) : HeartBeat(pageNumber, data) {
    val hardware = unsigned(data[0])
    val software = unsigned(data[1])
    val model = unsigned(data[2])
    override fun toString(): String = super.toString() + " [$pageNumber] hardware: $hardware, software: $software, model: $model"
}
