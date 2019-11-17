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
// Copyright (C) 2019. Charles W. Rapp.
// All Rights Reserved.
//
// Port to Groovy by Francois Perrad, francois.perrad@gadz.org
// Copyright 2007-2009, Francois Perrad.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python, Perl code generation and examples/Perl,
//   Ruby code generation and examples/Ruby, Lua code generation
//   and examples/Lua.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//

package net.sf.smc.generator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import org.junit.Test;

/**
 * Tests the C code generator to validate correct code
 * generation.
 *
 * @author charlesr
 */

public final class CGeneratorTest
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    private static final String APP_NAME = "unitTest";
    private static final String APP_VERSION = "0.0.0";
    private static final String SRC_BASE = "test";
    private static final String TARGET_BASE = "test_sm";
    private static final String TARGET_DIR = "./";
    private static final String HEADER_DIR = "./";
    private static final String TARGET_SUFFIX = ".c";
    private static final String HEADER_SUFFIX = ".h";
    private static final String FSM_NAME = "testFSM";
    private static final String TARGET_FILE = "/dev/null";
    private static final String MAP_NAME = "testMap";
    private static final String STATE_NAME = "testState";

    //-----------------------------------------------------------
    // Statics.
    //

    private static final SmcFSM sFsm;
    private static final SmcMap sMap;
    private static final SmcState sState;
    private static final List<SmcParameter> sParams;
    private static final List<SmcAction> sActions;
    private static final SmcOptions sOptions;
    private static final String sTargetFile;

    // Class static initialization.
    static
    {
        sFsm = new SmcFSM(FSM_NAME, TARGET_FILE);
        sMap = new SmcMap(MAP_NAME, 1, sFsm);
        sState = new SmcState(STATE_NAME, 2, sMap);
        sMap.addState(sState);
        sParams = new ArrayList<>();
        sActions = new ArrayList<>();
        sOptions = new SmcOptions(APP_NAME,
                                  APP_VERSION,
                                  SRC_BASE,
                                  TARGET_BASE,
                                  TARGET_DIR,
                                  HEADER_DIR,
                                  HEADER_SUFFIX,
                                  "",
                                  0,
                                  false,
                                  1,
                                  false,
                                  false,
                                  false,
                                  false,
                                  100,
                                  true,
                                  false,
                                  false,
                                  false,
                                  "",
                                  false);
        sTargetFile = TARGET_DIR + TARGET_BASE + TARGET_SUFFIX;
        sFsm.setContext("UnitTest");
    } // end of class static initialization.

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // JUnit Tests.
    //

    @Test
    public void pushTransitionTest()
        throws FileNotFoundException
    {
        final SmcTransition transition =
            new SmcTransition("testTransition",
                              sParams,
                              1,
                              123,
                              sState);
        final SmcGuard guard = new SmcGuard("", 2, transition);
        final SmcCGenerator generator =
            new SmcCGenerator(sOptions);

        guard.setTransType(SmcElement.TransType.TRANS_PUSH);
        guard.setEndState(SmcElement.NIL_STATE);
        guard.setPushState("ZerosMap::PushIt");
        guard.setActions(sActions);
        transition.addGuard(guard);

        try (final PrintStream fs = new PrintStream(sTargetFile))
        {
            generator.setTarget(fs);
            generator.visit(transition);
        }
    } // end of pushTransitionTest()

    //
    // end of JUnit Tests.
    //-----------------------------------------------------------
} // end of CGeneratorTest