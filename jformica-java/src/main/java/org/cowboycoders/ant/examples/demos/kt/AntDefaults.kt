package org.cowboycoders.ant.examples.demos.kt

/**
 * See ANT+ data sheet for explanation
 */
const val HRM_CHANNEL_PERIOD = 8070

/**
 * See ANT+ data sheet for explanation
 */
const val HRM_CHANNEL_FREQ = 57

/**
 * This should match the device you are connecting with.
 * Some devices are put into pairing mode (which sets this bit).
 *
 * Note: Many ANT+ sport devices do not set this bit (eg. HRM strap).
 *
 * See ANT+ docs.
 */
const val HRM_PAIRING_FLAG = false

/**
 * Should match device transmission id (0-255). Special rules
 * apply for shared channels. See ANT+ protocol.
 *
 * 0: wildcard, matches any value (slave only)
 */
const val HRM_TRANSMISSION_TYPE = 0

/**
 * device type for ANT+ heart rate monitor
 */
const val HRM_DEVICE_TYPE = 120

/**
 * You should make a note of the device id and use it in preference to the wild card
 * to pair to a specific device.
 *
 * 0: wild card, matches all device ids
 * any other number: match specific device id
 */
const val HRM_DEVICE_ID = 0
