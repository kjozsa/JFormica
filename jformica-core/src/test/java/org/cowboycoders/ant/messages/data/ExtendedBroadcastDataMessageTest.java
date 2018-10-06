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

package org.cowboycoders.ant.messages.data;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ExtendedBroadcastDataMessageTest {

    @Test
    public void test() {
        ExtendedBroadcastDataMessage msg = new ExtendedBroadcastDataMessage();
        byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
        int chan = 5;
        msg.setData(data);
        msg.setChannelNumber(chan);

        assertArrayEquals(data, msg.getPrimitiveData());
        assertEquals(chan, (int) msg.getChannelNumber());

    }

}
