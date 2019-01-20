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
// Copyright (C) 2000 - 2006, 2008. Charles W. Rapp.
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
// Id: SmcLexer.java,v 1.6 2013/07/14 14:32:38 cwrapp Exp
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* package */ final class SmcLexer
{
//---------------------------------------------------------------
// Member Data
//

    //-----------------------------------------------------------
    // Constants.
    //

    // Read in this many bytes at a time into the buffer.
    private static final int MAX_BUFFER_LEN = 4096;

    // Read into the input buffer starting at this offset.
    private static final int BUFFER_OFFSET = 2;

    // The read buffer's allocated size in bytes.
    private static final int READ_BUFFER_LEN =
        MAX_BUFFER_LEN + BUFFER_OFFSET;

    // When in cooked mode, run the characters through the
    // lexer FSM.
    /* package */ static final int COOKED = 1;

    // When in raw mode, collect characters until the ending
    // character is seen.
    // RAW is used to read in all characters between parens,
    // braces, etc. RAW mode will read in an entire .sm file
    // if the parens are mismatched.
    /* package */ static final int RAW = 2;

    // RAW2 reads in all characters between parens or until
    // a comma (or some other separator) is reached.
    /* package */ static final int RAW2 = 3;

    // RAW3 is like RAW but does not match open and closing
    // bracket characters.
    /* package */ static final int RAW3 = 4;

    // Each token type has an integer value. These token type
    // values are package-wide scope so the parser can access
    // them.
    /* package */ static final int TOKEN_NOT_SET =  0;
    /* package */ static final int DONE_FAILED   =  1;
    /* package */ static final int DONE_SUCCESS  =  2;
    /* package */ static final int ENTRY         =  3;
    /* package */ static final int EXIT          =  4;
    /* package */ static final int POP           =  5;
    /* package */ static final int PUSH          =  6;
    /* package */ static final int WORD          =  7;
    /* package */ static final int START_STATE   =  8;
    /* package */ static final int MAP_NAME      =  9;
    /* package */ static final int CLASS_NAME    = 10;
    /* package */ static final int HEADER_FILE   = 11;
    /* package */ static final int INCLUDE_FILE  = 12;
    /* package */ static final int PACKAGE_NAME  = 13;
    /* package */ static final int IMPORT        = 14;
    /* package */ static final int DECLARE       = 15;
    /* package */ static final int LEFT_BRACE    = 16;
    /* package */ static final int RIGHT_BRACE   = 17;
    /* package */ static final int LEFT_BRACKET  = 18;
    // Right bracket is not needed as it is consumed by raw text
    // processing.
    /* package */ static final int LEFT_PAREN    = 19;
    /* package */ static final int RIGHT_PAREN   = 20;
    /* package */ static final int COMMA         = 21;
    /* package */ static final int COLON         = 22;
    /* package */ static final int SEMICOLON     = 23;
    /* package */ static final int SOURCE        = 24;
    /* package */ static final int EOD           = 25;
    /* package */ static final int SLASH         = 26;
    /* package */ static final int EQUAL         = 27;
    /* package */ static final int ACCESS        = 28;
    /* package */ static final int DOLLAR        = 29;
    /* package */ static final int JUMP          = 30;
    /* package */ static final int FSM_CLASS_NAME  = 31;
    /* package */ static final int FSM_FILE_NAME = 32;
    /* package */ static final int TOKEN_COUNT   = FSM_FILE_NAME + 1;

    // There are four SMC keywords: entry, exit, push and pop.
    private static final int KEYWORD_COUNT = 4;

    // There are eight percent keywords.
    private static final int PERCENT_KEYWORD_COUNT = 9;

    // The ASCII characters all have explicit transitions.
    // Unicode characters are simply given the unicode
    // transition.
    private static final int MIN_ASCII_CHAR = 0;
    private static final int MAX_ASCII_CHAR = 128;

    // New line character.
    private static final int NEW_LINE = 10;

    // The escape character.
    private static final char ESCAPE = '\\';

    //-----------------------------------------------------------
    // Statics.
    //

    private static String[] sTypeName;
    private static Map<String, Integer> sKeywordMap;

    // Maps % keywords to an integer value.
    private static Map<String, Integer> sPercentKeywordMap;

    // Create an array which maps ASCII characters to
    // transitions.
    private static Method[] sTransMethod;

    static
    {
        String transName = "<not set>";

        sTypeName = new String[TOKEN_COUNT];
        sTypeName[SmcLexer.TOKEN_NOT_SET] = "TOKEN_NOT_SET";
        sTypeName[SmcLexer.DONE_FAILED]   = "DONE_FAILED";
        sTypeName[SmcLexer.DONE_SUCCESS]  = "DONE_SUCCESS";
        sTypeName[SmcLexer.ENTRY]         = "ENTRY";
        sTypeName[SmcLexer.EXIT]          = "EXIT";
        sTypeName[SmcLexer.POP]           = "POP";
        sTypeName[SmcLexer.PUSH]          = "PUSH";
        sTypeName[SmcLexer.WORD]          = "WORD";
        sTypeName[SmcLexer.START_STATE]   = "START_STATE";
        sTypeName[SmcLexer.MAP_NAME]      = "MAP_NAME";
        sTypeName[SmcLexer.CLASS_NAME]    = "CLASS_NAME";
        sTypeName[SmcLexer.HEADER_FILE]   = "HEADER_FILE";
        sTypeName[SmcLexer.INCLUDE_FILE]  = "INCLUDE_FILE";
        sTypeName[SmcLexer.PACKAGE_NAME]  = "PACKAGE_NAME";
        sTypeName[SmcLexer.FSM_CLASS_NAME] = "FSM_CLASS_NAME";
        sTypeName[SmcLexer.FSM_FILE_NAME] = "FSM_FILE_NAME";
        sTypeName[SmcLexer.IMPORT]        = "IMPORT";
        sTypeName[SmcLexer.DECLARE]       = "DECLARE";
        sTypeName[SmcLexer.LEFT_BRACE]    = "LEFT_BRACE";
        sTypeName[SmcLexer.RIGHT_BRACE]   = "RIGHT_BRACE";
        sTypeName[SmcLexer.LEFT_BRACKET]  = "LEFT_BRACKET";
        // Right bracket is not needed as it is consumed by
        // raw text processing.
        sTypeName[SmcLexer.LEFT_PAREN]    = "LEFT_PAREN";
        sTypeName[SmcLexer.RIGHT_PAREN]   = "RIGHT_PAREN";
        sTypeName[SmcLexer.COMMA]         = "COMMA";
        sTypeName[SmcLexer.COLON]         = "COLON";
        sTypeName[SmcLexer.COLON]         = "SEMICOLON";
        sTypeName[SmcLexer.SOURCE]        = "SOURCE";
        sTypeName[SmcLexer.EOD]           = "EOD";
        sTypeName[SmcLexer.SLASH]         = "SLASH";
        sTypeName[SmcLexer.EQUAL]         = "EQUAL";
        sTypeName[SmcLexer.ACCESS]        = "ACCESS";
        sTypeName[SmcLexer.DOLLAR]        = "DOLLAR";
        sTypeName[SmcLexer.JUMP]          = "JUMP";

        // Set up the keyword |-> token value map.
        sKeywordMap = new HashMap<>(KEYWORD_COUNT);
        sKeywordMap.put("Entry", SmcLexer.ENTRY);
        sKeywordMap.put("Exit", SmcLexer.EXIT);
        sKeywordMap.put("jump", SmcLexer.JUMP);
        sKeywordMap.put("pop", SmcLexer.POP);
        sKeywordMap.put("push", SmcLexer.PUSH);

        // Set up the percent keyword |-> token value map.
        sPercentKeywordMap = new HashMap<>(PERCENT_KEYWORD_COUNT);
        sPercentKeywordMap.put("%start", SmcLexer.START_STATE);
        sPercentKeywordMap.put("%map", SmcLexer.MAP_NAME);
        sPercentKeywordMap.put("%class", SmcLexer.CLASS_NAME);
        sPercentKeywordMap.put("%header", SmcLexer.HEADER_FILE);
        sPercentKeywordMap.put("%include",
                               SmcLexer.INCLUDE_FILE);
        sPercentKeywordMap.put("%package",
                               SmcLexer.PACKAGE_NAME);
        sPercentKeywordMap.put("%fsmclass",
                               SmcLexer.FSM_CLASS_NAME);
        sPercentKeywordMap.put("%fsmfile",
                               SmcLexer.FSM_FILE_NAME);
        sPercentKeywordMap.put("%import", SmcLexer.IMPORT);
        sPercentKeywordMap.put("%declare", SmcLexer.DECLARE);
        sPercentKeywordMap.put("%access", SmcLexer.ACCESS);

        // Set up the transition map.
        sTransMethod = new Method[SmcLexer.MAX_ASCII_CHAR];

        try
        {
            int i;
            Class<SmcLexerContext> fsmClass =
                SmcLexerContext.class;
            Class[] paramTypes = new Class[0];
            Method unicode;
            Method whitespace;
            Method alpha;
            Method digit;

            transName = "unicode";
            unicode =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "whitespace";
            whitespace =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "alpha";
            alpha =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "digit";
            digit =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            // Set all transitions to unicode and then set known
            // characters to other transitions.
            for (i = MIN_ASCII_CHAR; i < MAX_ASCII_CHAR; ++i)
            {
                sTransMethod[i] = unicode;
            }

            // Whitespace characters.
            sTransMethod[ 9] = whitespace;
            sTransMethod[11] = whitespace;
            sTransMethod[12] = whitespace;
            sTransMethod[28] = whitespace;
            sTransMethod[29] = whitespace;
            sTransMethod[30] = whitespace;
            sTransMethod[31] = whitespace;
            sTransMethod[32] = whitespace;

            // New line characters.
            sTransMethod[10] =
                fsmClass.getDeclaredMethod("EOL", paramTypes);
            sTransMethod[13] = sTransMethod[10];

            // Alphabetic characters.
            for (i = 'a'; i <= 'z'; ++i)
            {
                sTransMethod[i] = alpha;
            }

            for (i = 'A'; i <= 'Z'; ++i)
            {
                sTransMethod[i] = alpha;
            }

            // Digits
            for (i = '0'; i <= '9'; ++i)
            {
                sTransMethod[i] = digit;
            }

            // Visible characters.
            transName = "percent";
            sTransMethod['%'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "left_paren";
            sTransMethod['('] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "right_paren";
            sTransMethod[')'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "asterisk";
            sTransMethod['*'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "comma";
            sTransMethod[','] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "period";
            sTransMethod['.'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "slash";
            sTransMethod['/'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "colon";
            sTransMethod[':'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "semicolon";
            sTransMethod[';'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "left_bracket";
            sTransMethod['['] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            // Right bracket is not needed as it is consumed by
            // raw text processing.
            transName = "underscore";
            sTransMethod['_'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "left_brace";
            sTransMethod['{'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
            transName = "right_brace";
            sTransMethod['}'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "equal";
            sTransMethod['='] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "lt";
            sTransMethod['<'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "gt";
            sTransMethod['>'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);

            transName = "dollar";
            sTransMethod['$'] =
                fsmClass.getDeclaredMethod(transName,
                                           paramTypes);
        }
        catch (NoSuchMethodException ex1)
        {
            System.err.println(
                "INITIALIZATION ERROR! No such method as " +
                "SmcLexerContext." +
                transName +
                ".");
            System.exit(1);
        }
        catch (SecurityException ex2)
        {
            System.err.println(
                "INITIALIZATION ERROR! Not allowed to access " +
                "SmcLexerContext." +
                transName +
                ".");
            System.exit(1);
        }
    } // end of static

    //-----------------------------------------------------------
    // Locals.
    //

    // The lexer's state map.
    private final SmcLexerContext mLexerFSM;

    // The file being parsed.
    private final InputStream mStream;

    // Specifies whether this lexer is in raw or cooked mode.
    private int mMode;

    // Stop the event loop - a token has been found.
    private boolean mStopFlag;

    // The character currently being processed.
    private char mCurrentChar;

    // Token name is an int. Token value is a string.
    // Place the token's name and value into the array's first
    // and second position, respectively.
    private final Token mToken;

    // Collect the token in a string buffer before making a
    // string out of it.
    private final StringBuilder mTokenBuffer;

    // Keep track of the source line being parsed. This is needed
    // for error messages.
    private int mLineNumber;

    // Read in a buffer-full of data rather than one character
    // at a time.
    private final char[] mReadBuffer;

    // The actual number of read characters in the buffer.
    // May be less than the buffer's size.
    private int mBufferSize;

    // The next character to be read from the _readBuffer.
    private int mReadIndex;

    // When working in RAW mode, the characters are *not*
    // processed by the FSM. Instead, they are blindly
    // collected until the close character is seen. Store the
    // close character and its matching open character here.

    // Raw mode 1: open and closing characters.
    private char mOpenChar;
    private char mCloseChar;

    // Raw mode 2: List of known opening and closing pairs and
    // ending and separator characters which terminate the raw
    // token collection.
    private List<Character> mOpenList;
    private List<Character> mCloseList;
    private List<Character> mQuoteList;
    private char mEndChar;
    private char mSeparator;

    // Raw mode 3: List of characters terminating the raw token
    // collection.
    private String mCloseChars;

//---------------------------------------------------------------
// Member Methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /* package */ SmcLexer(final InputStream stream,
                           final boolean debugFlag)
    {
        mStream = stream;
        mToken = new Token();
        mTokenBuffer = new StringBuilder();
        mLineNumber = 1;
        mReadBuffer = new char[READ_BUFFER_LEN];
        mBufferSize = 0;
        mReadIndex = 0;

        mLexerFSM = new SmcLexerContext(this);
        mLexerFSM.setDebugFlag(debugFlag);
    } // end of SmcLexer(InputStream, boolean)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    // Return the current line being parsed.
    /* package */ int getLineNumber()
    {
        return(mLineNumber);
    } // end of getLineNumber()

    // Return the next token and its token name.
    /* package */ Token nextToken()
        throws IOException,
               IllegalAccessException,
               InvocationTargetException
    {
        Token retval;

        switch (mMode)
        {
            case COOKED:
                retval = nextCookedToken();
                break;

            case RAW:
                retval = nextRawToken();
                break;

            case RAW2:
                retval = nextParamTypeToken();
                break;

            // Mode RAW3.
            default:
                retval = nextRaw3Token();
        }

        return (retval);
    } // end of nextToken()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    // Put the lexer in raw mode. This means the characters are
    // *not* passed through the FSM.
    /* package */ void setRawMode(final char openChar,
                                  final char closeChar)
    {
        mMode = RAW;
        mOpenChar = openChar;
        mCloseChar = closeChar;

        return;
    } // end of setRawMode(char, char)

    // Put the lexer in raw mode 2. This is used to collect
    // parameter type declarations.
    /* package */ void setRawMode(final List<Character> openList,
                                  final List<Character> closeList,
                                  final List<Character> quoteList,
                                  char endChar,
                                  char separator)
    {
        mMode = RAW2;
        mOpenList = new ArrayList<>(openList);
        mCloseList = new ArrayList<>(closeList);
        mQuoteList = new ArrayList<>(quoteList);
        mEndChar = endChar;
        mSeparator = separator;

        return;
    } // end of setRawMode(List<>, List<>, char, char)

    // Put the lexer in raw mode 3. Collect all characters
    // until the close character is seen.
    /* package */ void setRawMode(final String closeChars)
    {
        mMode = RAW3;
        mCloseChars = closeChars;

        return;
    } // end of setRawMode(String)

    // Put the lexer in cooked mode.
    /* package */ void setCookedMode()
    {
        mMode = COOKED;
        return;
    } // end of setCookedMode()

    //
    // end of Set methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // State Map Actions
    // The following methods are actions in the state map.
    //
    /* package */ void startToken()
    {
        mToken.reset();
        mTokenBuffer.delete(0, mTokenBuffer.length());

        // The token's line number is the line on which the
        // token begins.
        mToken.setLineNumber(mLineNumber);

        return;
    } // end of startToken()

    /* package */ void addToToken()
    {
        mTokenBuffer.append(mCurrentChar);
        return;
    } // end of addToToken()

    /* package */ void outputChar()
    {
        System.out.println();
        System.out.print("Unknown character: 0x");
        System.out.println(Integer.toHexString(mCurrentChar));

        return;
    } // end of outputChar()

    /* package */ void addToToken(final String s)
    {
        mTokenBuffer.append(s);
        return;
    } // end of addToToken(String)

    /* package */ void endToken(final int type)
    {
        mToken.setType(type);
        mToken.setValue(mTokenBuffer.toString());
        mStopFlag = true;
        return;
    } // end of endTokne(int)

    // A malformed token has been detected.
    /* package */ void badToken(final String error_msg)
    {
        mToken.setType(DONE_FAILED);
        mToken.setValue(error_msg +
                        " (token: " +
                        mTokenBuffer.toString() +
                        ")");
        mStopFlag = true;
        return;
    } // end of badToken(String)

    // Check if the token is a keyword. Otherwise, set the token
    // type to WORD.
    /* package */ void checkKeyword()
    {
        String tokenStr = mTokenBuffer.toString();

        mToken.setValue(tokenStr);
        mStopFlag = true;

        if (sKeywordMap.containsKey(tokenStr))
        {
            mToken.setType(sKeywordMap.get(tokenStr));
        }
        else if (tokenStr.length() > 0)
        {
            mToken.setType(SmcLexer.WORD);
        }
        else
        {
            badToken("Zero-length word");
        }

        return;
    } // end of checkKeyword()

    /* package */ void checkPercentKeyword()
    {
        String tokenStr = mTokenBuffer.toString();

        mToken.setValue(tokenStr);
        mStopFlag = true;

        if (sPercentKeywordMap.containsKey(tokenStr))
        {
            mToken.setType(sPercentKeywordMap.get(tokenStr));
        }
        else
        {
            badToken("Unknown % directive");
        }

        return;
    } // end of checkPercentKeyword()

    // Back up one character in the file so that the character
    // will be read again when nextToken() is called. This is
    // usually done when one token is terminated by another.
    /* package */ void ungetChar()
    {
        // Make sure there is a character to unget first.
        if (mReadIndex > 0)
        {
            --mReadIndex;
        }

        return;
    } // end of ungetChar()

    // End of State Machine Actions.
    //-----------------------------------------------------------

    // Return the next cooked token.
    /* package */ Token nextCookedToken()
    {
        // Keep reading until told to stop or the
        // end-of-file is reached.
        try
        {
            Object[] args = new Object[0];

            mStopFlag = false;
            while (!mStopFlag)
            {
                mCurrentChar = readChar();

                // If the character's integer value is greater
                // than 127, then issue a unicode transition
                // and let the lexer FSM decide whether it is
                // acceptable or not.
                if (mCurrentChar >= sTransMethod.length)
                {
                    mLexerFSM.unicode();
                }
                else
                {
                    // If this is an end-of-line character, add
                    // one to the current line number. CR-LF is
                    // a single end-of-line.
                    if (mCurrentChar == NEW_LINE)
                    {
                        ++mLineNumber;
                    }

                    // Translate character into a transition.
                    sTransMethod[mCurrentChar].invoke(mLexerFSM,
                                                      args);
                }
            }
        }
        catch (EOFException e)
        {
            // If this is the end of the source file, let
            // the parser know.
            mToken.setType(DONE_SUCCESS);
            mToken.setValue("");
        }
        catch (InvocationTargetException invokex)
        {
            badToken("Unknown token");
        }
        catch (IOException |
               IllegalAccessException |
               IllegalArgumentException jex)
        {
            badToken(jex.getMessage() +
                     " (token: " +
                     mTokenBuffer.toString() +
                     ")");
        }

        return(mToken);
    } // end of nextCookedToken()

    // Keep reading in characters until the close character is
    // found.
    private Token nextRawToken()
        throws IOException
    {
        int startLine = mLineNumber;

        // Clear out the token and get ready to work.
        startToken();

        // Keep reading until told to stop or the
        // end-of-file is reached.
        try
        {
            int depth = 0;

            mStopFlag = false;
            while (!mStopFlag)
            {
                mCurrentChar = readChar();

                // When working in RAW mode, the close character
                // may naturally occur. The only way we have of
                // knowing if the close character is for us or
                // not is by keeping track of the matching open
                // characters seen. When an open character is
                // seen, add one to the depth. When a close
                // character is seen, then either:
                // + if the depth is zero, this is the end of the
                //   raw code; return the token.
                // + if the depth is greater than zero, then
                //   subtract one from the depth.
                if (mCurrentChar == mCloseChar && depth == 0)
                {
                    mStopFlag = true;
                }
                else
                {
                    mTokenBuffer.append(mCurrentChar);

                    // Only RAW and RAW2 use the concept of
                    // depth.
                    if (mMode != RAW3)
                    {
                        if (mCurrentChar == mCloseChar)
                        {
                            --depth;
                        }
                        // If this is the open character, then
                        // add one to the depth which lets us
                        // know that the next close character
                        // does *not* end the raw code section.
                        else if (mCurrentChar == mOpenChar)
                        {
                            ++depth;
                        }
                        // If this is an end-of-line character,
                        // add one to the current line number.
                        // CR-LF is a single end-of-line.
                        else if (mCurrentChar == NEW_LINE)
                        {
                            ++mLineNumber;
                        }
                    }
                }
            }

            mToken.setType(SOURCE);
            mToken.setValue(mTokenBuffer.toString());
            mToken.setLineNumber(startLine);
        }
        catch (EOFException e)
        {
            StringBuilder msg = new StringBuilder();

            msg.append(
                "User source code contains an unbalanced ");
            msg.append(mOpenChar);
            msg.append(", ");
            msg.append(mCloseChar);
            msg.append(" pair.");

            // If this is the end of the source file, then the
            // raw code section has an unbalanced open character/
            // close character pair.
            mToken.setType(DONE_FAILED);
            mToken.setValue(msg.toString());
        }

        return (mToken);
    } // end of _nextRawToken()

    // Keep reading in characters until one of the close
    // characters is found.
    private Token nextRaw3Token()
        throws IOException
    {
        int startLine = mLineNumber;

        // Clear out the token and get ready to work.
        startToken();

        // Keep reading until told to stop or the
        // end-of-file is reached.
        try
        {
            mStopFlag = false;
            while (!mStopFlag)
            {
                mCurrentChar = readChar();

                // If this is an end-of-line character, add one
                // to the current line number. CR-LF is a single
                // end-of-line.
                if (mCurrentChar == NEW_LINE)
                {
                    ++mLineNumber;
                }

                // Keep reading until one of the specified close
                // characters is found.
                if (mCloseChars.indexOf(mCurrentChar) >= 0)
                {
                    mStopFlag = true;
                }
                else
                {
                    mTokenBuffer.append(mCurrentChar);
                }
            }

            mToken.setType(SOURCE);
            mToken.setValue(mTokenBuffer.toString());
            mToken.setLineNumber(startLine);
        }
        catch (EOFException e)
        {
            // EOF is always a valid end-of-token marker.
            // Return what has been collected.
            mToken.setType(SOURCE);
            mToken.setValue(mTokenBuffer.toString());
            mToken.setLineNumber(startLine);
        }

        return (mToken);
    } // end of nextRaw3Token()

    // Read in a parameter type declaration as raw source code.
    private Token nextParamTypeToken()
        throws IOException
    {
        int startLine = mLineNumber;
        int tokenType = SOURCE;

        // Clear out the token and get ready to work.
        startToken();

        // Keep reading until told to stop or the
        // end-of-file is reached.
        try
        {
            Deque<Character> depth = new LinkedList<>();
            Character openChar;
            int openIndex;
            int closeIndex;
            boolean quoteFlag = false;
            char quoteChar = ' ';
            boolean escapeFlag = false;

            mStopFlag = false;
            while (!mStopFlag)
            {
                mCurrentChar = readChar();

                // Terminate when the end or separator character
                // is seen - but only if we are not inside a
                // collection.
                if ((mCurrentChar == mEndChar ||
                     mCurrentChar == mSeparator) &&
                    depth.isEmpty() &&
                    !quoteFlag)
                {
                    mStopFlag = true;

                    // Unlike nextRawToken() it is very important
                    // *not* to consume the ',' or ')' because
                    // the parser needs to see that token.
                    ungetChar();
                }
                else
                {
                    mTokenBuffer.append(mCurrentChar);

                    // Is this an escaped character?
                    if (escapeFlag)
                    {
                        // Yes. Turn off the escape.
                        escapeFlag = false;
                    }
                    // Is this the escape character?
                    else if (mCurrentChar == ESCAPE)
                    {
                        // Yes. Escape the next character.
                        escapeFlag = true;
                    }
                    // Is this a quote charater?
                    else if (mQuoteList.indexOf(mCurrentChar) >= 0)
                    {
                        // Is this the opening quote?
                        if (!quoteFlag)
                        {
                            // Yes, we are entering a quoted
                            // section.
                            quoteFlag = true;
                            quoteChar = mCurrentChar;
                        }
                        // Does this character match the opening
                        // quote character?
                        else if (mCurrentChar == quoteChar)
                        {
                            // Yes. So this must be a closing
                            // quote.
                            quoteFlag = false;
                            quoteChar = ' ';
                        }
                        // Otherwise this is just another
                        // character in the quoted string.
                    }
                    // Are we inside a quote?
                    else if (quoteFlag)
                    {
                        // Yes. Nothing more to do.
                    }
                    // Is this a closing character?
                    else if (
                        (closeIndex =
                         mCloseList.indexOf(mCurrentChar)) >= 0)
                    {
                        // Yes. Does it match the opening
                        // character?
                        if (depth.isEmpty())
                        {
                            // No because there is no opening
                            // character to match.
                            tokenType = DONE_FAILED;

                            // Place the error message into the
                            // token buffer.
                            mTokenBuffer.delete(
                                0, mTokenBuffer.length());
                            mTokenBuffer.append("'");
                            mTokenBuffer.append(mCurrentChar);
                            mTokenBuffer.append(
                                "' has no matching '");
                            mTokenBuffer.append(
                                mOpenList.get(closeIndex));
                            mTokenBuffer.append("'.");
                        }
                        // There is an open character.
                        else
                        {
                            openChar = depth.removeFirst();
                            openIndex =
                                mOpenList.indexOf(openChar);
                            if (closeIndex != openIndex)
                            {
                                // No, the closing character does
                                // not match the opening.
                                tokenType = DONE_FAILED;

                                mTokenBuffer.delete(
                                    0, mTokenBuffer.length());
                                mTokenBuffer.append("'");
                                mTokenBuffer.append(
                                    mCurrentChar);
                                mTokenBuffer.append(
                                    "' does not match '");
                                mTokenBuffer.append(openChar);
                                mTokenBuffer.append("'.");
                            }
                            // Yes, the closing character matches
                            // the opening character.
                        }
                    }
                    else if (mOpenList.contains(mCurrentChar))
                    {
                        depth.addFirst(mCurrentChar);
                    }
                    else if (mCurrentChar == NEW_LINE)
                    {
                        ++mLineNumber;
                    }
                }
            }
        }
        catch (EOFException e)
        {
            // If this is the end of the source file, then the
            // raw code section has an unbalanced open character/
            // close character pair.
            tokenType = DONE_FAILED;

            mTokenBuffer.delete(0, mTokenBuffer.length());
            mTokenBuffer.append("User source code contains ");
            mTokenBuffer.append("an unbalanced open, closing ");
            mTokenBuffer.append(" pair.");
        }

        mToken.setType(tokenType);
        mToken.setValue(mTokenBuffer.toString());
        mToken.setLineNumber(startLine);

        return (mToken);
    } // end of nextParamTypeToken()

    // Read the next character. Actually, this routine reads in
    // a large buffer and data returns the next character from
    // there. The idea is to do a few large, efficient reads and
    // make single character reads to be array retrievals.
    // NOTE: this lexer backs up at most two characters. So
    // when reading in a new buffer, copy the last two characters
    // to the first two bytes and read in the next maximum number
    // of bytes.
    private char readChar()
        throws IOException,
               EOFException
    {
        char retval;

        // If we are at the end of the buffer, read the
        // next buffer-full.
        if (mReadIndex == mBufferSize)
        {
            int index;
            int size;
            int offset = 0;
            int c = 0;

            // Copy the last two bytes to the first two bytes.
            // Why? Because the lexer can back up to two bytes.
            if (mBufferSize > 2)
            {
                offset = 2;

                mReadBuffer[0] = mReadBuffer[mBufferSize - 2];
                mReadBuffer[1] = mReadBuffer[mBufferSize - 1];
            }
            else if (mBufferSize > 1)
            {
                offset = 1;

                mReadBuffer[0] = mReadBuffer[mBufferSize - 1];
            }

            for (index = offset, size = 0;
                 size <  MAX_BUFFER_LEN;
                 ++index, ++size)
            {
                c = mStream.read();
                if (c < 0)
                {
                    break;
                }
                else
                {
                    mReadBuffer[index] = (char) c;
                }
            }

            // End of file has been reached.
            if (size == 0 && c < 0)
            {
                mBufferSize = 0;
                throw (new EOFException("end-of-file reached"));
            }
            else
            {
                // The buffer's true size is the number of bytes
                // read plus the offset.
                mBufferSize = size + offset;
                mReadIndex = offset;
            }
        }

        retval = mReadBuffer[mReadIndex];
        ++mReadIndex;

        return(retval);
    } // end of readChar()

//---------------------------------------------------------------
// Inner classes.
//

    /* package */ final class Token
    {
    //-----------------------------------------------------------
    // Member data.
    //

        //-------------------------------------------------------
        // Locals.
        //

        private int mType;
        private String mValue;
        private int mLineNumber;

    //-----------------------------------------------------------
    // Member methods.
    //

        //-------------------------------------------------------
        // Constructors.
        //

        /* package */ Token()
        {
            mType = TOKEN_NOT_SET;
            mValue = null;
            mLineNumber = -1;
        }

        //
        // end of Constructors.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Object Method Overrides.
        //

        @Override
        public String toString()
        {
            return("{" +
                   SmcLexer.sTypeName[mType] +
                   ", " +
                   mValue + "}");
        }

        //
        // end of Object Method Overrides.
        //-------------------------------------------------------

        //-------------------------------------------------------
        //
        //

        /* package */ int getType()
        {
            return(mType);
        }

        /* package */ String getValue()
        {
            return(mValue);
        }

        /* package */ int getLineNumber()
        {
            return (mLineNumber);
        }

        /* package */ Token copy()
        {
            Token retval = new Token();

            retval.setType(mType);
            retval.setValue(mValue);
            retval.setLineNumber(mLineNumber);

            return (retval);
        }

        //
        //
        //-------------------------------------------------------

        //-------------------------------------------------------
        //
        //

        /* package */ void setType(int type)
        {
            mType = type;
            return;
        }

        /* package */ void setLineNumber(int line_number)
        {
            mLineNumber = line_number;
            return;
        }

        /* package */ void setValue(String value)
        {
            mValue = value;
        }

        //
        //
        //-------------------------------------------------------

        private void reset()
        {
            mType = TOKEN_NOT_SET;
            mValue = null;
            mLineNumber = -1;
        }
    } // end of class Token
} // end of class SmcLexer

//
// CHANGE LOG
// Log: SmcLexer.java,v
// Revision 1.6  2013/07/14 14:32:38  cwrapp
// check in for release 6.2.0
//
// Revision 1.5  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
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
// Revision 1.2  2009/04/11 13:11:12  cwrapp
// Corrected raw mode 3 to handle multiple argument template/generic declarations.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.13  2007/12/28 12:34:41  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.12  2007/11/19 18:53:21  fperrad
// + add : jump syntax
//   jump uses the same syntax as push,
//   allows transition between states of different maps but without stacking a return context.
//
// Revision 1.11  2007/02/21 13:55:38  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.10  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.9  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.8  2006/07/11 18:15:49  cwrapp
// Corrected comment.
//
// Revision 1.7  2005/07/07 12:10:52  fperrad
// Add a new token '$' for Perl language.
//
// Revision 1.6  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the generate Context
// class' accessibility level in Java and C#.
//
// Revision 1.5  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.8  2005/02/21 18:13:40  charlesr
// Added separate _PercentKeywordMap like _KeywordMap and
// modified _checkPercentKeyword() method to use it.
//
// Corrected nextRaw3Token() which did not increment the current
// line number when end-of-line was reached.
//
// Removed unknown FSM transition, using unicode transition
// instead.
//
// General variable name clean up.
//
// Revision 1.7  2005/02/21 15:35:58  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.6  2005/02/03 16:56:21  charlesr
// Changed lexer and parser constructors so that the parser
// instantiates the lexer rather than the application. This
// change was done as part of an ongoing project to make the
// SMC parser self-contained with the goal of releasing a
// separate SMC parser library.
//
// Also tightened up the permissions from public to package.
//
// Revision 1.5  2004/10/30 16:05:08  charlesr
// Added support for unicode.
//
// Revision 1.4  2004/10/02 19:52:16  charlesr
// Corrected error when lexer needed to back up to a character in
// the previous buffer-full of data. Since the lexer backs up at
// most two characters, the previous two characters are carried
// over from the previous buffer to the new buffer.
//
// Revision 1.3  2004/09/06 16:40:11  charlesr
// Added C# support.
//
// Revision 1.2  2004/05/31 13:54:25  charlesr
// Added support for VB.net code generation.
//
// Revision 1.1  2004/01/29 02:16:22  charlesr
// Added InvocationTargetException catch to nextCookedToken()
// method which converts exception to a "bad token".
//
// Revision 1.0  2003/12/14 21:03:41  charlesr
// Initial revision
//
