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
package org.cowboycoders.ant.messages.config;

import org.cowboycoders.ant.ChannelId;
import org.cowboycoders.ant.messages.*;
import org.cowboycoders.ant.messages.Constants.DataElement;

/**
 * ChannelId message
 *
 * @author will
 */
public class ChannelIdMessage extends ChannelMessage {

    /**
     * The additional elements we are adding to channelmessage
     */
    private static DataElement[] additionalElements = new DataElement[]{
            DataElement.DEVICE_NUMBER, DataElement.DEVICE_TYPE, DataElement.TRANSMISSION_TYPE,
    };
    /**
     * companion object we delegate shared methods to
     */
    private ChannelIdCompanion companion;

    /**
     * ChannelIdMessage with all bells and whistles
     *
     * @param channelNo        the channel no to apply these settings to
     * @param deviceNumber     as given in ant spec
     * @param deviceType       as given in ant spec
     * @param transmissionType as give in ant spec
     * @param setPairingFlag   true to set pairing bit, else false
     */
    public ChannelIdMessage(int channelNo, int deviceNumber, int deviceType, int transmissionType, boolean setPairingFlag) {
        super(MessageId.CHANNEL, channelNo, additionalElements);
        this.companion = new ChannelIdCompanion(this);
        try {
            setDeviceNumber(deviceNumber);
            setDeviceType(deviceType);
            setTransmissionType(transmissionType);
            setPairingFlag(setPairingFlag);
        } catch (ValidationException e) {
            throw new FatalMessageException("Error setting values", e);
        }
    }

    public int getDeviceNumber() {
        return companion.getDeviceNumber();
    }

    /**
     * @param deviceNumber to set
     * @throws ValidationException if out of limit
     */
    private void setDeviceNumber(int deviceNumber) throws ValidationException {
        companion.setDeviceNumber(deviceNumber);
    }

    public int getTransmissionType() {
        return companion.getTransmissionType();
    }

    /**
     * @param transmissionType to set
     * @throws ValidationException if out of limit
     */
    private void setTransmissionType(int transmissionType) throws ValidationException {
        companion.setTransmissionType(transmissionType);
    }

    public boolean isPairingFlagSet() {
        return companion.isPairingFlagSet();
    }

    public int getDeviceType() {
        return companion.getDeviceType();
    }

    /**
     * @param deviceType to set
     * @throws ValidationException if out of limit
     */
    private void setDeviceType(int deviceType) throws ValidationException {
        companion.setDeviceType(deviceType);
    }

    public ChannelId getChannelId() {
        return ChannelId.Builder.newInstance()
                .setDeviceNumber(getDeviceNumber())
                .setDeviceType(getDeviceType())
                .setTransmissonType(getTransmissionType())
                .setPairingFlag(isPairingFlagSet())
                .build();
    }

    /**
     * @param setPairingFlag to set
     */
    private void setPairingFlag(boolean setPairingFlag) {
        companion.setPairingFlag(setPairingFlag);
    }

}
