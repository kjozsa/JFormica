package org.cowboycoders.ant.examples.demos.hrm;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cowboycoders.ant.examples.demos.hrm.BasicHeartRateMonitor.HRMListener.intToString;

public class HRMListenerTest
{
	private static final Logger log = LoggerFactory.getLogger( HRMListenerTest.class );

	@Test
	public void testIntToString() throws Exception
	{
		for( int i = 0; i < 129; i++ )
		{
			String s = intToString( i, 4 );
			int pageNumber = i & 0x7F;
			boolean pageChange = ((i & 0x80) != 0);

			log.info( "s: {} : {}", s, pageChange );
		}
	}
}