package org.cowboycoders.ant.interfaces;

/**
 *     Copyright (c) 2012-2013, Will Szumski, David George
 *
 *     This file is part of formicidae.
 *
 *     formicidae is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     formicidae is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with formicidae.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.cowboycoders.ant.messages.StandardMessage;
import org.cowboycoders.ant.messages.commands.ResetMessage;
import org.cowboycoders.ant.utils.ByteUtils;
import org.cowboycoders.ant.utils.UsbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.UsbConst;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AntTransceiver extends AbstractAntTransceiver
{
	private static final Logger log = LoggerFactory.getLogger( AntTransceiver.class );

	/**
	 * sync byte
	 */
	private static byte MESSAGE_TX_SYNC = (byte) 0xA4;

	/**
	 * Used to set read buffer size
	 */
	private static final int MAX_MSG_LENGTH = 23;
	private static final int MESSAGE_OFFSET_MSG_LENGTH = 1;

	/**
	 * Usb Interface
	 */
	private UsbInterface _interface;

	/**
	 * opened
	 */
	private volatile boolean running = false;

	/**
	 * class lock
	 */
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * interface claimed lock
	 */
	private ReentrantLock interfaceLock = new ReentrantLock();

	private boolean readEndpoint = true;

	private UsbEndpoint endpointIn;

	private UsbEndpoint endpointOut;

	private UsbDevice device;

	UsbPipe inPipe = null;

	private UsbReader usbReader;

	// private int deviceNumber;

	/**
	 * Looks for all known {@link org.cowboycoders.ant.interfaces.AntDeviceId}}
	 *
	 * @param deviceNumber
	 */
	public AntTransceiver( int deviceNumber )
	{
		this( null, deviceNumber );

	}

	/**
	 * Search for a specific device
	 *
	 * @param antId
	 * @param deviceNumber
	 */
	public AntTransceiver( AntDeviceId antId, int deviceNumber )
	{
		doInit( antId, deviceNumber );
	}

	/**
	 * Testing only
	 */
	AntTransceiver()
	{
	}

	private void doInit( AntDeviceId antId, int deviceNumber )
	{
		UsbServices usbServices = null;
		UsbHub rootHub;

		try
		{
			usbServices = UsbHostManager.getUsbServices();
			rootHub = usbServices.getRootUsbHub();
		}
		catch( SecurityException | UsbException e )
		{
			throw new AntCommunicationException( e );
		}

		List<UsbDevice> devices = new ArrayList<>();
		AntDeviceId[] deviceSearchList;

		// populate an array of device ids we wish to search for
		if( antId != null )
		{ // case : specific device requested
			deviceSearchList = new AntDeviceId[]{ antId };
		}
		else
		{ // case : look for all suitable devices
			deviceSearchList = AntDeviceId.values();
		}

		for( AntDeviceId device : deviceSearchList )
		{
			List<UsbDevice> matchingDevices;
			DeviceDescriptor usbDescriptor = device.getUsbDescriptor();
			short vendorId = usbDescriptor.getVendorId();
			short deviceId = usbDescriptor.getDeviceId();
			matchingDevices = UsbUtils.getUsbDevicesWithId( rootHub, vendorId, deviceId );
			if( matchingDevices.isEmpty() )
			{
				continue;
			}
			devices.addAll( matchingDevices );
		}

		log.info( "Number of detected USB devices: " + devices.size() );

		if( devices.size() < deviceNumber + 1 )
		{
			throw new AntCommunicationException( "Device not found" );
		}

		UsbDevice device = devices.get( deviceNumber );

		this.device = device;
	}

	private void logData( byte[] data, String tag )
	{
		StringBuilder logBuffer = new StringBuilder();

		for( Byte b : data )
		{
			logBuffer.append( String.format( "%x ", b ) );
		}

		logBuffer.append( (String.format( "\n" )) );

		log.debug( tag + " : " + logBuffer );
	}

	public class UsbReader extends Thread
	{
		private byte[] last;

		private static final int BUFFER_SIZE = 64;

		/**
		 * @param data buffer to check
		 * @return data remaining
		 */
		private byte[] lookForSync( byte[] data, int len )
		{
			if( data == null || data.length < 1 )
			{
				return new byte[0];
			}

			if( data[0] != MESSAGE_TX_SYNC )
			{
				int index = -1;
				for( int i = 0; i < len; i++ )
				{
					if( data[i] == MESSAGE_TX_SYNC )
					{
						index = i;
						break;
					}
				}
				// not found
				if( index < 0 )
				{
					log.warn( "data read from usb endpoint does not contain a sync byte : ignoring" );
					return new byte[0]; // zero length array
				}
				log.info( "found non-zero sync byte index" );
				data = Arrays.copyOfRange( data, index, data.length );
			}
			return data;
		}

		// All this array copying inefficient but simple (could keep reference
		// to current index instead)
		private byte[] skipCurrentSync( byte[] data )
		{
			if( data.length < 1 )
			{
				return new byte[0];
			}
			data = Arrays.copyOfRange( data, 1, data.length );
			return data;
		}

		/**
		 * Gets the next message and notifies interested listeners.
		 *
		 * @param data - message data
		 * @param len  - message length
		 */
		void processBuffer( byte[] data, int len )
		{
			while( len > 0 )
			{
				data = lookForSync( data, len );

				if( data.length <= MESSAGE_OFFSET_MSG_LENGTH )
				{
					log.info( "data length too small, checking next packet" );
					// assume rest will arrive in next packet
					last = data;
					break;
				}

				int msgLength = data[MESSAGE_OFFSET_MSG_LENGTH];

				// negative length does not make sense
				if( msgLength < 0 )
				{
					log.warn( "msgLength appears to be incorrect (ignorning). Length : " + msgLength );
					data = skipCurrentSync( data );
					continue;
				}

				int checkSumIndex = msgLength + 3;

				if( checkSumIndex >= data.length )
				{
					// unreasonably large checkSumIndex (dont span multiple
					// buffers)
					if( checkSumIndex >= BUFFER_SIZE - 1 )
					{
						log.warn( "msgLength appears to be incorrect (ignorning). Length : " + msgLength );
						data = skipCurrentSync( data );
						continue;
					}

					// we try assume continued in next buffer
					last = data;
					break;
				}

				// data minus sync and checksum
				byte[] cleanData = new byte[msgLength + 2];

				for( int i = 0; i < msgLength + 2; i++ )
				{
					cleanData[i] = data[i + 1];
				}

				if( getChecksum( cleanData ) != data[checkSumIndex] )
				{
					log.warn( "checksum incorrect : ignoring" );
					data = skipCurrentSync( data );
					continue;
				}

				AntTransceiver.this.broadcastRxMessage( cleanData );
				// cleandata length + sync + checksum
				len -= (cleanData.length + 2);
				data = Arrays.copyOfRange( data, cleanData.length + 2, data.length );
			}
		}

		/*
		 * Two Modifications (David George - 11/June/2013)
		 * 
		 * 1. continue if we get a USB Exception on read from lower layers, this
		 * is a timeout and we don't care
		 * 
		 * 2. use returned data length to make code more efficient (hopefully).
		 * No more searching for SYNC bytes in zero data
		 */
		@Override
		public void run()
		{

			try
			{
				while( readEndpoint )
				{

					// interfaceLock.lock();
					// byte [] data = new byte[MAX_MSG_LENGTH];
					byte[] data = new byte[BUFFER_SIZE];
					int len;
					try
					{
						// inPipe.open();
						len = inPipe.syncSubmit( data );
						// System.out.println("received " + len);
					}
					catch( UsbException e )
					{
						// Timeouts are expected in some implementations - these manifest
						// themselves as UsbExceptions. We should continue, but log the error
						// in case it indicates something more serious.
						log.warn( e.getMessage() );
						continue;
					}

					logData( data, "read" );

					// process remaining bytes from last buffer
					if( last != null )
					{
						// TODO len is bigger due to remaining bytes
						len += last.length;
						data = ByteUtils.joinArray( last, data );
						last = null;
					}

					processBuffer( data, len );

				}

			}
			catch( UsbNotActiveException | UsbDisconnectedException | IllegalArgumentException | UsbNotOpenException e )
			{
				log.error( "Error encountered while reading data", e );
			}

			log.trace( this.getClass().toString() + " killed" );
		}
	}

	/**
	 * @param _interface interface to claim / release
	 * @param claim      true to claim, false to release
	 */
	private void claimInterface( UsbInterface _interface, boolean claim )
	{

		try
		{
			interfaceLock.lock();
			if( claim )
			{
				_interface.claim();
				log.info( "USB Interface claimed." );
			}
			else
			{
				if( _interface.isClaimed() )
				{
					_interface.release();
					log.info( "USB Interface released." );
				}
			}
		}
		catch( UsbNotActiveException | UsbDisconnectedException | UsbException e )
		{
			throw new AntCommunicationException( e );
		}
		finally
		{
			interfaceLock.unlock();
		}

	}

	@Override
	public boolean start()
	{
		try
		{
			lock.lock();
			// already started
			if( running )
			{
				return true;
			}

			if( !device.isConfigured() )
			{
				throw new AntCommunicationException( "Ant stick not configured by OS" );
			}

			UsbInterface _interface = device.getActiveUsbConfiguration().getUsbInterface( (byte) 0 );

			this._interface = _interface;

			claimInterface( _interface, true );

			@SuppressWarnings( "unchecked" ) List<UsbEndpoint> endpoints = _interface.getUsbEndpoints();

			if( endpoints.size() != 2 )
			{
				throw new AntCommunicationException( "Unexpected number of endpoints" );
			}

			for( UsbEndpoint endpoint : endpoints )
			{
				if( endpoint.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN )
				{
					this.endpointIn = endpoint;
				}
				else
				{
					this.endpointOut = endpoint;
				}
			}

			if( this.endpointOut == null || this.endpointIn == null )
			{
				throw new AntCommunicationException( "Endpoints not found" );
			}

			inPipe = endpointIn.getUsbPipe();

			try
			{
				inPipe.open();
			}
			catch( UsbException e )
			{
				throw new AntCommunicationException( "Error opening inPipe" );
			}

			readEndpoint = true;

			this.usbReader = new UsbReader();

			this.usbReader.start();

			running = true;

		}
		catch( RuntimeException e )
		{
			e.printStackTrace();
			claimInterface( _interface, false );
			throw e;
		}
		finally
		{
			lock.unlock();
		}

		// TODO Auto-generated method stub
		return true;
	}

	private void killUsbReader()
	{

		readEndpoint = false;

		// Doesn't seem to work so we send a message instead
		// inPipe.abortAllSubmissions();

		StandardMessage msg = new ResetMessage();

		send( msg.encode() );

		try
		{
			usbReader.join();
		}
		catch( InterruptedException e )
		{
			log.warn( "interrupted waiting to shutdown device" );
		}
	}

	@Override
	public void stop()
	{
		try
		{
			lock.lock();

			if( !running )
			{
				return;
			}

			killUsbReader();

			try
			{
				// inPipe.abortAllSubmissions();
				inPipe.close();
			}
			catch( UsbException e )
			{
				throw new AntCommunicationException( "Error closing inPipe", e );
			}

			_interface.release();

			// } finally {
			// interfaceLock.unlock();
			// }

			// _interface.release();

			running = false;
		}
		catch( UsbException e )
		{
			throw new AntCommunicationException( e );
		}
		finally
		{
			lock.unlock();
		}

	}

	private void write( byte[] data ) throws
	                                  UsbNotActiveException,
	                                  UsbNotOpenException,
	                                  IllegalArgumentException,
	                                  UsbDisconnectedException,
	                                  UsbException
	{
		UsbPipe pipe = null;

		try
		{
			lock.lock();
			pipe = endpointOut.getUsbPipe();
			if( !pipe.isOpen() )
			{
				pipe.open();
			}
			pipe.syncSubmit( data );
			logData( data, "wrote" );
		}
		finally
		{
			if( pipe != null )
			{
				pipe.close();
			}
			lock.unlock();
		}

	}

	@Override
	public void send( byte[] message ) throws AntCommunicationException
	{
		try
		{
			if( !running )
			{
				throw new AntCommunicationException( "AntTransceiver not running. Use start()" );
			}
			write( addExtras( message ) );

		}
		catch( UsbNotActiveException | UsbException | UsbDisconnectedException | IllegalArgumentException | UsbNotOpenException e )
		{
			throw new AntCommunicationException( e );
		}
	}

	public byte getChecksum( byte[] nocheck )
	{
		byte checksum = 0;
		checksum = MESSAGE_TX_SYNC;
		for( byte b : nocheck )
		{
			checksum ^= b % 0xff;
		}
		return checksum;
	}

	public byte[] addExtras( byte[] nocheck )
	{
		byte[] data = new byte[nocheck.length + 2];
		data[0] = MESSAGE_TX_SYNC;
		for( int i = 1; i < data.length - 1; i++ )
		{
			data[i] = nocheck[i - 1];
		}
		data[data.length - 1] = getChecksum( nocheck );
		return data;
	}

	@Override
	public boolean isRunning()
	{
		try
		{
			lock.lock();
			return running;
		}
		finally
		{
			lock.unlock();
		}
	}
}
