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
// Copyright (C) 2009. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//
// RCS ID
// Id: SmcOptions.java,v 1.4 2013/09/02 14:45:58 cwrapp Exp
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

/**
 * This passive, immutable class stores the SMC generator options
 * and an instance is passed to the generator constructors.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcOptions
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Locals.
    //

    /**
     * The application name.
     */
    private final String mAppName;

    /**
     * The application version.
     */
    private final String mAppVersion;

    // The .sm file's base name.
    private final String mSrcfileBase;

    // The target file's base name.
    private final String mTargetfileBase;

    // Write the target source file to this directory.
    private final String mTargetDirectory;

    // Place the generated header file in this directory.
    private final String mHeaderDirectory;

    // The header file suffix.
    private final String mHeaderSuffix;

    // Use this cast type (C++ only).
    private final String mCastType;

    // Generate this much detail in the graph (-graph only).
    private final int mGraphLevel;

    // This flag is true when serialization is to be generated.
    private final boolean mSerialFlag;

    // Generate this much detail in the debug output.
    private final int mDebugLevel;

    // This flag is true when exceptions are not be thrown.
    private final boolean mNoExceptionFlag;

    // This flag is true when exceptions are not caught.
    private final boolean mNoCatchFlag;

    // This flag is true when I/O streams should not be used.
    private final boolean mNoStreamsFlag;

    // This flag is true when the state machine class is a template
    // from which the user defined class will be derived (CRTP).
    private final boolean mCRTPFlag;

    // The fixed-length state stack size. Used with -c++ and
    // -noex only.
    private final int mStateStackSize;

    // This flag is true when reflection is supported.
    private final boolean mReflectFlag;

    // This flag is true when synchronization code is to be
    // generated.
    private final boolean mSyncFlag;

    // This flag is true when reflection is to use a
    // generic transition map. Used with -java and -reflect only.
    private final boolean mGenericFlag;

    // When generics are used and the target language is Java,
    // then output diamond braces (<>) if the Java target version
    // is Java 7 or better.
    private final boolean mJava7Flag;

    // Use this access keyword for the generated classes.
    private final String mAccessLevel;

    // Use "@protocol" instead of "@class". Used with -objc only.
    private final boolean mUseProtocolFlag;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Stores the target code generator options.
     * @param appName application name.
     * @param appVersion application version.
     * @param srcfileBase basename of source file (*.sm).
     * @param targetfileBase write the emitted code to this target
     * source file name sans the suffix.
     * @param targetDirectory place the target source file in this
     * directory.
     * @param headerDirectory place the target header file in
     * this directory. Ignored if there is no generated header
     * file.
     * @param headerSuffix header file name suffix.
     * @param castType use this type cast (C++ code generation
     * only).
     * @param graphLevel amount of detail in the generated
     * GraphViz graph (graph code generation only).
     * @param serialFlag if {@code true}, generate unique
     * identifiers for persisting the FSM.
     * @param debugLevel if &ge; zero add debug output messages
     * to code.
     * @param noExceptionFlag if {@code true} then use asserts
     * rather than exceptions (C++ only).
     * @param noCatchFlag if {@code true} then do <i>not</i>
     * generate try/catch/rethrow code.
     * @param noStreamsFlag if {@code true} then use TRACE macro
     * for debug output.
     * @param crtpFlag if {@code true} then user defined class
     * derived from generated code via CRTP.
     * @param stateStackSize statically allocated state stack
     * maximum size. (C++ only).
     * @param reflectFlag if {@code true} then generate
     * reflection code.
     * @param syncFlag if {@code true} then generate
     * synchronization code.
     * @param genericFlag if {@code true} then use generic
     * collections.
     * @param java7Flag if {@code genericFlag} is {@code true}
     * and the target language is Java, then generate generic
     * code according to this Java version.
     * @param accessLevel use this access keyword for the
     * generated classes.
     * @param useProtocolFlag use "@protocol" instead of "@class"
     * in generated Objective-C code.
     */
    public SmcOptions(final String appName,
                      final String appVersion,
                      final String srcfileBase,
                      final String targetfileBase,
                      final String targetDirectory,
                      final String headerDirectory,
                      final String headerSuffix,
                      final String castType,
                      final int graphLevel,
                      final boolean serialFlag,
                      final int debugLevel,
                      final boolean noExceptionFlag,
                      final boolean noCatchFlag,
                      final boolean noStreamsFlag,
                      final boolean crtpFlag,
                      final int stateStackSize,
                      final boolean reflectFlag,
                      final boolean syncFlag,
                      final boolean genericFlag,
                      final boolean java7Flag,
                      final String accessLevel,
                      final boolean useProtocolFlag)
    {
        mAppName = appName;
        mAppVersion = appVersion;
        mSrcfileBase = srcfileBase;
        mTargetfileBase = targetfileBase;
        mHeaderDirectory = headerDirectory;
        mHeaderSuffix = headerSuffix;
        mCastType = castType;
        mGraphLevel = graphLevel;
        mTargetDirectory = targetDirectory;
        mSerialFlag = serialFlag;
        mDebugLevel = debugLevel;
        mNoExceptionFlag = noExceptionFlag;
        mNoCatchFlag = noCatchFlag;
        mNoStreamsFlag = noStreamsFlag;
        mCRTPFlag = crtpFlag;
        mStateStackSize = stateStackSize;
        mReflectFlag = reflectFlag;
        mSyncFlag = syncFlag;
        mGenericFlag = genericFlag;
        mJava7Flag = java7Flag;
        mAccessLevel = accessLevel;
        mUseProtocolFlag = useProtocolFlag;
    } // end f SmcOptions(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the application name.
     * @return application name.
     */
    public String applicationName()
    {
        return (mAppName);
    } // end of applicationName()

    /**
     * Returns the application version.
     * @return application version.
     */
    public String applicationVersion()
    {
        return (mAppVersion);
    } // end of applicationVersion()

    /**
     * Returns the source file name's base.
     * @return the source file name's base.
     */
    public String srcfileBase()
    {
        return (mSrcfileBase);
    } // end of srcfileBase()

    /**
     * Returns the target source file name's base.
     * @return the target source file name's base.
     */
    public String targetfileBase()
    {
        return (mTargetfileBase);
    } // end of targetfileBase()

    /**
     * Returns the target source file's directory.
     * @return the target source file's directory.
     */
    public String targetDirectory()
    {
        return (mTargetDirectory);
    } // end of targetDirectory()

    /**
     * Returns the target header file's directory.
     * @return the target header file's directory.
     */
    public String headerDirectory()
    {
        return (mHeaderDirectory);
    } // end of headerDirectory()

    /**
     * Returns the target header file suffix.
     * @return the target header file suffix.
     */
    public String headerSuffix()
    {
        return (mHeaderSuffix);
    } // end of headerSuffix()

    /**
     * Returns the C++ cast type.
     * @return the C++ cast type.
     */
    public String castType()
    {
        return (mCastType);
    } // end of castType()

    /**
     * Returns the GraphViz graph detail level.
     * @return the GraphViz graph detail level.
     */
    public int graphLevel()
    {
        return (mGraphLevel);
    } // end of graphLevel()

    /**
     * Returns the serialization flag.
     * @return the serialization flag.
     */
    public boolean serialFlag()
    {
        return (mSerialFlag);
    } // end of serialFlag()

    /**
     * Returns the debug output level.
     * @return the debug output level.
     */
    public int debugLevel()
    {
        return (mDebugLevel);
    } // end of debugLevel()

    /**
     * Returns the "no exception" flag.
     * @return the "no exception" flag.
     */
    public boolean noExceptionFlag()
    {
        return (mNoExceptionFlag);
    } // end of noExceptionFlag()

    /**
     * Returns the "no catch" flag.
     * @return the "no catch" flag.
     */
    public boolean noCatchFlag()
    {
        return (mNoCatchFlag);
    } // end of noCatchFlag()

    /**
     * Returns the "no streams" flag.
     * @return the "no streams" flag.
     */
    public boolean noStreamsFlag()
    {
        return (mNoStreamsFlag);
    } // end of noStreamsFlag()

    /**
     * Returns the "crtp" flag.
     * @return the "crtp" flag.
     */
    public boolean crtpFlag()
    {
        return (mCRTPFlag);
    } // end of crtpFlag()

    /**
     * Returns the fixed-length state stack size. A zero return
     * value means that the state stack size is unbounded.
     * @return fixed-length state stack size.
     */
    public int stateStackSize()
    {
        return (mStateStackSize);
    } // end of stateStackSize()

    /**
     * Returns the reflection flag.
     * @return the reflection flag.
     */
    public boolean reflectFlag()
    {
        return (mReflectFlag);
    } // end of reflectFlag()

    /**
     * Returns the synchronization flag.
     * @return the synchronization flag.
     */
    public boolean syncFlag()
    {
        return (mSyncFlag);
    } // end of syncFlag()

    /**
     * Returns the generic reflection flag.
     * @return the generic reflection flag.
     */
    public boolean genericFlag()
    {
        return (mGenericFlag);
    } // end of genericFlag()

    /**
     * Returns {@code true} if Java 7 diamond brace (&lt;&gt;)
     * are used in generic code.
     * @return {@code true} if the target language is Java 7.
     */
    public boolean java7Flag()
    {
        return (mJava7Flag);
    } // end of java7Flag()

    /**
     * Returns the generated class access level.
     * @return the generated class access level.
     */
    public String accessLevel()
    {
        return (mAccessLevel);
    } // end of accessLevel()

    /**
     * Returns {@code true} if "@protocol" should be used in
     * generated Objective-C code.
     * @return {@code true} if "@protocol" should be used in
     * generated Objective-C code.
     */
    public boolean useProtocolFlag()
    {
        return (mUseProtocolFlag);
    } // end of useProtocolFlag()

    //
    // end of Get methods.
    //-----------------------------------------------------------
} // end of class SmcOptions

//
// CHANGE LOG
// Log: SmcOptions.java,v
// Revision 1.4  2013/09/02 14:45:58  cwrapp
// SMC 6.3.0 commit.
//
// Revision 1.3  2013/07/14 14:32:38  cwrapp
// check in for release 6.2.0
//
// Revision 1.2  2010/02/15 18:05:43  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.1  2009/11/24 20:46:50  cwrapp
// Initial check in.
//
