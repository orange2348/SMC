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
// Copyright (C) 2000 - 2005, 2008. Charles W. Rapp.
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
// $Id: SmcParser.java,v 1.13 2015/08/02 19:44:36 cwrapp Exp $
//
// CHANGE LOG
// (See bottom of file.)
//

package net.sf.smc.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.TargetLanguage;

/**
 * Reads in a finite state machine definition from an input
 * stream and returns the
 * {@link net.sf.smc.model.SmcFSM FSM} model. If
 * {@link #parse} throws an exception, then call
 * {@link #getMessages} for a list of parser warning and error
 * messages which explain the problems found with the
 * FSM definition. A new parser instance must be instantiated for
 * each unique input stream. A parser instance cannot be reused
 * for a different stream.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcParser
{
//---------------------------------------------------------------
// Member Data
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * SMC currently supports 17 different target languages plus
     * one for an error code and plus another for a second
     * Java implementation.
     */
    public static final int LANGUAGE_COUNT = 19;

    //-----------------------------------------------------------
    // Statics.
    //

    // List of characters which open and clause subexpressions.
    private static List<Character> OPEN_CLAUSE_LIST;
    private static List<Character> CLOSE_CLAUSE_LIST;
    private static List<Character> QUOTE_LIST;

    // Create a hashmap which associates token names with
    // parser transitions. When a token is received, use this
    // table to get the appropriate transition method and
    // invoke that method.
    private static Method[] sTransMethod;

    static
    {
        String transName = "<not set>";

        OPEN_CLAUSE_LIST = new ArrayList<>();
        CLOSE_CLAUSE_LIST = new ArrayList<>();
        QUOTE_LIST = new ArrayList<>();

        OPEN_CLAUSE_LIST.add(new Character('('));
        OPEN_CLAUSE_LIST.add(new Character('{'));
        OPEN_CLAUSE_LIST.add(new Character('['));
        OPEN_CLAUSE_LIST.add(new Character('<'));

        CLOSE_CLAUSE_LIST.add(new Character(')'));
        CLOSE_CLAUSE_LIST.add(new Character('}'));
        CLOSE_CLAUSE_LIST.add(new Character(']'));
        CLOSE_CLAUSE_LIST.add(new Character('>'));

        QUOTE_LIST.add(new Character('"'));
        QUOTE_LIST.add(new Character('\''));

        sTransMethod = new Method[SmcLexer.TOKEN_COUNT];

        try
        {
            Class<SmcParserContext> fsmClass =
                SmcParserContext.class;
            Class[] paramTypes = new Class[1];

            paramTypes[0] = SmcLexer.Token.class;

            transName = "ENTRY";
            sTransMethod[SmcLexer.ENTRY] =
                fsmClass.getDeclaredMethod("ENTRY",
                                           paramTypes);
            transName = "EXIT";
            sTransMethod[SmcLexer.EXIT] =
                fsmClass.getDeclaredMethod("EXIT",
                                           paramTypes);
            transName = "JUMP";
            sTransMethod[SmcLexer.JUMP] =
                fsmClass.getDeclaredMethod("JUMP",
                                           paramTypes);
            transName = "POP";
            sTransMethod[SmcLexer.POP] =
                fsmClass.getDeclaredMethod("POP",
                                           paramTypes);
            transName = "PUSH";
            sTransMethod[SmcLexer.PUSH] =
                fsmClass.getDeclaredMethod("PUSH",
                                           paramTypes);
            transName = "WORD";
            sTransMethod[SmcLexer.WORD] =
                fsmClass.getDeclaredMethod("WORD",
                                           paramTypes);
            transName = "START_STATE";
            sTransMethod[SmcLexer.START_STATE] =
                fsmClass.getDeclaredMethod("START_STATE",
                                           paramTypes);
            transName = "MAP_NAME";
            sTransMethod[SmcLexer.MAP_NAME] =
                fsmClass.getDeclaredMethod("MAP_NAME",
                                           paramTypes);
            transName = "CLASS_NAME";
            sTransMethod[SmcLexer.CLASS_NAME] =
                fsmClass.getDeclaredMethod("CLASS_NAME",
                                           paramTypes);
            transName = "HEADER_FILE";
            sTransMethod[SmcLexer.HEADER_FILE] =
                fsmClass.getDeclaredMethod("HEADER_FILE",
                                           paramTypes);
            transName = "INCLUDE_FILE";
            sTransMethod[SmcLexer.INCLUDE_FILE] =
                fsmClass.getDeclaredMethod("INCLUDE_FILE",
                                           paramTypes);
            transName = "PACKAGE_NAME";
            sTransMethod[SmcLexer.PACKAGE_NAME] =
                fsmClass.getDeclaredMethod("PACKAGE_NAME",
                                           paramTypes);
            transName = "FSM_CLASS_NAME";
            sTransMethod[SmcLexer.FSM_CLASS_NAME] =
                fsmClass.getDeclaredMethod("FSM_CLASS_NAME",
                                           paramTypes);
            transName = "FSM_FILE_NAME";
            sTransMethod[SmcLexer.FSM_FILE_NAME] =
                fsmClass.getDeclaredMethod("FSM_FILE_NAME",
                                           paramTypes);
            transName = "IMPORT";
            sTransMethod[SmcLexer.IMPORT] =
                fsmClass.getDeclaredMethod("IMPORT",
                                           paramTypes);
            transName = "DECLARE";
            sTransMethod[SmcLexer.DECLARE] =
                fsmClass.getDeclaredMethod("DECLARE",
                                           paramTypes);
            transName = "LEFT_BRACE";
            sTransMethod[SmcLexer.LEFT_BRACE] =
                fsmClass.getDeclaredMethod("LEFT_BRACE",
                                           paramTypes);
            transName = "RIGHT_BRACE";
            sTransMethod[SmcLexer.RIGHT_BRACE] =
                fsmClass.getDeclaredMethod("RIGHT_BRACE",
                                           paramTypes);
            transName = "LEFT_BRACKET";
            sTransMethod[SmcLexer.LEFT_BRACKET] =
                fsmClass.getDeclaredMethod("LEFT_BRACKET",
                                           paramTypes);
            transName = "LEFT_PAREN";
            sTransMethod[SmcLexer.LEFT_PAREN] =
                fsmClass.getDeclaredMethod("LEFT_PAREN",
                                           paramTypes);
            transName = "RIGHT_PAREN";
            sTransMethod[SmcLexer.RIGHT_PAREN] =
                fsmClass.getDeclaredMethod("RIGHT_PAREN",
                                           paramTypes);
            transName = "COMMA";
            sTransMethod[SmcLexer.COMMA] =
                fsmClass.getDeclaredMethod("COMMA",
                                           paramTypes);
            transName = "COLON";
            sTransMethod[SmcLexer.COLON] =
                fsmClass.getDeclaredMethod("COLON",
                                           paramTypes);
            transName = "SEMICOLON";
            sTransMethod[SmcLexer.SEMICOLON] =
                fsmClass.getDeclaredMethod("SEMICOLON",
                                           paramTypes);
            transName = "SOURCE";
            sTransMethod[SmcLexer.SOURCE] =
                fsmClass.getDeclaredMethod("SOURCE",
                                           paramTypes);
            transName = "EOD";
            sTransMethod[SmcLexer.EOD] =
                fsmClass.getDeclaredMethod("EOD",
                                           paramTypes);
            transName = "SLASH";
            sTransMethod[SmcLexer.SLASH] =
                fsmClass.getDeclaredMethod("SLASH",
                                           paramTypes);
            transName = "EQUAL";
            sTransMethod[SmcLexer.EQUAL] =
                fsmClass.getDeclaredMethod("EQUAL",
                                           paramTypes);
            transName = "ACCESS";
            sTransMethod[SmcLexer.ACCESS] =
                fsmClass.getDeclaredMethod("ACCESS",
                                           paramTypes);
            transName = "DOLLAR";
            sTransMethod[SmcLexer.DOLLAR] =
                fsmClass.getDeclaredMethod("DOLLAR",
                                           paramTypes);
        }
        catch (NoSuchMethodException ex1)
        {
            System.err.println("INITIALIZATION ERROR! No such " +
                               "method as SmcParserContext." +
                               transName +
                               ".");
            System.exit(2);
        }
        catch (SecurityException ex2)
        {
            System.err.println("INITIALIZATION ERROR! Not " +
                               "allowed to access " +
                               "SmcParserContext." +
                               transName +
                               ".");
            System.exit(2);
        }
    } // end of static

    //-----------------------------------------------------------
    // Locals.
    //

    // The FSM name.
    private final String mName;

    // The target language affects how the SMC code is parsed.
    private final TargetLanguage mTargetLanguage;

    // Store warning and error messages into this list. Do not
    // output them. That is up to the application.
    private final List<SmcMessage> mMessages;

    // The parse state map.
    private final SmcParserContext mParserFSM;

    // Get tokens from the lexer.
    private final SmcLexer mLexer;

    // Keep track of errors.
    private boolean mParseStatus;
    private boolean mQuitFlag;

    // Store the parse result here.
    private SmcFSM mFsm;

    private SmcMap mMapInProgress;
    private SmcState mStateInProgress;
    private String mTransitionName;
    private SmcTransition mTransitionInProgress;
    private SmcGuard mGuardInProgress;
    private SmcParameter mParamInProgress;
    private SmcAction mActionInProgress;
    private String mArgInProgress;

    // Store parsed parameters here.
    private List<SmcParameter> mParamList;

    // Store parsed transition actions here.
    private List<SmcAction> mActionList;

    // Store parsed action arguments here.
    private List<String> mArgList;

    // The current line being parsed.
    private int mLineNumber;

    // Maps unique transition name, parameter list key to the
    // transition's assigned identifier.
    private final Map<TransitionKey, Integer> mTransitions;

    // Use this value to assign the next transition identifier.
    // Initial to one because zero is reserved for the default
    // transition.
    private int mNextTransitionId;

