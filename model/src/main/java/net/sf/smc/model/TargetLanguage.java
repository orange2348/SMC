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
// Copyright (C) 2018. Charles W. Rapp.
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

package net.sf.smc.model;

import java.text.MessageFormat;

/**
 * Enumerates the currently supported SMC target languages. This
 * enum associates the expected target language suffix and the
 * generated file name format.
 *
 * @author <a href="mailto:rapp@acm.org">Charles W. Rapp</a>
 */

public enum TargetLanguage
{
    /**
     * The target language is undefined.
     */
    LANG_NOT_SET ("", ""),

    /**
     * <a href="http://www.research.att.com/~bs/C++.html">C++</a>
     */
    C_PLUS_PLUS ("cpp", "{0}_sm"),

    /**
     * <a href="http://java.oracle.com">Java</a>
     */
    JAVA ("java", "{0}Context"),

    /**
     * <a href="http://www.tcl.tk">Tcl</a>
     */
    TCL ("tcl", "{0}_sm"),

    /**
     * <a href="http://msdn.microsoft.com/en-us/vbasic/default.aspx">VB.net</a>
     */
    VB ("vb", "{0}_sm"),

    /**
     * .<a href="http://msdn.microsoft.com/en-us/vcsharp/default.aspx">net C#</a>
     */
    C_SHARP ("cs", "{0}_sm"),

    /**
     * <a href="http://www.python.org">Python</a>
     */
    PYTHON ("py", "{0}_sm"),

    /**
     * An HTML table
     */
    TABLE ("html", "{0}_sm"),

    /**
     * <a href="http://www.graphviz.org">GraphViz</a>
     */
    GRAPH ("dot", "{0}_sm"),

    /**
     * <a href="http://www.perl.org">Perl</a>
     */
    PERL ("pm", "{0}_sm"),

    /**
     * <a href="http://ruby-lang.org">Ruby</a>
     */
    RUBY ("rb", "{0}_sm"),

    /**
     * C
     */
    C ("c", "{0}_sm"),

    /**
     * Objective C
     */
    OBJECTIVE_C ("m", "{0}_sm"),

    /**
     * <a href="http://www.lua.org">Lua</a>
     */
    LUA ("lua", "{0}_sm"),

    /**
     * <a href="http://groovy.codehaus.org">Groovy</a>
     */
    GROOVY ("groovy", "{0}Context"),

    /**
     * <a href="http://www.scala-lang.org">Scala</a>
     */
    SCALA ("scala", "{0}Context"),

    /**
     * <a href="http://www.php.net">PHP</a>
     */
    PHP ("php", "{0}_sm"),

    /**
     * JavaScript
     */
    JS ("js", "{0}_sm"),

    /**
     * <a href="http://java.oracle.com">Java 7</a>
     * <p>
     * This version generates a transition table, not the
     * State pattern.
     * </p>
     */
    JAVA7 ("java", "{0}Context");


//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Locals.
    //

    /**
     * Target language file suffix.
     */
    private final String mSuffix;

    /**
     * Text format used to generate the target file name.
     */
    private final String mTargetNameFormat;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a new target language instance for the given
     * suffix and file name format.
     * @param suffix target language file name suffix.
     * @param nameFormat target file name format.
     */
    private TargetLanguage(final String suffix,
                           final String nameFormat)
    {
        mSuffix = suffix;
        mTargetNameFormat = nameFormat;
    } // end of TargetLanguage(String, String)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods
    //

    /**
     * Returns the target language file name suffix.
     * @return file name suffix.
     */
    public String suffix()
    {
        return (mSuffix);
    } // end of suffix()

    /**
     * Returns the generated target file base name given the
     * FSM source file base name.
     * @param baseName FSM source file base name.
     * @return generated target file base name.
     */
    public String targetFileName(final String baseName)
    {
        return (
            MessageFormat.format(mTargetNameFormat, baseName));
    } // end of targetFileName(String)

    //
    // end of Get methods.
    //-----------------------------------------------------------
} // end of enum TargetLanguage
