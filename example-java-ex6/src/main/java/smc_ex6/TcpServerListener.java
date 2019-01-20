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
// Revision 1.0  2003/12/14 20:21:20  charlesr
// Initial revision
//

package smc_ex6;

/**
 * Classes wishing to be informed about accept TCP client
 * connections should implement this interface and pass it to
 * {@link TcpServer}
 *
 * @see TcpServer
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */
public interface TcpServerListener
    extends TcpConnectionListener
{

    /**
     * Called when the TCP server is opened.
     * @param server newly opened TCP server.
     */
    void opened(TcpServer server);

    /**
     * Called when TCP service fails to open.
     * @param reason text explaining why the TCP service failed
     * to open.
     * @param server closed TCP server.
     */
    void openFailed(String reason, TcpServer server);

    /**
     * Called when the TCP server is closed.
     * @param reason text explaining why the server is closed.
     * @param server newly closed TCP server.
     */
    void closed(String reason, TcpServer server);

    /**
     * Called when TCP server accepted a TCP client connection.
     * @param client accepted client connection.
     * @param server TCP server accepting {@code client}.
     */
    @Override
    void accepted(TcpClient client, TcpServer server);
}
