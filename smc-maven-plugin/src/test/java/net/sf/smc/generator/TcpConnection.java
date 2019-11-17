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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import net.sf.eBus.net.AbstractAsyncDatagramSocket;
import net.sf.eBus.net.AsyncDatagramSocket;
import net.sf.eBus.net.DatagramBufferWriter;
import net.sf.eBus.net.DatagramListener;
import net.sf.eBus.util.TimerEvent;
import net.sf.eBus.util.TimerTask;
import net.sf.eBus.util.TimerTaskListener;

/**
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

@SuppressWarnings("unchecked")
public abstract class TcpConnection
    implements DatagramListener
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    // The Initial Sequence Number.
    private static final int ISN = 1415531521;

    // Wait only so long for an ACK (in milliseconds).
    /* package */ static final long ACK_TIMEOUT = 2000;

    // Wait a while before reusing this port (in milliseconds).
    /* package */ static final long CLOSE_TIMEOUT = 10000;

    /* package */ static final long MIN_TIMEOUT = 1;

    private static final String TIMER_NAME = "DemoTimer";

    //-----------------------------------------------------------
    // Statics.
    //

    private static final Timer sTimer =
        new Timer(TIMER_NAME, true);

    // Use this table to translate received segment flags into
    // state map transitions.
    private static final Method[] sTransitionTable;

    // Class static initialization.
    static
    {

        sTransitionTable = new Method[TcpSegment.FLAG_MASK + 1];

        try
        {
            Class<TcpConnectionContext> context =
                TcpConnectionContext.class;
            Class<?>[] parameters = new Class<?>[1];
            Method undefined;
            int i;

            // All "TCP flag" transitions take a DatagramPacket as
            // a parameter.
            parameters[0] = TcpSegment.class;

            // First, set all transitions to undefined.
            undefined = context.getDeclaredMethod("UNDEF",
                                                  parameters);
            for (i = 0; i < sTransitionTable.length; ++i)
            {
                sTransitionTable[i] = undefined;
            }

            // Now go back and set the known transitions.
            sTransitionTable[TcpSegment.FIN] =
                context.getDeclaredMethod("FIN", parameters);
            sTransitionTable[TcpSegment.SYN] =
                context.getDeclaredMethod("SYN", parameters);
            sTransitionTable[TcpSegment.RST] =
                context.getDeclaredMethod("RST", parameters);
            sTransitionTable[TcpSegment.PSH] =
                context.getDeclaredMethod("PSH", parameters);
            sTransitionTable[TcpSegment.ACK] =
                context.getDeclaredMethod("ACK", parameters);
            sTransitionTable[TcpSegment.URG] =
                context.getDeclaredMethod("URG", parameters);
            sTransitionTable[TcpSegment.FIN_ACK] =
                context.getDeclaredMethod("FIN_ACK", parameters);
            sTransitionTable[TcpSegment.SYN_ACK] =
                context.getDeclaredMethod("SYN_ACK", parameters);
            sTransitionTable[TcpSegment.PSH_ACK] =
                context.getDeclaredMethod("PSH_ACK", parameters);
        }
        catch (NoSuchMethodException | SecurityException jex)
        {}
    } // end of class static initialization.

    //-----------------------------------------------------------
    // Locals.
    //

    private final TcpConnectionContext mFsm;
    private final PacketWriter mPacketWriter;
    private TcpConnectionListener mListener;
    private AsyncDatagramSocket mAsyncSocket;
    private int mSequenceNumber;

    // The port to which a client socket is connected.
    private InetSocketAddress mDestAddress;

    // The port to which this connection is bound.
    protected InetSocketAddress mBindAddress;

    // The server which accepted this connection.
    private final TcpServer mServer;

    // Currently running timer task. Set to null if there is no
    // such task.
    private TimerTask mTimerTask;

    // Store error messages here temporarily until forwarded to
    // listener.
    private String mErrorMessage;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    // Server socket constructor.
    protected TcpConnection(final TcpConnectionListener listener)
    {
        mListener = listener;
        mFsm = new TcpConnectionContext(this);
        mPacketWriter = new PacketWriter();
        mSequenceNumber = 0;
        mAsyncSocket = null;
        mDestAddress = null;
        mBindAddress = null;
        mServer = null;
        mErrorMessage = null;

        // REFLECTION
        // Turn on FSM debugging.
//        mFsm.setDebugFlag(true);
    } // end of TcpConnection(TcpConnectionListener)

    // "Accepted" socket constructor.
    protected TcpConnection(final InetSocketAddress remoteAddress,
                            final int seqNum,
                            final TcpServer server,
                            final TcpConnectionListener listener)
    {
        mDestAddress = remoteAddress;
        mBindAddress = null;
        mSequenceNumber = seqNum;
        mServer = server;
        mErrorMessage = null;
        mListener = listener;
        mPacketWriter = new PacketWriter();
        mFsm = new TcpConnectionContext(this);

        // REFLECTION
        // Turn on FSM debugging.
//        mFsm.setDebugFlag(true);
    } // end of TcpConnection(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // DatagramListener Interface Implementation.
    //

    @Override
    public final void handleInput(final ByteBuffer buffer,
                                  final InetSocketAddress address,
                                  final AbstractAsyncDatagramSocket socket)
    {
        try
        {
            final TcpSegment segment =
                TcpSegment.createSegment(buffer, address);

            // DEBUG
//            System.out.format(
//                "Receive event from %s:%n%s%n", address, segment);

            // REFLECTION
            // Uncomment the following line to output
            // transitions.
//            outputTransitions();

            sTransitionTable[segment.getFlags()].invoke(mFsm, segment);
        }
        catch (IllegalArgumentException |
               IllegalAccessException |
               InvocationTargetException jex)
        {
            System.err.println(jex);
            jex.printStackTrace(System.err);
        }
    } // end of handleInput(...)

    @Override
    public final void handleError(final Throwable e,
                                  final AbstractAsyncDatagramSocket dgramSocket)
    {
        System.err.println(
            "Unexpected error on datagram socket.");
        e.printStackTrace(System.err);
    } // end of handleError(Throwable, AbstractAsyncDatagramSocket)

    //
    // end of DatagramListener Interface Implementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get Methods.
    //

    // The address and port to which I am connected.
    public final InetSocketAddress getAddress()
    {
        return (mDestAddress);
    } // end of getAddress()

    public final InetSocketAddress getBindAddress()
    {
        return (mBindAddress);
    } // end of getBindAddress()

    public final boolean isOpen()
    {
        return (mAsyncSocket != null);
    } // end of isOpen()

    //
    // end of Get Methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set Methods.
    //

    public final void setDatagramSocket(final AsyncDatagramSocket dgramSocket)
    {
        mAsyncSocket = dgramSocket;
    } // end of setDatagramSocket(AsyncDatagromSocket)

    //
    // end of Set Methods.
    //-----------------------------------------------------------

    public final void start()
    {
        mFsm.enterStartState();
    } // end of start()

    public final void close()
    {
        synchronized (this)
        {
            try
            {
                // REFLECTION
                // Uncomment the following line to output
                // transitions.
//                outputTransitions();

                mFsm.Close();

                // Wait for the connection to close before
                // returning.
                while (mAsyncSocket != null)
                {
                    try
                    {
                        this.wait();
                    }
                    catch (InterruptedException interrupt)
                    {}
                }
            }
            finally
            {
                this.notifyAll();
            }
        }
    } // end of close()

    protected final void passiveOpen(final int port)
    {
        // REFLECTION
        // Uncomment the following line to output
        // transitions.
        // outputTransitions();

        mFsm.Open(port);

        return;
    } // end of passiveOpen(int)

    protected final void activeOpen(final InetAddress address,
                                    final int port)
    {
        final InetSocketAddress remoteAddress =
            new InetSocketAddress(address, port);

        // REFLECTION
        // Uncomment the following line to output
        // transitions.
        // outputTransitions();

        mFsm.Open(remoteAddress);
    } // end of activeOpen(InetAddress, int)

    protected final void acceptOpen(final TcpSegment segment)
    {
        // REFLECTION
        // Uncomment the following line to output
        // transitions.
        // outputTransitions();

        mFsm.Open(segment);

        return;
    } // end of acceptOpen(TcpSegment)

    protected final void setListener(final TcpConnectionListener listener)
        throws IllegalStateException
    {
        if (mListener != null)
        {
            throw(
                new IllegalStateException(
                    "Socket listener already set"));
        }
        else
        {
            mListener = listener;
        }

        return;
    } // end of setListener(TcpConnectionListener)

    protected void transmit(final byte[] data,
                            final int offset,
                            final int length)
    {
        // REFLECTION
        // Uncomment the following lines to output
        // transitions.
        // outputTransitions();

        mFsm.Transmit(data, offset, length);
    } // end of transmit(byte[], int, int)

    //-----------------------------------------------------------
    // State Map Actions.
    //

    /* package */ InetSocketAddress getFarAddress()
    {
        return (mDestAddress);
    } // end of getFarAddress()

    /* package */ int getSequenceNumber()
    {
        return (mSequenceNumber);
    }

    /* package */ void openServerSocket(final int port)
    {
        try
        {
            // Create the asynchronous datagram socket listener
            // and start it running.
            final AsyncDatagramSocket.DatagramBuilder builder =
                AsyncDatagramSocket.builder();

            mBindAddress = new InetSocketAddress(port);

            mAsyncSocket =  builder.listener(this).build();
            mAsyncSocket.open(mBindAddress);

            // Set the sequence number.
            mSequenceNumber = ISN;

            startTimer("SERVER_OPENED", MIN_TIMEOUT);

        }
        catch (IOException ioex)
        {
            mErrorMessage = ioex.getMessage();
            startTimer("OPEN_FAILED", MIN_TIMEOUT);
        }

        return;
    } // end of openServerSocket(int)

    /* package */ void openClientSocket(final InetSocketAddress address)
    {
        try
        {
            final AsyncDatagramSocket.DatagramBuilder builder =
                AsyncDatagramSocket.builder();

            mDestAddress = address;

            mAsyncSocket = builder.listener(this).build();
            mAsyncSocket.open();

            mBindAddress =
                (InetSocketAddress)
                    mAsyncSocket.localSocketAddress();

            // Set the sequence number.
            mSequenceNumber = ISN;

            startTimer("CLIENT_OPENED", MIN_TIMEOUT);
        }
        catch (IOException ioex)
        {
            // Do not issue a transition now since we are already
            // in a transition. Set a 1 millisecond timer and
            // issue transition when timer expires.
            mErrorMessage = ioex.toString();
            startTimer("OPEN_FAILED", MIN_TIMEOUT);
        }

        return;
    } // end of openClientSocket(InetAddress, int)

    /* package */ void openSuccess()
    {
        mListener.opened(this);
    } // end of openSuccess()

    /* package */ void openFailed(String reason)
    {
        mListener.openFailed(reason, this);
        return;
    }

    /* package */ void closeSocket()
    {
        synchronized (this)
        {
            try
            {
                if (mAsyncSocket != null)
                {
                    mAsyncSocket.close();

                    mAsyncSocket = null;
                    mDestAddress = null;
                }
            }
            finally
            {
                this.notify();
            }
        }
    } // end of closeSocket()

    /* package */ void halfClosed()
    {
        if (mListener != null)
        {
            mListener.halfClosed(this);
        }

        return;
    }

    /* package */ void closed(final String reason)
    {
        if (mListener != null)
        {
            mListener.closed(reason, this);
            mListener = null;
        }

        return;
    } // end of closed(String)

    /* package */ void clearListener()
    {
        mListener = null;
        return;
    }

    /* package */ void transmitted()
    {
        if (mListener != null)
        {
            mListener.transmitted(this);
        }

        return;
    }

    /* package */ void transmitFailed(String reason)
    {
        if (mListener != null)
        {
            mListener.transmitFailed(reason, this);
        }

        return;
    }

    /* package */ void receive(final TcpSegment segment)
    {
        // Send the TCP segment's data to the socket listener.
        if (mListener != null)
        {
            mListener.receive(segment.getData(), this);
        }

        return;
    } // end of receive(TcpSegment)

    // Creates a client socket to handle a new connection.
    /* package */ void accept(final TcpSegment segment)
    {
        try
        {
            final InetSocketAddress remoteAddress =
                segment.getSourceAddress();
            final AsyncDatagramSocket dgramSocket;
            TcpClient acceptClient;

            // Create a new client socket to handle this side of
            // the socket pair.
            acceptClient = new TcpClient(remoteAddress,
                                         mSequenceNumber,
                                         (TcpServer) this,
                                         mListener);

            dgramSocket =
                AsyncDatagramSocket.builder()
                                   .listener(acceptClient)
                                   .build();
            acceptClient.setDatagramSocket(dgramSocket);

            dgramSocket.open();

            acceptClient.acceptOpen(segment);
        }
        catch (IOException ioex)
        {
            // If the open fails, send a reset to the peer.
            send(TcpSegment.RST, null, 0, 0, segment);
        }

        return;
    } // end of accept(TcpSegment)

    /* package */ void accepted()
    {
        TcpServer server = mServer;
        TcpConnectionListener listener = mListener;

        // Tell the server listener that a new connection has
        // been accepted. Then clear the server listener because
        // this socket is now truly a client socket. Clear the
        // listener member data now because the callback method
        // will be resetting it and the reset will fail if we
        // don't do it.
        mListener = null;
        listener.accepted((TcpClient) this, server);

        return;
    } // end of accepted()

    // Send the SYN/ACK reply to the client's SYN.
    /* package */ void sendAcceptSynAck(final TcpSegment segment)
    {
        int clientPort;
        byte[] portBytes = new byte[2];

        // Tell the far-side client with what port it should now
        // communicate.
        clientPort =
            (mAsyncSocket.datagramSocket()).getLocalPort();

        portBytes[0] = (byte) ((clientPort & 0x0000ff00) >> 8);
        portBytes[1] = (byte)  (clientPort & 0x000000ff);

        send(TcpSegment.SYN_ACK, portBytes, 0, 2, null, segment);
    } // end of send/AcceptSynAck(TcpSegment)

    /* package */ void send(int flags,
                            byte[] data,
                            int offset,
                            int size,
                            TcpSegment recvSegment)
    {
        send(flags,
             data,
             offset,
             size,
             recvSegment.getSourceAddress(),
             recvSegment);
    } // end of send(int, byte[], int, int, TcpSegment)

    /* package */ void send(int flags,
                            byte[] data,
                            int offset,
                            int size,
                            InetSocketAddress address,
                            TcpSegment recvSegment)
    {
        // Quietly quit if there is no socket or no data to send.
        if (mAsyncSocket != null)
        {
            final int localPort;
            final int ackNum;
            final InetSocketAddress dest;
            TcpSegment sendSegment;

            // If there is a recv_segment, then use its
            // destination port as the local port. Otherwise, use
            // the local datagram socket's local port.
            if (recvSegment != null)
            {
                localPort = recvSegment.getDestinationPort();
            }
            else
            {
                localPort =
                    (mAsyncSocket.datagramSocket()).getLocalPort();
            }

            // Send the ack number only if the ack flag is set.
            if ((flags & TcpSegment.ACK) == 0)
            {
                ackNum = 0;
            }
            else
            {
                // Figure out the ack number based on the
                // received segment's sequence number and data
                // size.
                ackNum = getAck(recvSegment);
            }

            if (address == null)
            {
                dest = mDestAddress;
            }
            else
            {
                dest = address;
            }

            sendSegment =
                TcpSegment.createSegment(localPort,
                                         dest,
                                         mSequenceNumber,
                                         ackNum,
                                         flags,
                                         data,
                                         offset,
                                         size);

            // Advance the sequence number depending on the
            // message sent. Don't do this if message came from
            // an interloper.
            if (mDestAddress.equals(dest))
            {
                mSequenceNumber = getAck(sendSegment);
            }

            // Now send the data.
            try
            {
                mPacketWriter.send(sendSegment);

                // DEBUG
//                System.out.println(
//                    "Sending packet:\n" + sendSegment);

                mAsyncSocket.send(mPacketWriter);
            }
            catch (IOException ioex)
            {
                // Ignore - the ack timer will figure out this
                // packet was never sent.

                // DEBUG
//                System.err.println(
//                    "Send to " +
//                     sendSegment.getDestinationAddress() +
//                     " failed: " +
//                     ioex.getMessage());
//                ioex.printStackTrace(System.err);
            }
        }
    } // end of send(...)

    /* package */ synchronized void startTimer(final String name,
                                               final long time)
    {
        switch (name)
        {
            case "CONN_ACK_TIMER":
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.ConnAckTimeout();
                            }
                        });
                break;

            case "TRANS_ACK_TIMER":
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.TransAckTimeout();
                            }
                        });
                break;

            case "CLOSE_ACK_TIMER":
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.CloseAckTimeout();
                            }
                        });
                break;

            case "CLOSE_TIMER":
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.CloseTimeout();
                            }
                        });
                break;

            case "SERVER_OPENED":
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.Accepted();
                            }
                        });
                break;

            case "CLIENT_OPENED":
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.Opened(mDestAddress);
                            }
                        });
                break;

            // Open failed.
            default:
                mTimerTask =
                    new TimerTask(
                        new TimerTaskListener()
                        {
                            @Override
                            public synchronized void handleTimeout(final TimerEvent event)
                            {
                                mFsm.OpenFailed(mErrorMessage);
                                mErrorMessage = null;
                            }
                        });
        }

        sTimer.schedule(mTimerTask, time);
    } // end of startTimer(String, long)

    /* package */ synchronized void stopTimer()
    {
        if (mTimerTask != null)
        {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    } // end of stopTimer()

    /* package */ void setDestinationPort(final TcpSegment segment)
    {
        final byte[] data = segment.getData();
        final int port = (((((int) data[0]) & 0x000000ff) << 8) |
                          (((int) data[1]) & 0x000000ff));

        // The server socket is telling us the accepted client's
        // port number. Reset the destination port to that.
        mDestAddress =
            new InetSocketAddress(
                mDestAddress.getAddress(), port);

        // Modify the segment's source port so that the ack will
        // go to the correct destination.
        segment.setSourcePort(port);

        return;
    } // end of setDestinationPort(TcpSegment)

    private int getAck(final TcpSegment segment)
    {
        int retval;

        // The ack # depends on the segment's flags.
        switch (segment.getFlags())
        {
            case TcpSegment.FIN:
            case TcpSegment.SYN:
            case TcpSegment.FIN_ACK:
            case TcpSegment.SYN_ACK:
                retval = segment.getSequenceNumber() + 1;
                break;

            case TcpSegment.PSH:
            case TcpSegment.PSH_ACK:
                retval = segment.getSequenceNumber() +
                         segment.getDataSize();
                break;

            case TcpSegment.ACK:
            default:
                retval = segment.getSequenceNumber();
                break;
        }

        return (retval);
    } // end of getAck(TcpSegment)

    /*
     * REFLECTION
     * Uncomment the following method to output transitions.
    private void outputTransitions()
    {
        if (mFsm.getDebugFlag() == true)
        {
            final java.io.PrintStream str = mFsm.getDebugStream();
            final statemap.State7 state =
                (mFsm.isInTransition() ?
                 mFsm.getPreviousState() :
                 mFsm.getState());
            final String[] transitions = state.getTransitions();
            final int numTrans = transitions.length;
            int index;
            String sep;

            str.format(
                "State %s has transitions ", state.getName());

            for (index = 0, sep = "{";
                 index < numTrans;
                 ++index, sep = ", ")
            {
                str.format("%s%s", sep, transitions[index]);
            }

            str.println("}");
        }

        return;
    } // end of outputTransitions()
     *
     */

//---------------------------------------------------------------
// Inner classes.
//

    private static final class PacketWriter
        implements DatagramBufferWriter
    {
    //-----------------------------------------------------------
   // Member data.
   //

        //-------------------------------------------------------
        // Locals.
        //

        // Post these segments to the outgoing byte buffer.
        private final Queue<TcpSegment> mSegments;

    //-----------------------------------------------------------
    // Member methods.
    //

        //-------------------------------------------------------
        // Constructors.
        //

        private PacketWriter()
        {
            mSegments = new LinkedList<>();
        } // end of PacketWriter()

        //
        // end of Constructors.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // DatagramBufferWriter Interface Implementation.
        //

        @Override
        public SocketAddress fill(final ByteBuffer buffer)
            throws EmptyStackException
        {
            final TcpSegment segment = mSegments.poll();

            if (segment == null)
            {
                throw (new EmptyStackException());
            }

            buffer.putInt(segment.getSourcePort())
                  .putInt(segment.getDestinationPort())
                  .putInt(segment.getSequenceNumber())
                  .putInt(segment.getAcknowledgeNumber())
                  .putInt(segment.getFlags())
                  .putInt(segment.getDataSize());

            // Is there data to send?
            if (segment.hasData())
            {
                buffer.put(segment.getData());
            }

            return (segment.getDestinationAddress());
        } // end of fill(ByteBuffer)

        //
        // end of DatagramBufferWriter Interface Implementation.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Get Methods.
        //
        //
        // end of Get Methods.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Set Methods.
        //

        private void send(final TcpSegment segment)
        {
            mSegments.add(segment);
        } // end of send(TcpSegment)

        //
        // end of Set Methods.
        //-------------------------------------------------------
    } // end of class PacketWriter
} // end of class TcpConnection
