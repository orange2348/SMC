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
// Contributor(s):
//
// Name
//  TcpClient.java
//
// Description
//  A TCP client connection.
//

package net.sf.smc.generator;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class TcpClient
    extends TcpConnection
{
//---------------------------------------------------------------
// Member data
//

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    // Creates an unopened client.
    public TcpClient(final TcpConnectionListener listener)
    {
        super (listener);
    } // end of TcpClient(TcpConnectionListener)

    // Creates an "accepted" client connection. This constructor
    // may only be called by TcpConnection.
    /* package */ TcpClient(final InetSocketAddress remoteAddress,
                            final int seqNum,
                            final TcpServer server,
                            final TcpConnectionListener listener)
    {
        super (remoteAddress, seqNum, server, listener);
    } // end of TcpClient(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    public void open(final int port)
    {
        activeOpen(TcpSegment.LOCAL_ADDRESS, port);
    } // end of open(int)

    public void open(final InetAddress address,
                     final int port)
    {
        activeOpen(address, port);
    } // end of open(InetAddress, int)
} // end of class TcpClient
