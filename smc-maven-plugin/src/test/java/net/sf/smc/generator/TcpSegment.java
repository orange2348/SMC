//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
//
// The Original Code is State Machine Compiler (SMC).
//
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2007, 2019. Charles W. Rapp.
// All Rights Reserved.
//

package net.sf.smc.generator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public final class TcpSegment
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    public static final InetAddress LOCAL_ADDRESS =
        InetAddress.getLoopbackAddress();

    // TCP header flags.
    public static final int FIN = 0x01;
    public static final int SYN = 0x02;
    public static final int RST = 0x04;
    public static final int PSH = 0x08;
    public static final int ACK = 0x10;
    public static final int URG = 0x20;
    public static final int FIN_ACK = (FIN | ACK);
    public static final int SYN_ACK = (SYN | ACK);
    public static final int RST_ACK = (RST | ACK);
    public static final int PSH_ACK = (PSH | ACK);
    public static final int FLAG_MASK =
        (FIN | SYN | RST | PSH | ACK | URG);

    // Use this static byte array to store a generic TCP header.
    // Copy and modify for TCP transmissions.
    /* package */ static final int TCP_HEADER_SIZE = 16;

    private static final byte[] NO_DATA = new byte[0];

    //-----------------------------------------------------------
    // Locals.
    //

    public InetSocketAddress mSrcAddress;
    public final InetSocketAddress mDestAddress;
    public final int mSequenceNumber;
    public final int mAckNumber;
    public final int mFlags;
    public final byte[] mData;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    private TcpSegment(final InetSocketAddress srcAddress,
                       final InetSocketAddress destAddress,
                       final int seqNum,
                       final int ackNumber,
                       final int flags,
                       final byte[] data)
    {
        mSrcAddress = srcAddress;
        mDestAddress = destAddress;
        mSequenceNumber = seqNum;
        mAckNumber = ackNumber;
        mFlags = flags & FLAG_MASK;
        mData = data;
    } // end of TcpSegment(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Object Method Overrides.
    //

    @Override
    public String toString()
    {
        final StringBuilder retval = new StringBuilder();

        retval.append("\tSource       : ").append(mSrcAddress)
              .append("\n\tDestination  : ").append(mDestAddress)
              .append("\n\tSequence #   : ").append(mSequenceNumber)
              .append("\n\tAcknowledge #: ").append(mAckNumber)
              .append("\n\tFlags        : ").append(flagsToString(mFlags))
              .append("\n\tData size    : ").append(mData.length)
              .append("\n\tData         : \"").append(new String(mData)).append('"');

        return(retval.toString());
    } // end of toString()

    //
    // end of Object Method Overrides.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get Methods.
    //

    public InetSocketAddress getSourceAddress()
    {
        return(mSrcAddress);
    } // end of getSourceAddress()

    public int getSourcePort()
    {
        return (mSrcAddress.getPort());
    } // end of getSourcePort()

    public InetSocketAddress getDestinationAddress()
    {
        return (mDestAddress);
    } // end of getDestination()

    public int getDestinationPort()
    {
        return (mDestAddress.getPort());
    } // end of getDestinationPort()

    public int getSequenceNumber()
    {
        return(mSequenceNumber);
    } // end of getSequenceNumber()

    public int getAcknowledgeNumber()
    {
        return(mAckNumber);
    } // end of getAcknowledgeNumber()

    public int getFlags()
    {
        return(mFlags);
    } // end of getFlags()

    public boolean hasData()
    {
        return (mData.length > 0);
    } // end of hasData()

    public int getDataSize()
    {
        return (mData.length);
    } // end of getDataSize()

    /* package */ byte[] getData()
    {
        return (mData);
    } // end of getData()

    //
    // end of Get Methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set Methods.
    //

    public void setSourcePort(final int port)
    {
        mSrcAddress = new InetSocketAddress(port);
    } // end of setSourcePort(int)

    //
    // end of Set Methods.
    //-----------------------------------------------------------

    /**
     * Returns a TCP segment created from the given parameters.
     * Source address is set to this local host.
     * @param srcPort segment origin port.
     * @param destAddress segment destination address.
     * @param seqNum TCP sequence number.
     * @param ackNum TCP acknowledgment sequence number.
     * @param flags TCP segment flags.
     * @param data TCP payload.
     * @param offset offset into payload.
     * @param size payload size in bytes.
     * @return TCP segment.
     */
    public static TcpSegment createSegment(final int srcPort,
                                           final InetSocketAddress destAddress,
                                           final int seqNum,
                                           final int ackNum,
                                           final int flags,
                                           final byte[] data,
                                           final int offset,
                                           final int size)
    {
        final InetSocketAddress srcAddress =
            new InetSocketAddress(LOCAL_ADDRESS, srcPort);
        final byte[] payload;

        if (data == null || size == 0)
        {
            payload = NO_DATA;
        }
        else
        {
            payload = new byte[size];
            System.arraycopy(data, offset, payload, 0, size);
        }

        return (new TcpSegment(srcAddress,
                               destAddress,
                               seqNum,
                               ackNum,
                               flags,
                               payload));
    } // end of createSegment(...)

    /**
     * Returns the TCP segment extracted from the datagrapm
     * buffer.
     * @param buffer datagram buffer containing TCP socket.
     * @param address input came from this address.
     * @return TCP segment.
     */
    public static TcpSegment createSegment(final ByteBuffer buffer,
                                           final InetSocketAddress address)
    {
        final InetSocketAddress srcAddress =
            new InetSocketAddress(
                address.getAddress(), buffer.getInt());
        final InetSocketAddress destAddress =
            new InetSocketAddress(buffer.getInt());
        final int seqNum = buffer.getInt();
        final int ackNum = buffer.getInt();
        final int flags = buffer.getInt();
        final int dataSize = buffer.getInt();
        final byte[] data = new byte[dataSize];

        buffer.get(data);

        return (new TcpSegment(srcAddress,
                               destAddress,
                               seqNum,
                               ackNum,
                               flags,
                               data));
    } // end of createSegment(ByteBuffer, InetSocketAddress)

    /**
     * Returns a string containing the TCP flags.
     * @param flags TCP flags bit mask.
     * @return TCP flags as text.
     */
    private static String flagsToString(final int flags)
    {
        String separator = "{";
        final StringBuilder retval = new StringBuilder();

        if ((flags & FIN) == FIN)
        {
            retval.append(separator).append("FIN");
            separator = ", ";
        }

        if ((flags & SYN) == SYN)
        {
            retval.append(separator).append("SYN");
            separator = ", ";
        }

        if ((flags & RST) == RST)
        {
            retval.append(separator).append("RST");
            separator = ", ";
        }

        if ((flags & PSH) == PSH)
        {
            retval.append(separator).append("PSH");
            separator = ", ";
        }

        if ((flags & ACK) == ACK)
        {
            retval.append(separator).append("ACK");
            separator = ", ";
        }

        if ((flags & URG) == URG)
        {
            retval.append(separator).append("URG");
        }

        retval.append("}");

        return (retval.toString());
    } // end of flagsToString(int)
} // end of class TcpSegment
