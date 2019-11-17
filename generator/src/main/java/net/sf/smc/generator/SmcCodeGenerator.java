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
// Copyright (C) 2005, 2008 - 2009. Charles W. Rapp.
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
// Id: SmcCodeGenerator.java,v 1.8 2013/09/02 14:45:57 cwrapp Exp
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcVisitor;

/**
 * Base class for all target language code generators. The
 * syntax tree visitation methods of the
 * {@link net.sf.smc.model.SmcVisitor} super class are left to
 * this class' subclasses to define.
 *
 * @see SmcElement
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public abstract class SmcCodeGenerator
    extends SmcVisitor
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * The default header file suffix is "h".
     */
    public static final String DEFAULT_HEADER_SUFFIX = "h";

    // Debug output detail level.

    /**
     * No debug output.
     */
    public static final int NO_DEBUG_OUTPUT = -1;

    /**
     * Output states and transitions.
     */
    public static final int DEBUG_LEVEL_0 = 0;

    /**
     * Output states, transitions and all transition, entry and
     * exit actions.
     */
    public static final int DEBUG_LEVEL_1 = 1;

    // GraphViz detail level.

    /**
     * No graphing is done.
     */
    public static final int NO_GRAPH_LEVEL = -1;

    /**
     * Provide state and transition names only.
     */
    public static final int GRAPH_LEVEL_0 = 0;

    /**
     * Provide state and transition names plus transition guards
     * and actions.
     */
    public static final int GRAPH_LEVEL_1 = 1;

    /**
     * Provides state names, entry and exit actions, transition
     * name and arguments, guards, actions and their action
     * parameters and pop transition arguments.
     */
    public static final int GRAPH_LEVEL_2 = 2;

    /**
     * The target file name path format.
     */
    private static final String TARGET_PATH_FORMAT =
        "{0}{1}.{2}";

    /**
     * The backup path element is "..".
     */
    private static final String BACKDIR = "..";

    //-----------------------------------------------------------
    // Statics.
    //

    // Append this suffix to the end of the output file.
    private static String sSuffix;

    //-----------------------------------------------------------
    // Locals.
    //

    /**
     * Application name.
     */
    protected final String mAppName;

    /**
     * Application version.
     */
    protected final String mAppVersion;

    /**
     * Source file name underlying {@link #mTarget}.
     */
    protected String mTargetFile;

    /**
     * Emit the target target code to this output stream.
     */
    protected PrintStream mTarget;

    /**
     * The .sm file's base name.
     */
    protected final String mSrcfileBase;

    /**
     * The target file's base name.
     */
    protected final String mTargetfileBase;

    /**
     * Write the target target file to this directory.
     */
    protected final String mSrcDirectory;

    /**
     * Place the generated header file in this directory.
     */
    protected final String mHeaderDirectory;

    /**
     * Place this suffix on the header file.
     */
    protected final String mHeaderSuffix;

    /**
     * Use this cast type (C++ only).
     */
    protected final String mCastType;

    /**
     * Generate this much detail in the graph (-graph only).
     */
    protected final int mGraphLevel;

    /**
     * Output this indent before generating a line of code.
     */
    protected String mIndent;

    // This information is common between the transition and
    // guard visitor methods.
    /**
     * The total number of guards to be generated at this time.
     */
    protected int mGuardCount;

    /**
     * The guard currently being generated.
     */
    protected int mGuardIndex;

    /**
     * This flag is true when serialization is to be generated.
     */
    protected final boolean mSerialFlag;

    /**
     * This flag is true when debug output is to be generated.
     */
    protected final int mDebugLevel;

    /**
     * This flag is true when exceptions are not be thrown.
     */
    protected final boolean mNoExceptionFlag;

    /**
     * This flag is true when exceptions are not caught.
     */
    protected final boolean mNoCatchFlag;

    /**
     * This flag is true when I/O streams should not be used.
     */
    protected final boolean mNoStreamsFlag;

    /**
     * This flag is true when generated code is template for CRTP.
     */
    protected final boolean mCRTPFlag;

    /**
     * A value &gt; zero means that a statically-allocated state
     * stack of fixed-size is used. Otherwise, an unbounded,
     * dynamically allocated stack is used.
     */
    protected final int mStateStackSize;

    /**
     * This flag is true when reflection is supported.
     */
    protected final boolean mReflectFlag;

    /**
     * This flag is true when synchronization code is to be
     * generated.
     */
    protected final boolean mSyncFlag;

    /**
     * This flag is true when reflection is to use a
     * generic transition map. Used with -java and -reflect only.
     */
    protected final boolean mGenericFlag;

    /**
     * This flag is {@code true} when Java 7 generic code is
     * generated. Used with -java and -reflect only.
     */
    protected final boolean mJava7Flag;

    /**
     * Used this access keyword for the generated classes.
     */
    protected final String mAccessLevel;

    /**
     * This flag is {@code true} when Objective-C code uses a
     * protocol instead of a class.
     */
    protected final boolean mUseProtocolFlag;

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Constructs the target code generator for the given
     * parameters. All subclass constructors receive the same
     * arguments even though not all arguments apply to every
     * concrete code generator.
     * @param options The target code generator options.
     * @param suffix the target target file name suffix.
     */
    protected SmcCodeGenerator(final SmcOptions options,
                               final String suffix)
    {
        mAppName = options.applicationName();
        mAppVersion = options.applicationVersion();
        mSrcfileBase = options.srcfileBase();
        mTargetfileBase = options.targetfileBase();
        mSrcDirectory = options.targetDirectory();
        mHeaderDirectory = options.headerDirectory();
        mHeaderSuffix = options.headerSuffix();
        mCastType = options.castType();
        mGraphLevel = options.graphLevel();
        mSerialFlag = options.serialFlag();
        mDebugLevel = options.debugLevel();
        mNoExceptionFlag = options.noExceptionFlag();
        mNoCatchFlag = options.noCatchFlag();
        mNoStreamsFlag = options.noStreamsFlag();
        mCRTPFlag = options.crtpFlag();
        mStateStackSize = options.stateStackSize();
        mReflectFlag = options.reflectFlag();
        mSyncFlag = options.syncFlag();
        mGenericFlag = options.genericFlag();
        mJava7Flag = options.java7Flag();
        mAccessLevel = options.accessLevel();
        mUseProtocolFlag = options.useProtocolFlag();
        sSuffix = suffix;
        mTarget = null;
        mIndent = "";
        mGuardCount = 0;
        mGuardIndex = 0;
    } // end of SmcCodeGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    // Left undefined for the subclasses.

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the default generated file suffix used by this
     * code generator.
     * @return default file name suffix.
     */
    public String defaultSuffix()
    {
        return (sSuffix);
    } // end of defaultSuffix()

    /**
     * Returns the configured target file name. Returns
     * {@code null} if {@link #setTargetFile(String, String, String)}
     * not yet called.
     * @return the absolute target file name.
     */
    public String targetFile()
    {
        return (mTargetFile);
    } // end of targetFile()

    /**
     * Returns configure output stream. Returns {@code null} if
     * {@link #setTarget(PrintStream)} not yet called.
     * @return output stream.
     */
    public PrintStream target()
    {
        return (mTarget);
    } // end of target()

    /**
     * Returns {@code true} if this transition is an
     * <i>internal</i> loopback or a push transition and
     * {@code false} otherwise. If true, then do not perform the
     * the state exit and entry actions.
     * @param transType the transition type.
     * @param endState entering this state.
     * @return {@code true} if this transition is an internal
     * loopback or push transition and {@code false} otherwise.
     */
    protected boolean isLoopback(TransType transType,
                                 String endState)
    {
        return (
            (transType == TransType.TRANS_SET ||
             transType == TransType.TRANS_PUSH) &&
            endState.equals(SmcElement.NIL_STATE));
    } // end of isLoopback(int transType, String)

    /**
     * Returns {@code true} if each of the transition guards uses
     * the nil end state.
     * @param guards check if all this transitions use the nil
     * end state.
     * @return {@code true} if each of the transition guards uses
     * the nil end state.
     */
    protected boolean allNilEndStates(List<SmcGuard> guards)
    {
        Iterator<SmcGuard> git;
        SmcGuard guard;
        boolean retcode = true;

        for (git = guards.iterator();
             git.hasNext() && retcode;
            )
        {
            guard = git.next();
            retcode =
                (guard.getTransType() == TransType.TRANS_SET &&
                 (guard.getEndState()).equals("nil"));
        }

        return (retcode);
    } // end of allNilEndStates(List<SmcGuard>)

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Returns the target file name generated from the
     * destination directory, base name and suffix using
     * the target name format.
     * @param targetPath The target directory.
     * @param basename The file's basename sans suffix.
     * @param suffix Append this suffix to the file.
     * @return the absolute target file name.
     */
    public String setTargetFile(final String targetPath,
                                final String basename,
                                final String suffix)
    {
        MessageFormat formatter =
            new MessageFormat(TARGET_PATH_FORMAT);
        Object[] args = new Object[3];

        args[0] = targetPath;
        args[1] = basename;
        if (suffix == null)
        {
            args[2] = sSuffix;
        }
        else
        {
            args[2] = suffix;
        }

        mTargetFile = formatter.format(args);

        return (mTargetFile);
    } // end of setTargetFile(String, String, String)

    /**
     * Sets the target code output destination.
     * @param target the generated target code output stream.
     */
    public void setTarget(final PrintStream target)
    {
        mTarget = target;
        return;
    } // end of setTarget(PrintStream)

    //
    // end of Set methods.
    //-----------------------------------------------------------

    /**
     * Place a backslash escape character in front of backslashes
     * and doublequotes.
     * @param s Escape this string.
     * @return the backslash escaped string.
     */
    public static String escape(String s)
    {
        String retval;

        if (s.indexOf('\\') < 0 && s.indexOf('"') < 0)
        {
            retval = s;
        }
        else
        {
            StringBuilder buffer =
                new StringBuilder(s.length() * 2);
            int index;
            int length = s.length();
            char c;

            for (index = 0; index < length; ++index)
            {
                c = s.charAt(index);
                if (c == '\\' || c == '"')
                {
                    buffer.append('\\');
                }

                buffer.append(c);
            }

            retval = buffer.toString();
        }

        return (retval);
    } // end of escape(String)

    /**
     * Scopes the state name. If the state is unscoped, then
     * returns "&lt;mapName&gt;.&lt;stateName&gt;". If the state
     * name contains the scope string "::", replaces that with
     * a ".".
     * @param stateName an absolute or relative state name.
     * @param mapName the current map.
     * @return the scoped absolute state name.
     */
    protected String scopeStateName(String stateName,
                                    String mapName)
    {
        return (scopeStateName(stateName, mapName, "."));
    } // end of scopeStateName(String, String)

    /**
     * Scopes the state name. If the state is unscoped, then
     * returns "&lt;mapName&gt;.&lt;stateName&gt;". If the state
     * name contains the scope string "::", replaces that with
     * a {@code ifs}.
     * @param stateName an absolute or relative state name.
     * @param mapName the current map.
     * @param ifs the map name, state name separator.
     * @return the scoped absolute state name.
     */
    protected String scopeStateName(String stateName,
                                    String mapName,
                                    final String ifs)
    {
        final int index = stateName.indexOf("::");
        String mName = mapName;
        String sName = stateName;

        // If the index is > 0, then a map name was provided.
        if (index > 0)
        {
            mName = stateName.substring(0, index);
        }

        // If state name is of the form "::name", then that means
        // the state is in the current map.
        if (index >= 0)
        {
            sName = stateName.substring(index + 2);
        }

        // Else this is a relative state name. Use the map and
        // state names as given.

        return (mName + ifs + sName);
    } // end of scopeStateName(String, String, String)

    /**
     * Returns a relative path from directory {@code srdDir} to
     * directory {@code headerDir}. This method assumes that the
     * two directories are different.
     * @param srcDir the target file directory.
     * @param headerDir the header file directory.
     * @return relative path from {@code srcDir} to
     * {@code headerDir}.
     */
    protected String findPath(final String srcDir,
                              final String headerDir)
    {
        final File dir0 = new File(srcDir);
        final File dir1 = new File(headerDir);
        String path0 = "";
        String path1 = "";

        try
        {
            final String ifs =
                Pattern.quote(File.separator);
            final String[] abs0 =
                (dir0.getCanonicalPath()).split(ifs);
            final String[] abs1 =
                (dir1.getCanonicalPath()).split(ifs);
            final int minSize =
                (abs0.length < abs1.length ?
                 abs0.length :
                 abs1.length);
            int index;
            boolean flag;

            for (index = 0, flag = true;
                 index < minSize && flag;
                 ++index)
            {
                flag = abs0[index].equals(abs1[index]);
            }

            // Did we find a divergence between the two paths?
            if (flag == false)
            {
                // Yes. So back up the index by one to get to the
                // most common parent.
                --index;

                // Generate the "backup" path from the first
                // directory back to this most common parent.
                path0 = backupPath(index, abs0.length);
                path1 = generatePath(index, abs1);
            }
            // No, the two directories lie along exactly the same
            // absolute path but they are not the same directory.
            // Instead, one directory is a subdirectory of
            // another.
            // Is dir1 the subdirectory?
            else if (abs1.length > abs0.length)
            {
                // Yes, dir1 is the subdirectory.
                path0 = "";
                path1 = generatePath(index, abs1);
            }
            // Is dir0 the subdirectory?
            else if (abs0.length > abs1.length)
            {
                // Yes, dir0 is the subdirectory. Then back up
                // from dir0.
                path0 = backupPath(index, abs0.length);
                path1 = "";
            }
            // Else the target and header directories are the
            // same.
        }
        catch (IOException ioex)
        {
            path0 =
                "ERROR calling java.io.File.getCanonicalPath: ";
            path1 = ioex.getMessage();
        }

        return (path0 + path1);
    } // end of findPath(File, File)

    /**
     * Returns a path which backs up from the target target file
 directory to the most code parent directory.
     * @param index the path list index of the most common parent
     * directory.
     * @param length the path list length.
     * @return the path from the target target file directory to
 the most common parent directory.
     */
    private static String backupPath(int index, final int length)
    {
        final StringBuilder retval = new StringBuilder();
        int i;
        String sep = "";

        for (i = index; i < length; ++i, sep = File.separator)
        {
            retval.append(sep);
            retval.append(BACKDIR);
        }

        retval.append(File.separator);

        return (retval.toString());
    } // end of backupPath(int, int)

    /**
     * Returns the relative path from the most common parent
     * directory to the header file directory.
     * @param index the path list index of the most common parent
     * directory.
     * @param path the absolute path to the header file
     * directory.
     * @return relative path from the most common parent
     * directory to the header file directory.
     */
    private static String generatePath(int index,
                                       final String[] path)
    {
        final StringBuilder retval = new StringBuilder();
        int i;
        String sep = "";

        for (i = index; i < path.length; ++i, sep = File.separator)
        {
            retval.append(sep);
            retval.append(path[index]);
        }

        retval.append(File.separatorChar);

        return (retval.toString());
    } // end of generatePath(int, String[])
} // end of class SmcCodeGenerator

//
// CHANGE LOG
// Log: SmcCodeGenerator.java,v
// Revision 1.8  2013/09/02 14:45:57  cwrapp
// SMC 6.3.0 commit.
//
// Revision 1.7  2013/07/14 14:32:38  cwrapp
// check in for release 6.2.0
//
// Revision 1.6  2010/02/15 18:05:43  fperrad
// fix 2950619 : make distinction between target filename (*.sm) and target filename.
//
// Revision 1.5  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.4  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.3  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.2  2009/03/16 19:45:26  cwrapp
// Corrected isLoopback.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
//
