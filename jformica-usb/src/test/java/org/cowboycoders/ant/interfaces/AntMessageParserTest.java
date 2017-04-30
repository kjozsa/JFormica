package org.cowboycoders.ant.interfaces;

import org.junit.*;

import java.util.List;

import static org.junit.Assert.*;

/**
 * User: npratt
 * Date: 1/13/14
 * Time: 21:15
 */
public class AntMessageParserTest
{
	private AntMessageParser parser;

	@Before
	public void setUp() throws Exception
	{
		parser = new AntMessageParser();
	}

	@Test
	public void testFeedRandomDataInWithoutSync() throws Exception
	{
		byte[] buf = genBuf( 1, 2, 3, 2, 1, 2, 3, 2, 1 );
		List<byte[]> msgs = parser.parse( buf, buf.length );

		assertEquals( 0, msgs.size() );
	}

	@Test
	public void testLotsOfRandomData() throws Exception
	{
		byte[] buf = genBuf( 1, 2, 3, 2, 1, 2, 3, 2, 1 );
		parser.parse( buf, buf.length );
		parser.parse( buf, buf.length );
		List<byte[]> msgs = parser.parse( buf, buf.length );

		assertEquals( 0, msgs.size() );
	}

	@Test
	public void testSyncAtStart() throws Exception
	{
		byte[] buf = genBuf( AntMessage.ANT_SYNC_1, 2, 0, 4, 2, -96 );
		List<byte[]> msgs = parser.parse( buf, buf.length );

		assertEquals( 1, msgs.size() );
		assertArrayEquals( genBuf( 2, 0, 4, 2 ), msgs.get( 0 ) );
	}

	@Test
	public void testSyncAfterGarbage() throws Exception
	{
		byte[] buf = genBuf( 1, 2, 1, 2, 12, 2, 1, 2, 1, 2, 1, 2, AntMessage.ANT_SYNC_1, 2, 0, 4, 2, -96 );
		List<byte[]> msgs = parser.parse( buf, buf.length );

		assertEquals( 1, msgs.size() );
		assertArrayEquals( genBuf( 2, 0, 4, 2 ), msgs.get( 0 ) );
	}

	@Test
	public void testMsgSplitOverMultiplePackets() throws Exception
	{
		byte[] buf = genBuf( AntMessage.ANT_SYNC_1, 5, 0, 1 );
		List<byte[]> msgs = parser.parse( buf, buf.length );
		assertEquals( 0, msgs.size() );

		buf = genBuf( 2, 3 );
		msgs = parser.parse( buf, buf.length );
		assertEquals( 0, msgs.size() );

		buf = genBuf( 4, 5, -96 );
		msgs = parser.parse( buf, buf.length );
		assertEquals( 1, msgs.size() );

		assertArrayEquals( genBuf( 5, 0, 1, 2, 3, 4, 5 ), msgs.get( 0 ) );
	}

	private byte[] genBuf( int... data )
	{
		byte[] bytes = new byte[data.length];
		for( int i = 0; i < data.length; i++ )
		{
			bytes[i] = (byte) data[i];
		}

		return bytes;
	}
}
