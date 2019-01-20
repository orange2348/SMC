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
// Copyright (C) 2000 - 2007. Charles W. Rapp.
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
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.5  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:19:17  charlesr
// Initial revision
//

package smc_ex6;

/**
 * Classes wishing to receive inbound TCP bytes should implement
 * this interface and connect it to a {@link TcpClient}
 * connection.
 *
 * @see TcpClient
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public interface TcpClientListener
    extends TcpConnectionListener
{
    /**
     * This method is called when the TCP client connection is
     * successfully opened to the destination host and port.
     * @param client the now open TCP client.
     */
    void opened(TcpClient client);

    /**
     * This method is called when the TCP client fails to
     * establish a connection to the destination host and port.
     * @param reason text explaining the failure reason.
     * @param client the closed TCP client.
     */
    void openFailed(String reason, TcpClient client);

    /**
     * This method is called when a previously open TCP client
     * connection is closed.
     * @param reason text explaining why the client is closed.
     * @param client the now closed TCP client.
     */
    void closed(String reason, TcpClient client);

    /**
     * This method is called when output is now successfully
     * transmitted to the destination.
     * @param client data transmitted on this client.
     */
    void transmitted(TcpClient client);

    /**
     * This method is called when output transmit failed.
     * @param reason text explaining the transmit failure.
     * @param client transmit failure occurred on this client.
     */
    void transmitFailed(String reason, TcpClient client);

    /**
     * This method is used to forward received bytes to listener.
     * @param data inbound bytes received on TCP connection.
     * @param client bytes received on this TCP connection.
     */
    void receive(byte[] data, TcpClient client);
}
