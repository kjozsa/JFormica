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

import org.cowboycoders.ant.defines.AntDefine;
import org.cowboycoders.ant.messages.nonstandard.CombinedBurst;
import org.cowboycoders.ant.utils.BurstMessageSequenceGenerator;
import org.cowboycoders.ant.utils.ByteUtils;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CombinedBurstTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testRecombine() {
        int LENGTH = 100;
        byte[] data = new byte[LENGTH + 1];
        //encode length in first byte
        data[0] = (byte) LENGTH;
        for (int i = 1; i < data.length; i++) {
            data[i] = (byte) i;
        }
        List<BurstDataMessage> messages = sendBurst(data);
        ;
        CombinedBurst.Builder builder = new CombinedBurst.Builder();
        CombinedBurst burst;

        // test reuseable
        for (BurstDataMessage m : messages) {
            if ((burst = builder.addMessage(m)) != null) {
                assertEquals(true, burst.isComplete());
                assertEquals(false, burst.getStatusFlags().contains(CombinedBurst.StatusFlag.ERROR_SEQUENCE_INVALID));
                assertEquals(false, burst.isExtended());
                Byte[] received = Arrays.copyOfRange(burst.getData(), 1, ByteUtils.unsignedByteToInt(data[0]) + 1);
                int count = 1;
                for (byte b : received) {
                    assertEquals(b, data[count]);
                    count++;
                }
            }
        }

        // test reuseable
        for (BurstDataMessage m : messages) {
            if ((burst = builder.addMessage(m)) != null) {
                assertEquals(true, burst.isComplete());
                assertEquals(false, burst.getStatusFlags().contains(CombinedBurst.StatusFlag.ERROR_SEQUENCE_INVALID));
                assertEquals(false, burst.isExtended());
                Byte[] received = Arrays.copyOfRange(burst.getData(), 1, ByteUtils.unsignedByteToInt(data[0]) + 1);
                int count = 1;
                for (byte b : received) {
                    assertEquals(b, data[count]);
                    count++;
                }
            }
        }

    }

    public List<BurstDataMessage> sendBurst(byte[] data) {
        final List<byte[]> list = ByteUtils.splitByteArray(data, AntDefine.ANT_STANDARD_DATA_PAYLOAD_SIZE);
        final BurstMessageSequenceGenerator generator = new BurstMessageSequenceGenerator();
        final List<BurstDataMessage> sentMessages = new ArrayList<>();

        // handle all but last
        for (int i = 0; i < list.size() - 1; i++) {
            BurstDataMessage msg = new BurstDataMessage();
            msg.setData(list.get(i));
            int next = generator.next();
            msg.setSequenceNumber(next);
            sentMessages.add(msg);
        }

        BurstDataMessage msg = new BurstDataMessage();
        msg.setData(list.get(list.size() - 1));
        int next = generator.finish();
        msg.setSequenceNumber(next);
        sentMessages.add(msg);

        return sentMessages;
    }

    @Test
    public void testExtended() {
        int LENGTH = 100;
        byte[] data = new byte[LENGTH + 1];
        //encode length in first byte
        data[0] = (byte) LENGTH;
        for (int i = 1; i < data.length; i++) {
            data[i] = (byte) i;
        }
        List<BurstDataMessage> messages = sendBurst(data);
        ;
        CombinedBurst.Builder builder = new CombinedBurst.Builder();
        CombinedBurst burst;

        ExtendedBurstDataMessage msg = new ExtendedBurstDataMessage();
        msg.setData(messages.get(0).getData());

        messages.set(0, msg);

        System.out.println(messages.size());
        for (BurstDataMessage m : messages) {
            if ((burst = builder.addMessage(m)) != null) {
                assertEquals(true, burst.isComplete());
                assertEquals(false, burst.getStatusFlags().contains(CombinedBurst.StatusFlag.ERROR_SEQUENCE_INVALID));
                assertEquals(true, burst.isExtended());
                Byte[] received = Arrays.copyOfRange(burst.getData(), 1, ByteUtils.unsignedByteToInt(data[0]) + 1);
                int count = 1;
                for (byte b : received) {
                    assertEquals(b, data[count]);
                    count++;
                }
            }
        }
    }

}
