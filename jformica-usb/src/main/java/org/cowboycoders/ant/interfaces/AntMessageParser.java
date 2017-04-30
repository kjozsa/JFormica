package org.cowboycoders.ant.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class AntMessageParser
{
	private static final Logger log = LoggerFactory.getLogger( AntMessageParser.class );
	private static final int LOOKING_FOR_SYNC = 0;
	private static final int LOOKING_FOR_LEN = 1;
	private static final int LOOKING_FOR_MSGID = 2;
	private static final int LOOKING_FOR_END_OF_MSG = 3;
	private static final int LOOKING_FOR_CHECKSUM = 4;
	private int currentState = LOOKING_FOR_SYNC;
	private byte[] dataBuf;
	private byte msgSync;
	private byte msgLen;
	private byte msgId;
	private int msgIdx;

	List<byte[]> parse( byte[] buf, int bytesRead )
	{
		List<byte[]> messages = new ArrayList<>();

		// Sync, MsgLen, MsgId, Data, CS
		//  1      1       1     N     1

		for( int i = 0; i < bytesRead; i++ )
		{
			byte b = buf[i];

			switch( currentState )
			{
				case LOOKING_FOR_SYNC:
					if( b == AntMessage.ANT_SYNC_1 || b == AntMessage.ANT_SYNC_2 )
					{
						msgSync = b;
						currentState = LOOKING_FOR_LEN;
					}
					break;
				case LOOKING_FOR_LEN:
					msgLen = b;
					dataBuf = new byte[msgLen + 2];
					dataBuf[msgIdx++] = msgLen;
					currentState = LOOKING_FOR_MSGID;
					break;
				case LOOKING_FOR_MSGID:
					msgId = b;
					dataBuf[msgIdx++] = msgId;
					currentState = LOOKING_FOR_END_OF_MSG;
					break;
				case LOOKING_FOR_END_OF_MSG:
					dataBuf[msgIdx++] = b;
					if( msgIdx == msgLen + 2 )
					{
						currentState = LOOKING_FOR_CHECKSUM;
					}
					break;
				case LOOKING_FOR_CHECKSUM:
					int checksum = b;
					byte calculatedChecksum = getChecksum( msgSync, dataBuf );
					if( calculatedChecksum == checksum )
					{
						log.debug( "Received new AntMessage, msgId: " + String.format( "0x%x", msgId ) + ", len: " + msgLen );
						byte b0 = dataBuf[0];
						byte b1 = dataBuf[1];
						messages.add( dataBuf );
					}
					else
					{
						log.warn( "Checksum mismatch. Calculated: " + calculatedChecksum + " vs Received: " + checksum );
					}
					currentState = LOOKING_FOR_SYNC;
					dataBuf = null;
					msgSync = 0;
					msgLen = 0;
					msgId = 0;
					msgIdx = 0;
					break;
			}
		}

		return messages;
	}

	private static byte getChecksum( byte syncByte, byte[] msgData )
	{
		byte checksum = 0;
		checksum ^= syncByte;
		if( msgData != null )
		{
			for( byte b : msgData )
			{
				checksum ^= b;
			}
		}
		return checksum;
	}
}
