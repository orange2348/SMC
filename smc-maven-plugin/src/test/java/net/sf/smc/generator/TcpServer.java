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

package net.sf.smc.generator;

public final class TcpServer
    extends TcpConnection
{
//---------------------------------------------------------------
// Member data.
//

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    public TcpServer(final TcpConnectionListener listener)
    {
        super (listener);
    } // end of TcpServer(TcpConnectionListener)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    public void open(final int port)
    {
        passiveOpen(port);
        return;
    } // end of open(int)
} // end of class TcpServer(int)