//---------------------------------------------------------------
// Member Methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a parser for the named FSM in the given input
     * stream. If <code>debugFlag</code> is <code>true</code>,
     * then the parser and lexer debug output will be generated.
     * @param name the finite state machine's name.
     * @param istream the input stream contains the SMC code.
     * @param targetLanguage Generates code for this target
     * language.
     * @param debugFlag if true, turn on debug output.
     */
    public SmcParser(String name,
                     InputStream istream,
                     TargetLanguage targetLanguage,
                     boolean debugFlag)
    {
        mName = name;
        mTargetLanguage = targetLanguage;
        mMessages = new ArrayList<>();
        mTransitions = new HashMap<>();
        mNextTransitionId = 1;

        mLexer = new SmcLexer(istream, debugFlag);
        mParserFSM = new SmcParserContext(this);
        mParserFSM.setDebugFlag(debugFlag);
    } // end of SmcParser(...)

    //
    // Constructors.
    //-----------------------------------------------------------

    /**
     * Parses the named FSM in the given input stream and returns
     * the finite state machine. If this method throws an
     * exception, then call {@link #getMessages} for a list of
     * parser warnings and errors which explain the problems
     * found in the FSM defintion.
     * @return the parser FSM model.
     * @exception IOException
     * if there is a problem reading the input stream.
     * @exception IllegalAccessException
     * if there is a problem accessing the input stream.
     * @exception InvocationTargetException
     * if there is a parse error.
     */
    public SmcFSM parse()
        throws IOException,
               IllegalAccessException,
               InvocationTargetException
    {
        SmcLexer.Token token = null;
        int tokenType;
        Object[] params = new Object[1];

        mMapInProgress = null;
        mStateInProgress = null;
        mTransitionName = null;
        mTransitionInProgress = null;
        mGuardInProgress = null;
        mParamInProgress = null;
        mActionInProgress = null;
        mArgInProgress = null;

        mParamList = null;
        mActionList = null;
        mArgList = null;

        mParseStatus = true;
        mQuitFlag = false;

        mFsm = new SmcFSM(mName,
                          mTargetLanguage.targetFileName(mName));

        // Start lexing in cooked mode.
        mLexer.setCookedMode();

        // Read all the tokens into a list.
        while (!mQuitFlag && (token = mLexer.nextToken()) != null)
        {
            tokenType = token.getType();
            mLineNumber = token.getLineNumber();

            // Is the token type valid?
            if (tokenType <= SmcLexer.TOKEN_NOT_SET &&
                tokenType >= SmcLexer.TOKEN_COUNT)
            {
                // No.
                error("Undefined token type (" +
                      Integer.toString(tokenType) +
                      ")",
                      token.getLineNumber());

                mQuitFlag = true;
                mParseStatus = false;
            }
            // If the last token is a failure, don't go on.
            else if (tokenType == SmcLexer.DONE_FAILED)
            {
                mQuitFlag = true;
                mParseStatus = false;
                error(token.getValue(), token.getLineNumber());
            }
            // If the last token is success, don't go on either.
            else if (tokenType == SmcLexer.DONE_SUCCESS)
            {
                mQuitFlag = true;
            }
            else
            {
                // Issue a transition for this token.
                params[0] = token;
                sTransMethod[tokenType].invoke(mParserFSM,
                                                params);
            }
        }

        // If the parse failed, delete the tree.
        if (!mParseStatus)
        {
            mFsm = null;
        }

        return (mFsm);
    } // end of parse()

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the parser's warning and error messages list.
     * @return the parser's warning and error messages list.
     */
    public List<SmcMessage> getMessages()
    {
        return (mMessages);
    } // end of getMessages()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // State Machine Guards
    //

    /* package */ boolean isValidHeader()
    {
        String context = mFsm.getContext();
        String start = mFsm.getStartState();

        return (context != null &&
                context.length() > 0 &&
                start != null &&
                start.length() > 0);
    } // end of isValidHeader()

    /* package */ boolean isValidStartState(String name)
    {
        int index;
        boolean retval = false;

        // The name must be of the form "<id>::<id>".
        index = name.indexOf("::");

        // Fail if "::" does not appear at all or appears
        // more than once.
        if (index >= 0 && name.indexOf("::", (index + 1)) < 0)
        {
            // Given how the lexer works, we are guaranteed
            // that the two identifiers are valid.
            retval = true;
        }

        return (retval);
    } // end of isValidStartState(String)

    /* package */ boolean isDuplicateMap(String name)
    {
        return (mFsm.findMap(name) != null);
    } // end of isDuplicateMap(String)

    /* package */ boolean isDuplicateState(String name)
    {
        return (mMapInProgress.isKnownState(name));
    } // end of isDuplicateState(String)

    /**
     * Returns {@code true} if the in-progress state is the
     * default state and {@code false} otherwise.
     * @return {@code true} if the in-progress state is the
     * default state.
     */
    /* package */ boolean isDefaultState()
    {
        return (mStateInProgress.isDefaultState());
    } // end of isDefaultState()

    //
    // end of State Machine Guards
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // State Machine Actions
    //

    /* package */ void warning(String errorMsg, int lineNumber)
    {
        mMessages.add(
            new SmcMessage(mName,
                           lineNumber,
                           SmcMessage.WARNING,
                           errorMsg));
        return;
    } // end of warning(String, int)

    /* package */ void error(String errorMsg, int lineNumber)
    {
        mMessages.add(
            new SmcMessage(mName,
                           lineNumber,
                           SmcMessage.ERROR,
                           errorMsg));

        mParseStatus = false;

        return;
    } // end of error(String, int)

    /* package */ int getLineNumber()
    {
        return (mLineNumber);
    }

    /* package */ TargetLanguage getTargetLanguage()
    {
        return (mTargetLanguage);
    }

	// THIS METHOD WAS ADDED BY kgreg99 ONLY TO RESOLVE COMPILATION ERROR
	// IT HAS TO BE EVALUATED
    // Put the lexer into raw mode.
    /* package */ void setRawMode(String openChar,
                                  String closeChar,
                                  String dummy )
    {
        mLexer.setRawMode(openChar.charAt(0),
                          closeChar.charAt(0) );
        return;
    } // end of setRawMode(String, String)

    // Put the lexer into raw mode.
    /* package */ void setRawMode(String openChar,
                                  String closeChar)
    {
        mLexer.setRawMode(openChar.charAt(0),
                          closeChar.charAt(0));
        return;
    } // end of setRawMode(String, String)

    // Put the lexer into the raw mode used for
    // collecting parameter types.
    /* package */ void setRawMode2()
    {
        mLexer.setRawMode(OPEN_CLAUSE_LIST,
                          CLOSE_CLAUSE_LIST,
                          QUOTE_LIST,
                          ')',
                          ',');

        return;
    } // end of setRawMode2()

    // Put the lexer into the raw mode used for collecting
    // parameter types.
    /* package */ void setRawMode(String closeChars)
    {
        mLexer.setRawMode(closeChars);
        return;
    } // end of setRawMode(String)

    // Put the lexer into cooked mode.
    /* package */ void setCookedMode()
    {
        mLexer.setCookedMode();
        return;
    } // end of setCookedMode()

    /* package */ void setHeaderLine(int lineNumber)
    {
        mFsm.setHeaderLine(lineNumber);
        return;
    } // end of setHeaderList(int)

    /* package */ void setSource(String source)
    {
        String src = mFsm.getSource();

        if (src != null && src.length() > 0)
        {
            warning("%{ %} source previously specified, new " +
                    "source ignored.",
                    mLineNumber);
        }
        else
        {
            mFsm.setSource(source);
        }

        return;
    } // end of setSource(String)

    /* package */ void setStartState(String stateName)
    {
        String start = mFsm.getStartState();

        if (start != null && start.length() > 0)
        {
            warning("%start previously specified, new start " +
                    "state ignored.",
                    mLineNumber);
        }
        else
        {
            mFsm.setStartState(stateName);
        }

        return;
    }

    /* package */ void setContext(String name)
    {
        String context = mFsm.getContext();

        if (context != null && context.length() > 0)
        {
            warning("%class previously specified, new context " +
                    "ignored.",
                    mLineNumber);
        }
        else
        {
            mFsm.setContext(name);
        }

        return;
    }

    /* package */ void setFsmClassName(String name)
    {
        mFsm.setFsmClassName(name.trim());
        return;
    }

    /* package */ void setFsmFileName(String name)
    {
        mFsm.setFsmTargetFile(name.trim());
        return;
    }

    /* package */ void setPackageName(String name)
    {
        String pkg = mFsm.getPackage();

        if (pkg != null && pkg.length() > 0)
        {
            warning("%package previously specified, " +
                    "new package ignored.",
                    mLineNumber);
        }
        else
        {
            mFsm.setPackage(name.trim());
        }

        return;
    }

    /* package */ void addImport(String name)
    {
        mFsm.addImport(name.trim());
        return;
    }

    /* package */ void addDeclare(String name)
    {
        mFsm.addDeclare(name.trim());
        return;
    }

    /* package */ void setHeader(String name)
    {
        String header = mFsm.getHeader();

        if (header != null && header.length() > 0)
        {
            warning("%header previously specified, " +
                    "new header file ignored.",
                    mLineNumber);
        }
        else
        {
            mFsm.setHeader(name.trim());
        }

        return;
    }

    /* package */ void addInclude(String name)
    {
        mFsm.addInclude(name.trim());
        return;
    }

    /* package */ void setAccessLevel(String level)
    {
        String accessLevel = mFsm.getAccessLevel();

        if (accessLevel != null && accessLevel.length() > 0)
        {
            warning("%access previously specified, " +
                    "new access level ignored.",
                    mLineNumber);
        }
        else
        {
            mFsm.setAccessLevel(level.trim());
        }

        return;
    }

    /* package */ void addMap()
    {
        if (mMapInProgress == null)
        {
            error("There is no in-progress map to add",
                  mLineNumber);
        }
        else
        {
            // If this map does not have a default state, then
            // create one now.
            if (!mMapInProgress.hasDefaultState())
            {
                SmcState DefaultState =
                    new SmcState(
                        "Default",
                        mMapInProgress.getLineNumber(),
                        mMapInProgress);

                mMapInProgress.addState(DefaultState);
            }

            mFsm.addMap(mMapInProgress);
            mMapInProgress = null;
        }

        return;
    }

    /* package */ void createMap(String name, int lineNumber)
    {
        if (mMapInProgress != null)
        {
            error("Cannot create new map while still filling " +
                  "in previous map (" +
                  mMapInProgress.getName() +
                  ").",
                  lineNumber);
        }
        else
        {
            if (mParserFSM.getDebugFlag())
            {
                PrintStream os = mParserFSM.getDebugStream();

                os.println("CREATE MAP   : " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            mMapInProgress = new SmcMap(name, lineNumber, mFsm);
        }

        return;
    }

    /* package */ void addState()
    {
        if (mMapInProgress == null)
        {
            error("There is no in-progress map to which the " +
                  "state may be added.",
                  mLineNumber);
        }
        else if (mStateInProgress == null)
        {
            error("There is no in-progrss state to add to the " +
                  "map.",
                  mLineNumber);
        }
        else
        {
            mMapInProgress.addState(mStateInProgress);
            mStateInProgress = null;
        }

        return;
    }

    /* package */ void createState(String name, int lineNumber)
    {
        if (mStateInProgress != null)
        {
            error("Cannot create new state while still " +
                  "filling in previous state (" +
                  mStateInProgress.getName() +
                  ").",
                  lineNumber);
        }
        else
        {
            if (mParserFSM.getDebugFlag())
            {
                PrintStream os = mParserFSM.getDebugStream();

                os.println("CREATE STATE : " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            mStateInProgress =
                new SmcState(name, lineNumber, mMapInProgress);
        }

        return;
    }

    /* package */ void setEntryAction(List<SmcAction> actions)
    {
        // Verify there is an in-progress state.
        if (mStateInProgress == null)
        {
            error("There is no in-progress state to receive " +
                  "the entry action.",
                  mLineNumber);
        }
        else if (mStateInProgress.getEntryActions() != null)
        {
            warning("Entry action previously specified, new " +
                    "entry action ignored.",
                    mLineNumber);
        }
        else
        {
            mStateInProgress.setEntryActions(actions);
        }

        return;
    }

    /* package */ void setExitAction(List<SmcAction> actions)
    {
        // Verify there is an in-progress state.
        if (mStateInProgress == null)
        {
            error("There is no in-progress state to receive " +
                  "the exit action.",
                  mLineNumber);
        }
        else if (mStateInProgress.getExitActions() != null)
        {
            warning("Exit action previously specified, new " +
                    "exit action ignored.",
                    mLineNumber);
        }
        else
        {
            mStateInProgress.setExitActions(actions);
        }

        return;
    }

    // Append the in-progress transition to the in-progress
    // state's transition list.
    /* package */ void addTransition()
    {
        if (mStateInProgress == null)
        {
            error("There is no in-progress state to which the " +
                  "transition may be added.",
                  mLineNumber);
        }
        else if (mTransitionInProgress == null)
        {
            error("There is no in-progress transition to add " +
                  "to the state.",
                  mLineNumber);
        }
        else
        {
            mStateInProgress.addTransition(
                mTransitionInProgress);
            mTransitionInProgress = null;
        }

        return;
    }

    // Returns the stored transition name.
    /* package */ String getTransitionName()
    {
        return (mTransitionInProgress != null ?
                mTransitionInProgress.getName() :
                mTransitionName);
    }

    // Store away the transition's name for later use in
    // creating the transition.
    /* package */ void storeTransitionName(String name)
    {
        if (mTransitionName != null)
        {
            error("There already is a previously stored " +
                  "transition name - \"" +
                  name +
                  "\".",
                  mLineNumber);
        }
        else
        {
            mTransitionName = name;
        }

        return;
    }

    // Create a transition object with the current token as its
    // name.
    /* package */ void
        createTransition(List<SmcParameter> params,
                         int lineNumber)
    {
        if (mTransitionInProgress != null)
        {
            error("Cannot create new transition while still " +
                  "filling in previous transition (" +
                  mTransitionInProgress.getName() +
                  ").",
                  lineNumber);
        }
        else if (mStateInProgress == null)
        {
            error("There is no in-progress state to which the " +
                  "transition may be added.",
                  lineNumber);
        }
        else if (mTransitionName == null)
        {
            error("There is no stored transition name.",
                  lineNumber);
        }
        else
        {
            // Check if this state already has a transition with
            // this name. If so, then reuse that object.
            // Otherwise, create a new transition object.
            mTransitionInProgress =
                mStateInProgress.findTransition(mTransitionName, params);
            if (mTransitionInProgress == null)
            {
                final int transId =
                    getTransitionId(
                        new TransitionKey(
                            mTransitionName, params));

                if (mParserFSM.getDebugFlag())
                {
                    PrintStream os = mParserFSM.getDebugStream();
                    Iterator<SmcParameter> pit;
                    String sep;
                    StringBuffer buffer = new StringBuffer(80);

                    buffer.append("CREATE TRANS : ");
                    buffer.append(mTransitionName);
                    buffer.append('(');

                    for (pit = params.iterator(), sep = "";
                         pit.hasNext();
                         sep = ", ")
                    {
                        buffer.append(sep);
                        buffer.append(pit.next());
                    }

                    buffer.append(')');
                    buffer.append(" (");
                    buffer.append(lineNumber);
                    buffer.append(")");

                    os.println(buffer);
                }

                mTransitionInProgress =
                    new SmcTransition(
                        mTransitionName,
                        params,
                        transId,
                        lineNumber,
                        mStateInProgress);
            }

            mTransitionName = null;
        }

        return;
    }

    // Create a transition object with the current token as its
    // name.
    /* package */ void createTransition(int lineNumber)
    {
        createTransition(new ArrayList<SmcParameter>(), lineNumber);
        return;
    }

    /* package */ void addGuard()
    {
        if (mTransitionInProgress == null)
        {
            error("There is no in-progress transition to " +
                  "which the guard may be added.",
                  mLineNumber);
        }
        else if (mGuardInProgress == null)
        {
            error("There is no in-progress guard to add " +
                  "to the " +
                  mTransitionInProgress.getName() +
                  " transition.",
                  mLineNumber);
        }
        else
        {
            mTransitionInProgress.addGuard(mGuardInProgress);
            mGuardInProgress = null;
        }

        return;
    }

    // Create a guard object with the in-progress action as its
    // condition.
    /* package */ void createGuard(String transition,
                                   String condition,
                                   int lineNumber)
    {
        if (mGuardInProgress != null)
        {
            error("Cannot create new guard while still " +
                  "filling in previous guard.",
                  lineNumber);
        }
        else
        {
            if (mParserFSM.getDebugFlag())
            {
                PrintStream os = mParserFSM.getDebugStream();

                os.println("CREATE GUARD : " +
                           condition +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            mGuardInProgress =
                new SmcGuard(condition,
                             lineNumber,
                             mTransitionInProgress);
        }

        return;
    }

    // Set the in-progress guard's transtion type (set, push or
    // pop).
    /* package */ void setTransType(TransType trans_type)
    {
        if (mGuardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "set the transition type.",
                  mLineNumber);
        }
        else
        {
            switch (trans_type)
            {
                case TRANS_SET:
                case TRANS_PUSH:
                case TRANS_POP:
                    mGuardInProgress.setTransType(trans_type);
                    break;

                default:
                    error("Transition type must be either " +
                          "\"TRANS_SET\", \"TRANS_PUSH\" or " +
                          "\"TRANS_POP\".",
                          mLineNumber);
                    break;
            }
        }

        return;
    }

    // Set the in-progress guard's end state.
    /* package */ void setEndState(String state)
    {
        if (mGuardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the end state.",
                  mLineNumber);
        }
        else
        {
            mGuardInProgress.setEndState(state);
        }

        return;
    }

    // Set the in-progress guard's push state.
    /* package */ void setPushState(String name)
    {
        if (mGuardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the end state.",
                  mLineNumber);
        }
        else if (mGuardInProgress.getTransType() !=
                     TransType.TRANS_PUSH)
        {
            error("Cannot set push state on a non-push " +
                  "transition.",
                  mLineNumber);
        }
        else if (name.equals("nil"))
        {
            error("Cannot push to \"nil\" state.",
                  mLineNumber);
        }
        else
        {
            mGuardInProgress.setPushState(name);
        }

        return;
    }

    // Set the guard's actions.
    /* package */ void setActions(List<SmcAction> actions)
    {
        if (mGuardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the action.",
                  mLineNumber);
        }
        else
        {
            mGuardInProgress.setActions(actions);
        }

        return;
    }

    /* package */ void setPopArgs(String args)
    {
        if (mGuardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the action.",
                  mLineNumber);
        }
        else
        {
            mGuardInProgress.setPopArgs(args);
        }

        return;
    }

    /* package */ void createParamList()
    {
        if (mParamList == null)
        {
            mParamList = new ArrayList<>();
        }

        return;
    }

    /* package */ List<SmcParameter> getParamList()
    {
        List<SmcParameter> retval = mParamList;

        mParamList = null;

        return (retval);
    }

    /* package */ void createParameter(String name, int lineNumber)
    {
        if (mParamInProgress != null)
        {
            error("Cannot create new parameter while still " +
                  "filling in previous one.",
                  lineNumber);
        }
        else
        {
            String type = "";

            if (mParserFSM.getDebugFlag())
            {
                PrintStream os = mParserFSM.getDebugStream();

                os.println("CREATE PARAM : " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            // While Tcl is weakly typed, it still differentiates
            // between call-by-value and call-by-name.
            // By default, SMC generates call-by-value.
            if (mTargetLanguage == TargetLanguage.TCL)
            {
                type = SmcParameter.TCL_VALUE_TYPE;
            }

            mParamInProgress =
                new SmcParameter(name, lineNumber, type);
        }

        return;
    }

    /* package */ void setParamType(String type)
    {
        if (mParamInProgress == null)
        {
            error("There is no in-progress parameter to which " +
                  "to add the type.",
                  mLineNumber);
        }
        else
        {
            mParamInProgress.setType(type);
        }

        return;
    }

    /* package */ void addParameter()
    {
        if (mParamList == null)
        {
            error("There is no parameter list to which the " +
                  "parameter may be added.",
                  mLineNumber);
        }
        else if (mParamInProgress == null)
        {
            error("There is no in-progress parameter to add " +
                  "to the list.",
                  mLineNumber);
        }
        else
        {
            mParamList.add(mParamInProgress);
            mParamInProgress = null;
        }

        return;
    }

    /* package */ void clearParameter()
    {
        mParamInProgress = null;
        return;
    }

    /* package */ void createActionList()
    {
        if (mActionList != null)
        {
            error("Cannot create an action list when one " +
                  "already exists.",
                  mLineNumber);
        }
        else
        {
            mActionList = new ArrayList<>();
        }

        return;
    }

    /* package */ List<SmcAction> getActionList()
    {
        List<SmcAction> retval = mActionList;

        mActionList = null;

        return(retval);
    }

    /* package */ void createAction(String name, int lineNumber)
    {
        if (mActionInProgress != null)
        {
            error("Cannot create new action while still " +
                  "filling in previous one.",
                  lineNumber);
        }
        else
        {
            if (mParserFSM.getDebugFlag())
            {
                PrintStream os = mParserFSM.getDebugStream();

                os.println("CREATE ACTION: " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            mActionInProgress =
                new SmcAction(name, lineNumber);
        }

        return;
    }

    /* package */ void setActionArgs(List<String> args)
    {
        if (mActionInProgress == null)
        {
            error("There is no in-progress action to which to " +
                  "add the arguments.",
                  mLineNumber);
        }
        else
        {
            mActionInProgress.setArguments(args);
        }

        return;
    }

    /* package */ void addAction()
    {
        if (mActionList == null)
        {
            error("There is no action list to which the " +
                  "action may be added.",
                  mLineNumber);
        }
        else if (mActionInProgress == null)
        {
            error("There is no in-progress action to add to " +
                  "the list.",
                  mLineNumber);
        }
        else
        {
            mActionList.add(mActionInProgress);
            mActionInProgress = null;
        }

        return;
    }

    // Retrieve the current action's property assignment flag.
    /* package */ boolean getProperty()
    {
        boolean retcode = false;

        if (mActionInProgress == null)
        {
            error("There is no in-progress action, " +
                  "get property flag failed.",
                  mLineNumber);
        }
        else
        {
            retcode = mActionInProgress.isProperty();
        }

        return (retcode);
    }

    // Mark the current action as a .Net assignment.
    /* package */ void setProperty(boolean flag)
    {
        if (mActionInProgress == null)
        {
            error("There is no in-progress action, " +
                  "set property flag failed.",
                  mLineNumber);
        }
        else
        {
            mActionInProgress.setProperty(flag);
        }

        return;
    }

    /* package */ void clearActions()
    {
        if (mActionList != null)
        {
            mActionList.clear();
            mActionList = null;
        }

        return;
    }

    /* package */ void createArgList()
    {
        if (mArgList != null)
        {
            error("Cannot create an argument list when one " +
                  "already exists.",
                  mLineNumber);
        }
        else
        {
            mArgList = new ArrayList<>();
        }

        return;
    }

    /* package */ List<String> getArgsList()
    {
        List<String> retval = mArgList;

        mArgList = null;

        return(retval);
    }

    /* package */ void
        createArgument(String name, int lineNumber)
    {
        if (mArgInProgress != null)
        {
            error("Cannot create new argument while still " +
                  "filling in previous one.",
                  lineNumber);
        }
        else
        {
            if (mParserFSM.getDebugFlag())
            {
                PrintStream os = mParserFSM.getDebugStream();

                os.println("   CREATE ARG: " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            mArgInProgress = name;
        }

        return;
    } // end of createArgument(String, int)

    /* package */ void addArgument()
    {
        if (mArgList == null)
        {
            error("There is no argument list to which the " +
                  "argument may be added.",
                  mLineNumber);
        }
        else if (mArgInProgress == null)
        {
            error("There is no in-progress argument to add to " +
                  "the list.",
                  mLineNumber);
        }
        else
        {
            mArgList.add(mArgInProgress.trim());
            mArgInProgress = null;
        }

        return;
    } // end of addArgument()

    /* package */ void clearArguments()
    {
        if (mArgList != null)
        {
            mArgList.clear();
            mArgList = null;
        }

        return;
    } // end of clearArguments()

    //
    // end of State Machine Actions
    //-----------------------------------------------------------

    // Returns the unique integer value assigned to the given
    // transition name, parameters key. If there is no previously
    // assigned value, then assigns one now.
    private int getTransitionId(final TransitionKey transKey)
    {
        Integer retval = mTransitions.get(transKey);

        if (retval == null)
        {
            retval = mNextTransitionId;
            ++mNextTransitionId;

            mTransitions.put(transKey, retval);
        }

        return (retval);
    } // end of getTransitionId(TransitionKey)

//---------------------------------------------------------------
// Inner classes.
//

    /**
     * This immutable class is used to map a unique transition
     * name, parameter list pair to its unique integer
     * identifier. This is needed to support -java7 and
     * overloaded transition names.
     * <p>
     * The associated parameter integer identifier is stored
     * in {@code #_transitions} map.
     * </p>
     */
    private static final class TransitionKey
        implements Comparable<TransitionKey>
    {
    //-----------------------------------------------------------
    // Member data.
    //

        //-------------------------------------------------------
        // Locals.
        //

        // The transition name.
        private final String mName;

        // The transition parameters.
        private final List<SmcParameter> mParameters;

    //-----------------------------------------------------------
    // Member methods.
    //

        //-------------------------------------------------------
        // Constructors.
        //

        // Creates a new transition key for the given name and
        // parameters.
        // Note: not argument checking is performed.
        private TransitionKey(final String name,
                              final List<SmcParameter> params)
        {
            mName = name;
            mParameters = params;
        } // end of TransitionKey(String, List<>)

        //
        // end of Constructors.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Comparable Interface Implementation.
        //

        /**
         * Returns an interger value &lt;, equal to or &gt; than
         * zero if {@code this} transition key is &lt;, equal to
         * or &gt; than {@code key}. This compararison is based
         * on the transition name first and, if equal, then on
         * the parameters.
         * @param key the compared transition instance.
         * @return an interger value &lt;, equal to or &gt; than
         * zero if {@code this} transition is &lt;, equal to or
         * &gt; than {@code key}.
         */
        @Override
        public int compareTo(final TransitionKey key)
        {
            int retval = mName.compareTo(key.mName);

            if (retval == 0)
            {
                retval =
                    SmcTransition.compareParams(
                        mParameters, key.mParameters);
            }

            return (retval);
        } // end of compareTo(TransitionKey)

        //
        // end of Comparable Interface Implementation.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Object Method Overrides.
        //

        /**
         * Returns {@code true} if {@code obj} is a
         * non-{@code null TransitionKey} instance with the same
         * name and parameters; {@code false} otherwise.
         * <p>
         * This class does <em>not</em> override
         * {@link Object#hashCode}.
         * </p>
         * @param obj the compared object.
         * @return {@code true} if {@code obj} is a
         * non-{@code null TransitionKey} instance with the same
         * name and parameters.
         */
        @Override
        public boolean equals(final Object obj)
        {
            boolean retcode = (this == obj);

            if (!retcode && obj instanceof TransitionKey)
            {
                final TransitionKey key = (TransitionKey) obj;

                retcode =
                    (mName.equals(key.mName) &&
                     SmcTransition.compareParams(
                         mParameters, key.mParameters) == 0);
            }

            return (retcode);
        } // end of equals(Object)

        @Override
        public int hashCode()
        {
            return (mName.hashCode() ^ mParameters.hashCode());
        }

        /**
         * Returns the transition text representation.
         * @return the transition text representation.
         */
        @Override
            public String toString()
        {
            final StringBuffer retval = new StringBuffer(512);
            String sep;
            Iterator<SmcParameter> pit;

            retval.append(mName);
            retval.append("(");

            for (pit = mParameters.iterator(), sep = "";
                 pit.hasNext();
                 sep = ", ")
            {
                retval.append(sep);
                retval.append(pit.next());
            }

            retval.append(")");

            return (retval.toString());
        } // end of toString()

        //
        // end of Object Method Overrides.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Get Methods.
        //

        public String name()
        {
            return (mName);
        } // end of name()

        public List<SmcParameter> parameters()
        {
            return (mParameters);
        } // end of parameters()

        //
        // end of Get Methods.
        //-------------------------------------------------------
    } // end of class TransitionKey
} // end of class SmcParser

//
// CHANGE LOG
// $Log: SmcParser.java,v $
// Revision 1.13  2015/08/02 19:44:36  cwrapp
// Release 6.6.0 commit.
//
// Revision 1.12  2015/02/16 21:43:09  cwrapp
// SMC v. 6.5.0
//
// SMC - The State Machine Compiler v. 6.5.0
//
// Major changes:
//
// (Java)
//     Added a new "-java7" target language. This version represents
//     the FSM as a transition table. The transition table maps the
//     current state and the transition to a
//     java.lang.invoke.MethodHandle. The transition is executed by
//     calling MethodHandle.invokeExact, which is only slightly
//     slower than a compiled method call.
//
//     The -java7 generated code is compatible with -java generated
//     code. This allows developers to switch between the two
//     without changing application code.
//
//     NOTE: -java7 requires Java 1.7 or latter to run.
//
//
// Minor changes:
//
// (None.)
//
//
// Bug Fixes:
//
// (Objective-C)
//     Incorrect initWithOwner body generated. Same fundamental
//     problem as SF bug 200. See below.
//     (SF bug 198)
//
// (Website)
//     Corrected broken link in FAQ page.
//     (SF bug 199)
//
// (C++)
//     Corrected the invalid generated FSM class name.
//     (SF bug 200)
//
// (C)
//     EXIT_STATE() #define macro not generated.
//     (SF bug 201)
//
// (Manual)
//     Corrected examples which showed %fsmclass and %map set to the
//     same name. This is invalid for most target languages since
//     that would mean the nested map class would have the same name
//     as the containing FSM class.
//
//
//
// ++++++++++++++++++++++++++++++++++++++++
//
// If you have any questions or bugs, please surf
// over to http://smc.sourceforge.net and check out
// the discussion and bug forums. Note: you must be
// a SourceForge member to add articles or bugs. You
// do not have to be a member to read posted
// articles or bugs.
//
// Revision 1.11  2013/07/14 14:32:39  cwrapp
// check in for release 6.2.0
//
// Revision 1.10  2011/11/20 16:29:53  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.9  2011/02/14 21:29:56  nitin-nizhawan
// corrected some build errors
//
// Revision 1.7  2010/02/15 18:05:44  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.6  2009/11/27 19:44:39  cwrapp
// Correct TargetLanguage.GRAPH source file name definition.
//
// Revision 1.5  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.4  2009/09/12 21:44:49  kgreg99
// Implemented feature req. #2718941 - user defined generated class name.
// A new statement was added to the syntax: %fsmclass class_name
// It is optional. If not used, generated class is called as before "XxxContext" where Xxx is context class name as entered via %class statement.
// If used, generated class is called asrequested.
// Following language generators are touched:
// c, c++, java, c#, objc, lua, groovy, scala, tcl, VB
// This feature is not tested yet !
// Maybe it will be necessary to modify also the output file name.
//
// Revision 1.3  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.2  2009/04/11 13:11:13  cwrapp
// Corrected raw mode 3 to handle multiple argument template/generic declarations.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.16  2007/11/19 18:53:21  fperrad
// + add : jump syntax
//   jump uses the same syntax as push,
//   allows transition between states of different maps but without stacking a return context.
//
// Revision 1.15  2007/02/21 13:56:09  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.14  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.13  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.12  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.11  2005/09/14 01:51:33  cwrapp
// Changes in release 4.2.0:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (Java) -java broken due to an untested minor change.
//
// Revision 1.10  2005/08/26 15:21:34  cwrapp
// Final commit for release 4.2.0. See README.txt for more information.
//
// Revision 1.9  2005/07/07 12:11:04  fperrad
// Add a new token '$' for Perl language.
//
// Revision 1.8  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the generate Context
// class' accessibility level in Java and C#.
//
// Revision 1.7  2005/06/18 18:28:42  cwrapp
// SMC v. 4.0.1
//
// New Features:
//
// (No new features.)
//
// Bug Fixes:
//
// + (C++) When the .sm is in a subdirectory the forward- or
//   backslashes in the file name are kept in the "#ifndef" in the
//   generated header file. This is syntactically wrong. SMC now
//   replaces the slashes with underscores.
//
// + (Java) If %package is specified in the .sm file, then the
//   generated *Context.java class will have package-level access.
//
// + The Programmer's Manual had incorrect HTML which prevented the
//   pages from rendering correctly on Internet Explorer.
//
// + Rewrote the Programmer's Manual section 1 to make it more
//   useful.
//
// Revision 1.6  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.5  2005/02/21 15:37:43  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.4  2005/02/21 15:19:30  charlesr
// Trimming import, header and include names because they are lexed
// as pure source.
//
// Revision 1.3  2005/02/03 17:04:39  charlesr
// The parser was modified as part of an ongoing project to
// make the SMC parser a self-contained, stand-alone library.
// These changes include:
// + Have SmcParser instantiate the SmcLexer thereby making
//   the lexer entirely encapsulated by the parser.
// + Collecting warning and error messages in SmcMessage
//   objects. The application owning the parser can then call
//   SmcParser.getMessages() and display the messages in the
//   appropriate manner.
// + If the parser is in debug mode, then all output is now
//   guaranteed to be written to the same output stream.
//
// Revision 1.2  2004/09/06 16:41:03  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:56:05  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:05:08  charlesr
// Initial revision
//
