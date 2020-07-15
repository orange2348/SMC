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
// Copyright (C) 2000 - 2008. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python, Perl code generation and examples/Perl,
//   Ruby code generation and examples/Ruby, Lua code generation
//   and examples/Lua, Groovy code generation and examples/Groovy,
//   Scala code generation and examples/Scala.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//   Toni Arnold contributed the PHP code generation and
//   examples/PHP.
//
// SMC --
//
//  State Map Compiler
//
// This class parses a state map exception, checks the code
// for semantic consistency and then generates object-oriented
// code in the user specified target language.
//
// RCS ID
// Id: Smc.java,v 1.45 2013/12/15 16:31:41 fperrad Exp
//
// CHANGE LOG
// (See bottom of file.)
//

package net.sf.smc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import net.sf.smc.generator.SmcCGenerator;
import net.sf.smc.generator.SmcCSharpGenerator;
import net.sf.smc.generator.SmcCodeGenerator;
import net.sf.smc.generator.SmcCppGenerator;
import net.sf.smc.generator.SmcGraphGenerator;
import net.sf.smc.generator.SmcGroovyGenerator;
import net.sf.smc.generator.SmcHeaderCGenerator;
import net.sf.smc.generator.SmcHeaderGenerator;
import net.sf.smc.generator.SmcHeaderObjCGenerator;
import net.sf.smc.generator.SmcJSGenerator;
import net.sf.smc.generator.SmcJava7Generator;
import net.sf.smc.generator.SmcJavaGenerator;
import net.sf.smc.generator.SmcLuaGenerator;
import net.sf.smc.generator.SmcObjCGenerator;
import net.sf.smc.generator.SmcOptions;
import net.sf.smc.generator.SmcPerlGenerator;
import net.sf.smc.generator.SmcPhpGenerator;
import net.sf.smc.generator.SmcPythonGenerator;
import net.sf.smc.generator.SmcRubyGenerator;
import net.sf.smc.generator.SmcScalaGenerator;
import net.sf.smc.generator.SmcTableGenerator;
import net.sf.smc.generator.SmcTclGenerator;
import net.sf.smc.generator.SmcVBGenerator;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.TargetLanguage;
import net.sf.smc.parser.SmcMessage;
import net.sf.smc.parser.SmcParser;

/**
 * Main class for the state machine compiler application.
 * This class is responsible for processing the command line
 * arguments, configuring the parser, model and generator
 * packages according to the command line and outputing the
 * results to the user. The actual work is performed by the
 * parser, model and generator packages.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class Smc
{
//---------------------------------------------------------------
// Member Data
//

    //-----------------------------------------------------------
    // Constants.
    //

    // Specifies target programming language.
    // Constants.
    //

    public static final String APP_NAME = "smc";
    public static final String VERSION = "v7.1.1";

    /**
     * SMC target files must end with {@value}.
     */
    public static final String SM_SUFFIX = ".sm";

    // Command line option flags.
    public static final String ACCESS_FLAG = "-access";
    public static final String CAST_FLAG = "-cast";
    public static final String DIRECTORY_FLAG = "-d";
    public static final String DEBUG_FLAG = "-g";
    public static final String DEBUG_LEVEL0_FLAG = "-g0";
    public static final String DEBUG_LEVEL1_FLAG = "-g1";
    public static final String GENERIC_FLAG = "-generic";
    public static final String GENERIC7_FLAG = "-generic7";
    public static final String GLEVEL_FLAG = "-glevel";
    public static final String HEADER_FLAG = "-headerd";
    public static final String HEADER_SUFFIX_FLAG = "-hsuffix";
    public static final String HELP_FLAG = "-help";
    public static final String NO_CATCH_FLAG = "-nocatch";
    public static final String NO_EXCEPTIONS_FLAG = "-noex";
    public static final String NO_STREAMS_FLAG = "-nostreams";
    public static final String CRTP_FLAG = "-crtp";
    public static final String STACK_FLAG = "-static";
    public static final String REFLECT_FLAG = "-reflect";
    public static final String RETURN_FLAG = "-return";
    public static final String SERIAL_FLAG = "-serial";
    public static final String SILENT_FLAG = "-silent";
    public static final String SUFFIX_FLAG = "-suffix";
    public static final String SYNC_FLAG = "-sync";
    public static final String VERBOSE_FLAG = "-verbose";
    public static final String VERSION_FLAG = "-version";
    public static final String VVERBOSE_FLAG = "-vverbose";
    public static final String USE_PROTOCOL_FLAG = "-protocol";

    /**
     * Java package level access is {@value}.
     */
    public static final String PACKAGE_LEVEL = "package";

    /**
     * Since "package" is a Java restricted keyword and cannot be
     * used to specify an access level, then use {@value} instead.
     */
    public static final String PACKAGE_ACCESS = "/* package */";

    //-----------------------------------------------------------
    // Statics.
    //

    // Specifies target programming language.
    /* package */ static Language sTargetLanguage;

    // The target file currently being compiled.
    private static String sSourceFileName;

    // The state map target code to be compiled.
    private static List<String> sSourceFileList;

    // Append this suffix to the end of the output file.
    private static String sSuffix;

    // Append this suffix to the end of the output header file.
    private static String sHSuffix;

    // Place the output files in this directory. May be null.
    private static String sOutputDirectory;

    // Place header files in this directory. May be null.
    private static String sHeaderDirectory;

    // The debug level.
    private static int sDebugLevel;

    // If true, then do not use C++ iostreams for debugging.
    // Application code must provide a TRACE macro to output
    // the debug messages.
    private static boolean sNostreams;

    // If true, then user supplied class has to be derived
    // from state machine.
    // See CRTP ("curiously recurring template pattern").
    private static boolean sCRTP;

    // If true, then generate thread-safe Java code.
    private static boolean sSync;

    // If true, then do *not* generate C++ exception throws.
    private static boolean sNoex;

    // If true, then do *not* generate try/catch/rethrow code.
    private static boolean sNocatch;

    // If > 0, then this is the state stack's fixed-length.
    private static int sStateStackSize;

    // If true, then generate unique integer IDs for each state.
    private static boolean sSerial;

    // If true, then generate getTransitions() method for each
    // state.
    private static boolean sReflection;

    // If true, then use a Map<String, Integer> for the
    // reflection map.
    private static boolean sGeneric;

    // If generics are used, then this is the Java target
    // language version.
    private static boolean sJava7Flag;

    // If true, then generate compiler verbose messages.
    private static boolean sVerbose;

    // If true, then generate FSM messages.
    private static boolean sFSMVerbose;

    // The details placed into the GraphViz DOT file.
    private static int sGraphLevel;

    // When generating C++ code, use this cast type.
    private static String sCastType;

    // Have Smc.main() return rather than exit.
    private static boolean sReturn;

    // Use this access identifier for the generated classes.
    private static String sAccessLevel;

    // Use-defined FSM context class extends a @protocol.
    // Generated code references the context class via the
    // protocol.
    private static boolean sProtocol;

    // Store command line error messages here.
    private static String sErrorMsg;

    // The app's version ID.
    private static String sVersion;

    // The list of all supported languages.
    private static final Language[] sLanguages;

    // Maps each command line option flag to the target languages
    // supporting the flag.
    private static final Map<String, List<Language>> sOptionMap;

    // Maps the target language to the list of acceptable access
    // levels.
    private static final Map<Language, List<String>> sAccessMap;

    static
    {
        // Fill in the static languages array.
        sLanguages = new Language[SmcParser.LANGUAGE_COUNT];
        sLanguages[TargetLanguage.LANG_NOT_SET.ordinal()] =
            new Language(TargetLanguage.LANG_NOT_SET,
                         "",
                         null,
                         null,
                         null);
        sLanguages[TargetLanguage.C.ordinal()] =
            new Language(
                TargetLanguage.C,
                "-c",
                "C",
                SmcCGenerator.class,
                SmcHeaderCGenerator.class);
        sLanguages[TargetLanguage.C_PLUS_PLUS.ordinal()] =
            new Language(
                TargetLanguage.C_PLUS_PLUS,
                "-c++",
                "C++",
                SmcCppGenerator.class,
                SmcHeaderGenerator.class);
        sLanguages[TargetLanguage.C_SHARP.ordinal()] =
            new Language(
                TargetLanguage.C_SHARP,
                "-csharp",
                "C#",
                SmcCSharpGenerator.class,
                null);
        sLanguages[TargetLanguage.JAVA.ordinal()] =
            new Language(
                TargetLanguage.JAVA,
                "-java",
                "Java",
                SmcJavaGenerator.class,
                null);
        sLanguages[TargetLanguage.JAVA7.ordinal()] =
            new Language(
                TargetLanguage.JAVA7,
                "-java7",
                "Java7",
                SmcJava7Generator.class,
                null);
        sLanguages[TargetLanguage.GRAPH.ordinal()] =
            new Language(
                TargetLanguage.GRAPH,
                "-graph",
                "-graph",
                SmcGraphGenerator.class,
                null);
        sLanguages[TargetLanguage.GROOVY.ordinal()] =
            new Language(
                TargetLanguage.GROOVY,
                "-groovy",
                "Groovy",
                SmcGroovyGenerator.class,
                null);
        sLanguages[TargetLanguage.LUA.ordinal()] =
            new Language(
                TargetLanguage.LUA,
                "-lua",
                "Lua",
                SmcLuaGenerator.class,
                null);
        sLanguages[TargetLanguage.OBJECTIVE_C.ordinal()] =
            new Language(
                TargetLanguage.OBJECTIVE_C,
                "-objc",
                "Objective-C",
                SmcObjCGenerator.class,
                SmcHeaderObjCGenerator.class);
        sLanguages[TargetLanguage.PERL.ordinal()] =
            new Language(
                TargetLanguage.PERL,
                "-perl",
                "Perl",
                SmcPerlGenerator.class,
                null);
        sLanguages[TargetLanguage.PHP.ordinal()] =
            new Language(
                TargetLanguage.PERL,
                "-php",
                "PHP",
                SmcPhpGenerator.class,
                null);
        sLanguages[TargetLanguage.PYTHON.ordinal()] =
            new Language(
                TargetLanguage.PYTHON,
                "-python",
                "Python",
                SmcPythonGenerator.class,
                null);
        sLanguages[TargetLanguage.RUBY.ordinal()] =
            new Language(
                TargetLanguage.RUBY,
                "-ruby",
                "Ruby",
                SmcRubyGenerator.class,
                null);
        sLanguages[TargetLanguage.SCALA.ordinal()] =
            new Language(
                TargetLanguage.SCALA,
                "-scala",
                "Scala",
                SmcScalaGenerator.class,
                null);
        sLanguages[TargetLanguage.TABLE.ordinal()] =
            new Language(
                TargetLanguage.TABLE,
                "-table",
                "-table",
                SmcTableGenerator.class,
                null);
        sLanguages[TargetLanguage.TCL.ordinal()] =
            new Language(
                TargetLanguage.TCL,
                "-tcl",
                "[incr Tcl]",
                SmcTclGenerator.class,
                null);
        sLanguages[TargetLanguage.VB.ordinal()] =
            new Language(
                TargetLanguage.VB,
                "-vb",
                "VB.net",
                SmcVBGenerator.class,
                null);
        sLanguages[TargetLanguage.JS.ordinal()] =
            new Language(
                TargetLanguage.JS,
                "-js",
                "JavaScript",
                SmcJSGenerator.class,
                null);

        List<Language> languages = new ArrayList<>();

        sOptionMap = new HashMap<>();

        // Languages supporting each option:
        // +    -access:  Java
        // +      -cast:  C++
        // +         -d:  all
        // +         -g:  all
        // +        -g0:  all
        // +        -g1:  all
        // +    -glevel:  graph
        // +    -header:  C, C++, Objective-C
        // +    -hsuffix: C, C++, Objective-C
        // +      -help:  all
        // +   -nocatch:  all
        // +      -noex:  C++
        // + -nostreams:  C++
        // +  -protocol:  Objective-C
        // +   -reflect:  C#, Java, JavaScript, TCL, VB, Lua, Perl,
        //                PHP, Python, Ruby, Groovy, Scala
        // +    -return:  all
        // +    -serial:  C#, C++, Java, Tcl, VB, Groovy, Scala
        // +    -static:  C++
        // +    -suffix:  all
        // +      -sync:  C#, Java, VB, Groovy, Scala
        // +   -verbose:  all
        // +   -version:  all
        // +  -vverbose:  all

        // Set the options supporting all languages first.
        for (TargetLanguage target :
                 EnumSet.allOf(TargetLanguage.class))
        {
            languages.add(sLanguages[target.ordinal()]);
        }

        sOptionMap.put(DIRECTORY_FLAG, languages);
        sOptionMap.put(DEBUG_FLAG, languages);
        sOptionMap.put(DEBUG_LEVEL0_FLAG, languages);
        sOptionMap.put(DEBUG_LEVEL1_FLAG, languages);
        sOptionMap.put(HELP_FLAG, languages);
        sOptionMap.put(NO_CATCH_FLAG, languages);
        sOptionMap.put(RETURN_FLAG, languages);
        sOptionMap.put(SUFFIX_FLAG, languages);
        sOptionMap.put(VERBOSE_FLAG, languages);
        sOptionMap.put(VERSION_FLAG, languages);
        sOptionMap.put(VVERBOSE_FLAG, languages);

        // Set the options supported by less than all langugages.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.C_PLUS_PLUS.ordinal()]);
        sOptionMap.put(CAST_FLAG, languages);
        sOptionMap.put(NO_EXCEPTIONS_FLAG, languages);
        sOptionMap.put(NO_STREAMS_FLAG, languages);
        sOptionMap.put(CRTP_FLAG, languages);
        sOptionMap.put(STACK_FLAG, languages);

        // The -access option.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.JAVA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA7.ordinal()]);
        sOptionMap.put(ACCESS_FLAG, languages);

        // Languages using a header file.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.C_PLUS_PLUS.ordinal()]);
        languages.add(sLanguages[TargetLanguage.C.ordinal()]);
        languages.add(sLanguages[TargetLanguage.OBJECTIVE_C.ordinal()]);
        sOptionMap.put(HEADER_FLAG, languages);
        sOptionMap.put(HEADER_SUFFIX_FLAG, languages);

        // Languages supporting thread synchronization.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA7.ordinal()]);
        languages.add(sLanguages[TargetLanguage.VB.ordinal()]);
        languages.add(sLanguages[TargetLanguage.GROOVY.ordinal()]);
        languages.add(sLanguages[TargetLanguage.SCALA.ordinal()]);
        sOptionMap.put(SYNC_FLAG, languages);

        // Languages supporting reflection.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA7.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JS.ordinal()]);
        languages.add(sLanguages[TargetLanguage.VB.ordinal()]);
        languages.add(sLanguages[TargetLanguage.TCL.ordinal()]);
        languages.add(sLanguages[TargetLanguage.LUA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.PERL.ordinal()]);
        languages.add(sLanguages[TargetLanguage.PHP.ordinal()]);
        languages.add(sLanguages[TargetLanguage.PYTHON.ordinal()]);
        languages.add(sLanguages[TargetLanguage.RUBY.ordinal()]);
        languages.add(sLanguages[TargetLanguage.GROOVY.ordinal()]);
        languages.add(sLanguages[TargetLanguage.SCALA.ordinal()]);
        sOptionMap.put(REFLECT_FLAG, languages);

        // Languages supporting serialization.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA7.ordinal()]);
        languages.add(sLanguages[TargetLanguage.VB.ordinal()]);
        languages.add(sLanguages[TargetLanguage.TCL.ordinal()]);
        languages.add(sLanguages[TargetLanguage.C_PLUS_PLUS.ordinal()]);
        languages.add(sLanguages[TargetLanguage.GROOVY.ordinal()]);
        languages.add(sLanguages[TargetLanguage.SCALA.ordinal()]);
        sOptionMap.put(SERIAL_FLAG, languages);

        // The -glevel option.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.GRAPH.ordinal()]);
        sOptionMap.put(GLEVEL_FLAG, languages);

        // The -generic option.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA7.ordinal()]);
        languages.add(sLanguages[TargetLanguage.VB.ordinal()]);
        sOptionMap.put(GENERIC_FLAG, languages);

        // The -generic7 option.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.JAVA.ordinal()]);
        languages.add(sLanguages[TargetLanguage.JAVA7.ordinal()]);
        sOptionMap.put(GENERIC7_FLAG, languages);

        // The -protocol option.
        languages = new ArrayList<>();
        languages.add(sLanguages[TargetLanguage.OBJECTIVE_C.ordinal()]);
        sOptionMap.put(USE_PROTOCOL_FLAG, languages);

        // Define the allowed access level keywords for each language
        // which supports the -access option.
        List<String> accessLevels;

        sAccessMap = new HashMap<>();
        accessLevels = new ArrayList<>();
        accessLevels.add("public");
        accessLevels.add("protected");
        accessLevels.add("package");
        accessLevels.add("private");
        sAccessMap.put(
            sLanguages[TargetLanguage.JAVA.ordinal()], accessLevels);
        sAccessMap.put(
            sLanguages[TargetLanguage.JAVA7.ordinal()], accessLevels);
    } // end of static

//---------------------------------------------------------------
// Member Methods
//

    //-----------------------------------------------------------
    // Main method.
    //

    /**
     * The state machine compiler main method.
     * @param args command line arguments.
     */
    public static void main(final String[] args)
    {
        int retcode = 0;

        sErrorMsg = new String();

        // The default smc output level is 1.
        sTargetLanguage = null;
        sVersion = VERSION;
        sDebugLevel = SmcCodeGenerator.NO_DEBUG_OUTPUT;
        sNostreams = false;
        sCRTP = false;
        sSync = false;
        sNoex = false;
        sNocatch = false;
        sStateStackSize = 0;
        sSerial = false;
        sCastType = "dynamic_cast";
        sGraphLevel = SmcCodeGenerator.GRAPH_LEVEL_0;
        sSourceFileList = new ArrayList<>();
        sVerbose = false;
        sFSMVerbose = false;
        sReturn = false;
        sReflection = false;
        sOutputDirectory = null;
        sHeaderDirectory = null;
        sSuffix = null;
        sHSuffix = SmcCodeGenerator.DEFAULT_HEADER_SUFFIX;
        sAccessLevel = null;
        sGeneric = false;
        sJava7Flag = false;
        sProtocol = false;

        // Process the command line.
        if (parseArgs(args) == false)
        {
            retcode = 1;
            System.err.println(APP_NAME + ": " + sErrorMsg);
        }
        // Arguments check out - start compiling..
        else
        {
            SmcParser parser;
            SmcFSM fsm;
            Iterator<String> sit;
            long startTime = 0;
            long finishTime;
            long totalStartTime = 0;
            long totalFinishTime;

            if (sVerbose)
            {
                totalStartTime = System.currentTimeMillis();
            }

            try
            {
                for (sit = sSourceFileList.iterator();
                     sit.hasNext() == true;
                    )
                {
                    sSourceFileName = sit.next();

                    if (sVerbose)
                    {
                        System.out.print("[parsing started ");
                        System.out.print(sSourceFileName);
                        System.out.println("]");

                        startTime = System.currentTimeMillis();
                    }

                    parser =
                        new SmcParser(
                            getFileName(sSourceFileName),
                            new FileInputStream(sSourceFileName),
                            sTargetLanguage.language(),
                            sFSMVerbose);

                    // First - do the parsing
                    fsm = parser.parse();

                    if (sVerbose == true)
                    {
                        finishTime = System.currentTimeMillis();

                        System.out.print("[parsing completed ");
                        System.out.print(finishTime - startTime);
                        System.out.println("ms]");
                    }

                    if ( parser.getMessages().size() > 0 )
                    {
                        // Output the parser's messages.
                        outputMessages(sSourceFileName,
                                       System.err,
                                       parser.getMessages());
                    }

                    if (fsm == null)
                    {
                        retcode = 1;
                    }
                    else
                    {
                        SmcSyntaxChecker checker =
                            new SmcSyntaxChecker(
                                sSourceFileName,
                                sTargetLanguage.language());

                        if (sVerbose == true)
                        {
                            System.out.print("[checking ");
                            System.out.print(sSourceFileName);
                            System.out.println("]");
                        }

                        // Second - do the semantic check.
                        fsm.accept(checker);
                        if (checker.getMessages().size() > 0)
                        {
                            outputMessages(
                               sSourceFileName,
                               System.err,
                               checker.getMessages());
                        }
                        if (!checker.isValid())
                        {
                            retcode = 1;
                        }
                        else
                        {
                            // Third - do the code generation.
                            generateCode(fsm);
                        }
                    }
                }
            }
            // Report an unknown file exception.
            catch (FileNotFoundException filex)
            {
                System.err.print(sSourceFileName);
                System.err.print(": error - ");
                System.err.println(filex.getMessage());
            }
            // A parse exception may be thrown by generateCode().
            // This is not a problem.
            catch (ParseException parsex)
            {
                System.err.print(sSourceFileName);
                System.err.print(":");
                System.err.print(parsex.getErrorOffset());
                System.err.print(": error - ");
                System.err.println(parsex.getMessage());
            }
            catch (IOException |
                   IllegalAccessException |
                   InvocationTargetException e)
            {
                retcode = 1;

                System.err.println(
                    "SMC has experienced a fatal error. Please e-mail the following error output to rapp@acm.org. Thank you.\n");
                System.err.println(
                    "--------------------------------------------------------------------------------");
                System.err.println("SMC version: " + sVersion);
                System.err.println(
                    "JRE version: v. " +
                    System.getProperty("java.version"));
                System.err.println(
                    "JRE vender: " +
                    System.getProperty("java.vendor") +
                    " (" +
                    System.getProperty("java.vendor.url") +
                    ")");
                System.err.println(
                    "JVM: " +
                    System.getProperty("java.vm.name") +
                    ", v. " +
                    System.getProperty("java.vm.version"));
                System.err.println(
                    "JVM vender: " +
                    System.getProperty("java.vm.vendor"));
                System.err.println("Exception:\n");
                e.printStackTrace(System.err);
                System.err.println(
                    "--------------------------------------------------------------------------------");
            }

            if (sVerbose == true)
            {
                totalFinishTime = System.currentTimeMillis();

                System.out.print("[total ");
                System.out.print(
                    totalFinishTime - totalStartTime);
                System.out.println("ms]");
            }
        }

        // Need to return the appropriate exit code in case SMC
        // is called by make. Just doing a return always results
        // in a zero return code.
        // v. 4.0.0: But calling exit when SMC is an ANT task is
        // problematic. ANT is a Java program and calls Smc.main
        // directly and not as a forked process. So when Smc.main
        // exits, it exits the JVM for everyone including ANT.
        if (sReturn == false)
        {
            System.exit(retcode);
        }
        else
        {
            return;
        }
    } // end of main(String[])

    //
    // end of Main method.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Constructors.
    //

    // Private to prevent instantiation.
    private Smc()
    {}

    //
    // end of Constructors.
    //-----------------------------------------------------------

    /**
     * Returns the language record associated with the given
     * name. Returns {@code null} if {@code name} is not
     * associated with any supported target language.
     * @param name target language name.
     * @return language record.
     */
    public static final Language findTargetLanguage(final String name)
    {
        final String dash = "-";

        // Make sure the name is in lower case and prepend a
        // dash (-) if not there.
        String lName = name.toLowerCase();

        if (!lName.startsWith(dash))
        {
            lName = dash + lName;
        }

        return (findLanguage(lName));
    } // end of findTargetLanguage(String)

    /**
     * Returns {@code true} if the target language supports the
     * specified option.
     * @param option command line option.
     * @param language check if this language supports
     * {@code option}.
     * @return {@code true} if the command line option is
     * valid for {@code language}.
     */
    public static boolean supportsOption(final String option,
                                         final Language language)
    {
        final List<Language> languages = sOptionMap.get(option);

        return (languages != null &&
                languages.contains(language));
    } // end of supportsOption(String, Language)

    /**
     * Returns {@code true} if {@code path} is a valid
     * directory. Valid means that the direct exists and is
     * either readable or writable.
     * @param path used to create {@link File} instance.
     * @param readable expected {@link File#canRead()} result.
     * @param writable expected {@link File#canWrite()} result.
     * @return {@code true} if specified directory meets
     * expected results.
     */
    public static boolean isValidDirectory(final String path,
                                           final boolean readable,
                                           final boolean writable)
    {
        boolean retcode = false;

        try
        {
            final File pathObj = new File(path);

            if (pathObj.isDirectory() == false)
            {
                sErrorMsg =
                    "\"" + path + "\" is not a directory";
            }
            else if (readable && !pathObj.canRead())
            {
                sErrorMsg =
                    "\"" + path + "\" is not readble";
            }
            else if (writable && !pathObj.canWrite())
            {
                sErrorMsg =
                    "\"" + path + "\" is not writable";
            }
            else
            {
                retcode = true;
            }
        }
        catch (SecurityException securex)
        {
            sErrorMsg = "unable to access \"" + path + "\"";
        }

        return (retcode);
    } // end of isValidDirectory(String)

    /**
     * Returns {@code true} if the given string is a valid
     * access level for target language.
     * @param s access level string.
     * @param targetLanguage target language.
     * @return {@code true} if {@code s} is a valid
     * {@code targetLanguage} access level.
     */
    public static boolean isValidAccessLevel(final String s,
                                             final Language targetLanguage)
    {
        boolean retcode = sAccessMap.containsKey(targetLanguage);

        if (retcode)
        {
            retcode =
                (sAccessMap.get(targetLanguage)).contains(s);
        }

        return (retcode);
    } // end of isValidAccessLevel(String, Language)

    /**
     * Returns {@code true} if {@code castType} is a valid
     * C++ cast.
     * @param castType C++ cast type.
     * @return {@code true} if cast type is valid.
     */
    public static boolean isValidCast(final String castType)
    {
        return (castType.equals("dynamic_cast") == true ||
                castType.equals("static_cast") == true ||
                castType.equals("reinterpret_cast") == true);
    } // end of isValidCast(String)

    /**
     * Returns {@code true} if {@code glevel} is a valid graph
     * detail level.
     * @param glevel validate this graph detail level.
     * @return {@code true} if {@code glevel} is valid.
     */
    public static boolean isValidGraphLevel(final int glevel)
    {
        return (glevel >= SmcCodeGenerator.GRAPH_LEVEL_0 &&
                glevel <= SmcCodeGenerator.GRAPH_LEVEL_2);
    } // end of isValidGraphLevel(int)

    /**
     * Returns {@code name} portion from {@code path/name.sm}.
     * @param fullName full qualified file name.
     * @return base file name.
     */
    public static String getFileName(final String fullName)
    {
        File file = new File(fullName);
        String fileName = file.getName();

        // Note: this works because the file name's form
        // has already been validated as ending in .sm.
        return (
            fileName.substring(
                0, fileName.toLowerCase().indexOf(SM_SUFFIX)));
    } // end of getFileName(String)

    /**
     * Writes SMC output messages to the provided stream.
     * @param srcFileName SMC target file name.
     * @param stream write messages to this print stream.
     * @param messages output these messages.
     */
    public static void outputMessages(final String srcFileName,
                                      final PrintStream stream,
                                      final List<SmcMessage> messages)
    {
        for (SmcMessage message: messages)
        {
            stream.println(message);
        }

        return;
    } // end of outputMessages(String, PrintStream, List<>)

    // Parse the command line arguments and fill in the static
    // data accordingly.
    private static boolean parseArgs(final String[] args)
    {
        int i;
        int argsConsumed;
        boolean helpFlag;
        boolean retcode = true;

        // Look for either -help or -verson first. If specified,
        // then output the necessary info and return.
        helpFlag = needHelp(args);
        if (helpFlag == false)
        {
            // Look for the target language second. Verify that
            // exactly one target language is specifed.
            try
            {
                sTargetLanguage = findTargetLanguage(args);
            }
            catch (IllegalArgumentException argex)
            {
                retcode = false;
                sErrorMsg = argex.getMessage();
            }

            if (retcode == true && sTargetLanguage == null)
            {
                retcode = false;
                sErrorMsg = "Target language was not specified.";
            }
        }

        // Parse all options first. Keep going until an error is
        // encountered or there are no more options left.
        for (i = 0, argsConsumed = 0;
             i < args.length &&
                 helpFlag == false &&
                 retcode == true &&
                 args[i].startsWith("-") == true;
             i += argsConsumed, argsConsumed = 0)
        {
            // Ignore the target language flags - they have
            // been processed.
            if (findLanguage(args[i]) != null)
            {
                argsConsumed = 1;
            }
            else if (args[i].startsWith("-ac") == true)
            {
                // -access should be followed by a string.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        ACCESS_FLAG +
                        " not followed by an access keyword";
                }
                else if (supportsOption(ACCESS_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        ACCESS_FLAG +
                        ".";
                }
                else if (isValidAccessLevel(args[i+1]) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support access level" +
                        args[i+1] +
                        ".";
                }
                else
                {
                    sAccessLevel = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-sy") == true)
            {
                if (supportsOption(SYNC_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        SYNC_FLAG +
                        ".";
                }
                else
                {
                    sSync = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-su") == true)
            {
                // -suffix should be followed by a suffix.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        SUFFIX_FLAG + " not followed by a value";
                }
                else if (supportsOption(SUFFIX_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        SUFFIX_FLAG +
                        ".";
                }
                else
                {
                    sSuffix = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-hs") == true)
            {
                // -hsuffix should be followed by a suffix.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        HEADER_SUFFIX_FLAG +
                        " not followed by a value";
                }
                else if (supportsOption(
                             HEADER_SUFFIX_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        HEADER_SUFFIX_FLAG +
                        ".";
                }
                else
                {
                    sHSuffix = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-ca") == true)
            {
                // -cast should be followed by a cast type.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        CAST_FLAG +
                        " not followed by a value";
                }
                else if (supportsOption(CAST_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        CAST_FLAG +
                        ".";
                }
                else if (isValidCast(args[i+1]) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        "\"" +
                        args[i+1] +
                        "\" is an invalid C++ cast type.";
                }
                else
                {
                    sCastType = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-crtp") == true)
            {
                if (supportsOption(CRTP_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        CRTP_FLAG +
                        ".";
                }
                else
                {
                    sCRTP = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals("-d") == true)
            {
                // -d should be followed by a directory.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        DIRECTORY_FLAG +
                        " not followed by directory";
                }
                else if (
                    supportsOption(DIRECTORY_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        DIRECTORY_FLAG +
                        ".";
                }
                else
                {
                    sOutputDirectory = args[i+1];
                    argsConsumed = 2;

                    // If the output directory does not end with
                    // file path separator, then add one.
                    if (sOutputDirectory.endsWith(
                            File.separator) == false)
                    {
                        sOutputDirectory += File.separator;
                    }

                    retcode =
                        isValidDirectory(sOutputDirectory,
                                         false,
                                         true);
                }
            }
            else if (args[i].startsWith("-hea") == true)
            {
                // -headerd should be followed by a directory.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg = HEADER_FLAG +
                                " not followed by directory";
                }
                else if (
                    supportsOption(HEADER_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        HEADER_FLAG +
                        ".";
                }
                else
                {
                    sHeaderDirectory = args[i+1];
                    argsConsumed = 2;

                    // If the output directory does not end with
                    // file path separator, then add one.
                    if (sHeaderDirectory.endsWith(
                            File.separator) == false)
                    {
                        sHeaderDirectory += File.separator;
                    }

                    retcode =
                        isValidDirectory(sHeaderDirectory,
                                         false,
                                         true);
                }
            }
            else if (args[i].startsWith("-gl") == true)
            {
                // -glevel should be followed by an integer.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        GLEVEL_FLAG +
                        " not followed by integer";
                }
                else if (supportsOption(GLEVEL_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        GLEVEL_FLAG +
                        ".";
                }
                else
                {
                    try
                    {
                        sGraphLevel =
                            Integer.parseInt(args[i+1]);

                        if (!isValidGraphLevel(sGraphLevel))
                        {
                            retcode = false;
                            sErrorMsg =
                                GLEVEL_FLAG +
                                " must be 0, 1 or 2";
                        }
                        else
                        {
                            argsConsumed = 2;
                        }
                    }
                    catch (NumberFormatException numberex)
                    {
                        retcode = false;

                        sErrorMsg =
                            GLEVEL_FLAG +
                            " not followed by valid integer";
                    }
                }
            }
            else if (args[i].equals("-g") == true)
            {
                if (supportsOption(DEBUG_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        DEBUG_FLAG +
                        ".";
                }
                else
                {
                    sDebugLevel = SmcCodeGenerator.DEBUG_LEVEL_0;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals("-g0") == true)
            {
                if (supportsOption(DEBUG_LEVEL0_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        DEBUG_LEVEL0_FLAG +
                        ".";
                }
                else
                {
                    sDebugLevel = SmcCodeGenerator.DEBUG_LEVEL_0;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals("-g1") == true)
            {
                if (supportsOption(DEBUG_LEVEL1_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        DEBUG_LEVEL1_FLAG +
                        ".";
                }
                else
                {
                    sDebugLevel = SmcCodeGenerator.DEBUG_LEVEL_1;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-nos") == true)
            {
                if (supportsOption(NO_STREAMS_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        NO_STREAMS_FLAG +
                        ".";
                }
                else
                {
                    sNostreams = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-noe") == true)
            {
                if (supportsOption(NO_EXCEPTIONS_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        NO_EXCEPTIONS_FLAG +
                        ".";
                }
                else
                {
                    sNoex = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-noc") == true)
            {
                if (supportsOption(NO_CATCH_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        NO_CATCH_FLAG +
                        ".";
                }
                else
                {
                    sNocatch = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-stac") == true)
            {
                if (supportsOption(STACK_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        STACK_FLAG +
                        ".";
                }
                else if ((i + 1) == args.length ||
                         args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    sErrorMsg =
                        STACK_FLAG +
                        " not followed by an integer value.";
                }
                else
                {
                    try
                    {
                        sStateStackSize =
                            Integer.parseInt(args[i+1]);

                        if (sStateStackSize <= 0)
                        {
                            retcode = false;
                            sErrorMsg =
                                STACK_FLAG +
                                " not followed by an integer value > 0.";
                        }
                        else
                        {
                            argsConsumed = 2;
                        }
                    }
                    catch (NumberFormatException numberex)
                    {
                        retcode = false;
                        sErrorMsg =
                            STACK_FLAG +
                            " not followed by a valid integer.";
                    }
                }
            }
            else if (args[i].startsWith("-proto") == true)
            {
                if (supportsOption(USE_PROTOCOL_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        USE_PROTOCOL_FLAG +
                        ".";
                }
                else
                {
                    sProtocol = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-ret") == true)
            {
                if (supportsOption(RETURN_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        RETURN_FLAG +
                        ".";
                }
                else
                {
                    sReturn = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-ref") == true)
            {
                if (supportsOption(REFLECT_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        REFLECT_FLAG +
                        ".";
                }
                else
                {
                    sReflection = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals("-generic") == true)
            {
                if (supportsOption(GENERIC_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        GENERIC_FLAG +
                        ".";
                }
                else if (sGeneric == true)
                {
                    retcode = false;
                    sErrorMsg = GENERIC_FLAG + " already set.";
                }
                else
                {
                    sGeneric = true;
                    sJava7Flag = false;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals("-generic7") == true)
            {
                if (supportsOption(GENERIC7_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        GENERIC7_FLAG +
                        ".";
                }
                else if (sGeneric == true)
                {
                    retcode = false;
                    sErrorMsg = GENERIC_FLAG + " already set.";
                }
                else
                {
                    sGeneric = true;
                    sJava7Flag = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-se") == true)
            {
                if (supportsOption(SERIAL_FLAG) == false)
                {
                    retcode = false;
                    sErrorMsg =
                        sTargetLanguage.name() +
                        " does not support " +
                        SERIAL_FLAG +
                        ".";
                }
                else
                {
                    sSerial = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-verb") == true)
            {
                sVerbose = true;
                argsConsumed = 1;
            }
            else if (args[i].startsWith("-vverb") == true)
            {
                sFSMVerbose = true;
                argsConsumed = 1;
            }
            else
            {
                retcode = false;
                sErrorMsg = "Unknown option (" +
                             args[i] +
                             ")";
            }
        }

        // Was a state map target file given? It must be the
        // last argument in the list.
        if (helpFlag == false && retcode == true)
        {
            if (i == args.length)
            {
                retcode = false;
                sErrorMsg = "Missing source file";
            }
            else
            {
                File sourceFile;
                String fileName;

                for (; i < args.length && retcode == true; ++i)
                {
                    // The file name must end in ".sm".
                    if (args[i].toLowerCase().endsWith(".sm") ==
                            false)
                    {
                        retcode = false;
                        sErrorMsg =
                            "Source file name must end in " +
                            "\".sm\" (" +
                            args[i] +
                            ")";
                    }
                    else
                    {
                        sourceFile = new File(args[i]);
                        if (sourceFile.exists() == false)
                        {
                            retcode = false;
                            sErrorMsg = "No such file named \"" +
                                         args[i] +
                                         "\"";
                        }
                        else if (sourceFile.canRead() == false)
                        {
                            retcode = false;
                            sErrorMsg = "Source file \"" +
                                         args[i] +
                                         "\" is not readable";
                        }
                        else
                        {
                            // Normalize the file name. If the /
                            // file name separator is used and
                            // this is Windows, then replace with
                            // \.
                            fileName = args[i];
                            if (File.separatorChar != '/')
                            {
                                String fileSeparator =
                                    Matcher.quoteReplacement(
                                        File.separator);

                                fileName =
                                    fileName.replaceAll(
                                        "/", fileSeparator);
                            }

                            sSourceFileList.add(fileName);
                        }
                    }
                }
            }
        }

        return (retcode);
    } // end of parseArgs(String[])

    // Process the -help and -version flags separately.
    private static boolean needHelp(final String[] args)
    {
        int i;
        boolean retval = false;

        for (i = 0; i < args.length && retval == false; ++i)
        {
            if (args[i].startsWith("-hel") == true)
            {
                retval = true;
                usage(System.out);
            }
            else if (args[i].startsWith("-vers") == true)
            {
                retval = true;
                System.out.println(APP_NAME + " " + sVersion);
            }
        }

        return (retval);
    } // end of needHelp(String[])

    // Returns the target language found in the command line
    // arguments. Throws an IllegalArgumentException if more than
    // one target language is specified.
    // As a side effect sets the default suffix.
    private static Language findTargetLanguage(final String[] args)
    {
        int i;
        Language lang;
        Language retval = null;

        for (i = 0; i < args.length; ++i)
        {
            // Is this argument a language name?
            if ((lang = findLanguage(args[i])) != null)
            {
                // Only one target langugage can be specified.
                if (retval != null && retval != lang)
                {
                    throw (
                        new IllegalArgumentException(
                            "Only one target language " +
                            "may be specified"));
                }
                else
                {
                    retval = lang;
                }
            }
        }

        return (retval);
    } // end of findTargetLanguage(String[])

    /**
     * Returns the language record associated with the given
     * command line option.
     * @param option target language option.
     * @return language record.
     */
    private static Language findLanguage(final String option)
    {
        int index;
        Language retval = null;

        for (index = 1;
             index < sLanguages.length && retval == null;
             ++index)
        {
            if (option.equals(
                    sLanguages[index].optionFlag()) == true)
            {
                retval = sLanguages[index];
            }
        }

        return (retval);
    } // end of findLanguage(String)

    /**
     * Returns {@code true} if the target language supports the
     * specified option.
     * @param option command line option.
     * @return {@code true} if the command line option is
     * valid for the specified target language.
     */
    private static boolean supportsOption(final String option)
    {
        final List<Language> languages = sOptionMap.get(option);

        return (languages != null &&
                languages.contains(sTargetLanguage));
    } // end of supportsOption(String)

    // Returns true if the string is a valid access level for
    // the target language.
    private static boolean isValidAccessLevel(final String s)
    {
        boolean retcode =
            sAccessMap.containsKey(sTargetLanguage);

        if (retcode)
        {
            final List<String> levels =
                sAccessMap.get(sTargetLanguage);

            retcode = levels.contains(s);
        }

        return (retcode);
    } // end of isValidAccessLevel(String)

    private static void usage(final PrintStream stream)
    {
        stream.print("usage: ");
        stream.print(APP_NAME);
        stream.print(" [-access level]");
        stream.print(" [-suffix suffix]");
        stream.print(" [-g | -g0 | -g1]");
        stream.print(" [-nostreams]");
        stream.print(" [-crtp]");
        stream.print(" [-version]");
        stream.print(" [-verbose]");
        stream.print(" [-vverbose]");
        stream.print(" [-help]");
        stream.print(" [-sync]");
        stream.print(" [-noex]");
        stream.print(" [-nocatch]");
        stream.print(" [-stack max-stack-depth]");
        stream.print(" [-protocol]");
        stream.print(" [-serial]");
        stream.print(" [-return]");
        stream.print(" [-reflect]");
        stream.print(" [-generic]");
        stream.print(" [-generic7]");
        stream.print(" [-cast cast_type]");
        stream.print(" [-d directory]");
        stream.print(" [-headerd directory]");
        stream.print(" [-hsuffix suffix]");
        stream.print(" [-glevel int]");
        stream.print(
            " {-c | -c++ | -csharp | -graph | -groovy | -java | ");
        stream.print(
            "-java7 | -js -lua | -objc | -perl | -php | -python | ");
        stream.print("-ruby | -scala | -table |-tcl | -vb}");
        stream.println(" statemap_file");
        stream.println("    where:");
        stream.println(
            "\t-access   Use this access keyword for the generated classes");
        stream.println("\t          (use with -java, -java7 only)");
        stream.println(
            "\t-suffix   Add this suffix to output file");
        stream.println(
            "\t-g, -g0   Add level 0 debugging output to generated code");
        stream.println(
            "\t          (output for entering, exiting states and transitions)");
        stream.println(
            "\t-g1       Add level 1 debugging output to generated code");
        stream.println(
            "\t          (level 0 output plus state Entry and Exit actions)");
        stream.println("\t-nostreams Do not use C++ iostreams");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.println("\t-crtp     Generate state machine using CRTP");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.print("\t-version  Print smc version ");
        stream.println("information to standard out and exit");
        stream.print("\t-verbose  ");
        stream.println("Output compiler messages (SMC is silent by default).");
        stream.print("\t-vverbose  ");
        stream.println("Output more compiler messages.");
        stream.print("\t-help     Print this message to ");
        stream.println("standard out and exit");
        stream.println(
            "\t-sync     Synchronize access to transition methods");
        stream.print("\t          ");
        stream.println("(use with -csharp, -java, -java7, -groovy, -scala and -vb only)");
        stream.println(
            "\t-noex     Do not generate C++ exception throws ");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.print(
            "\t-nocatch  Do not generate try/catch/rethrow ");
        stream.println("code (not recommended)");
        stream.println("\t-stack    Specifies a fixed-size state stack");
        stream.print("\t          ");
        stream.println("using no dynamic memory allocation.");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.println(
            "\t-protocol FSM context extends a @protocol and referenced via protocol");
        stream.print("\t          ");
        stream.println("(use with -objc only)");
        stream.println(
            "\t-serial   Generate serialization code");
        stream.print("\t-return   ");
        stream.println("Smc.main() returns, not exits");
        stream.print("\t          ");
        stream.println("(use this option with ANT)");
        stream.println("\t-reflect  Generate reflection code");
        stream.print("\t          ");
        stream.print("(use with -csharp, -groovy, -java, -java7, -js, -lua,");
        stream.print(" -perl, -php, -python, -ruby, -scala, ");
        stream.println("-tcl and -vb only)");
        stream.println("\t-generic  Use generic collections");
        stream.print("\t          ");
        stream.println("(use with -csharp, -java or -vb and -reflect only)");
        stream.println("\t-generic7  Use Java 7 generic collections");
        stream.print("\t          ");
        stream.println("(use with -java  and -reflect only)");
        stream.println("\t-cast     Use this C++ cast type ");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.println(
            "\t-d        Place generated files in directory");
        stream.print(
            "\t-headerd  Place generated header files in ");
        stream.println("directory");
        stream.print("\t          ");
        stream.println("(use with -c, -c++, -objc only)");
        stream.println(
            "\t-hsuffix  Add this suffix to output header file");
        stream.print("\t          ");
        stream.println("(use with -c, -c++, -objc only)");
        stream.print(
            "\t-glevel   Detail level from 0 (least) to 2 ");
        stream.println("(greatest)");
        stream.print("\t          ");
        stream.println("(use with -graph only)");
        stream.println("\t-c        Generate C code");
        stream.println("\t-c++      Generate C++ code");
        stream.println("\t-csharp   Generate C# code");
        stream.println("\t-graph    Generate GraphViz DOT file");
        stream.println("\t-groovy   Generate Groovy code");
        stream.println("\t-java     Generate Java code");
        stream.println("\t-java7    Generate Java code as a transition table");
        stream.println("\t-js       Generate JavaScript code");
        stream.println("\t-lua      Generate Lua code");
        stream.println("\t-objc     Generate Objective-C code");
        stream.println("\t-perl     Generate Perl code");
        stream.println("\t-php      Generate PHP code");
        stream.println("\t-python   Generate Python code");
        stream.println("\t-ruby     Generate Ruby code");
        stream.println("\t-scala    Generate Scala code");
        stream.println("\t-table    Generate HTML table code");
        stream.println("\t-tcl      Generate [incr Tcl] code");
        stream.println("\t-vb       Generate VB.Net code");
        stream.println();
        stream.println(
            "    Note: statemap_file must end in \".sm\"");
        stream.print(
            "    Note: must select one of -c, -c++, -csharp, ");
        stream.print("-graph, -groovy, -java, -java7, -lua, -objc, -perl, ");
        stream.println(
            "-php, -python, -ruby, -scala, -table, -tcl or -vb.");

        return;
    } // end of usage(PrintStream)

    // Generates the State pattern in the target language.
    private static void generateCode(final SmcFSM fsm)
        throws FileNotFoundException,
               IOException,
               ParseException
    {
        final int endIndex =
            sSourceFileName.lastIndexOf(File.separatorChar);
        String targetFilePath;
        String targetFileBase = fsm.getTargetFileName();
        String headerPath;
        String headerFileName = "";
        FileOutputStream headerFileStream = null;
        PrintStream headerStream;
        SmcCodeGenerator headerGenerator = null;
        String srcFileName;
        FileOutputStream sourceFileStream;
        PrintStream sourceStream;
        SmcOptions options;
        SmcCodeGenerator generator;

        // For some strange reason I get the wrong
        // line separator character when I use Java
        // on Windows. Set the line separator to "\n"
        // and all is well.
        System.setProperty("line.separator", "\n");

        // If -d was specified, then place generated file
        // there.
        if (sOutputDirectory != null)
        {
            targetFilePath = sOutputDirectory;
        }
        // Strip away any preceding directories from the target
        // file name and use that as the target file path.
        else if (endIndex >= 0)
        {
            // Note: this substring includes the file separator
            // at the end because endIndex points to that
            // character.
            targetFilePath =
                sSourceFileName.substring(
                    0, (endIndex + 1));
        }
        // If there are no preceeding directories, then put the
        // generated code in the current working directory.
        else
        {
            targetFilePath = "";
        }

        // If -headerd was specified, then place the file
        // there. -headerd takes precedence over -d.
        if (sHeaderDirectory != null)
        {
            headerPath = sHeaderDirectory;
        }
        else
        {
            headerPath = targetFilePath;
        }

        if (sAccessLevel == null)
        {
            sAccessLevel = "public";
        }
        else if (sAccessLevel.equals(PACKAGE_LEVEL))
        {
            sAccessLevel = "/* package */";
        }

        // If the target language is Java7, then turn on
        // the java7 flag.
        sJava7Flag =
            (sTargetLanguage.language() == TargetLanguage.JAVA7);

        options = new SmcOptions(APP_NAME,
                                 VERSION,
                                 fsm.getSourceFileName(),
                                 targetFileBase,
                                 targetFilePath,
                                 headerPath,
                                 sHSuffix,
                                 sCastType,
                                 sGraphLevel,
                                 sSerial,
                                 sDebugLevel,
                                 sNoex,
                                 sNocatch,
                                 sNostreams,
                                 sCRTP,
                                 sStateStackSize,
                                 sReflection,
                                 sSync,
                                 sGeneric,
                                 sJava7Flag,
                                 sAccessLevel,
                                 sProtocol);

        // Create the header file name and generator -
        // if the language uses a header file.
        if (sTargetLanguage.hasHeaderFile())
        {
            headerGenerator =
                sTargetLanguage.headerGenerator(options);
            headerFileName =
                headerGenerator.setTargetFile(
                    headerPath, targetFileBase, sHSuffix);
            headerFileStream =
                new FileOutputStream(headerFileName);
            headerStream =
                new PrintStream(headerFileStream);
            headerGenerator.setTarget(headerStream);
        }

        // Create the language-specific target code generator.
        generator = sTargetLanguage.generator(options);
        srcFileName =
            generator.setTargetFile(
                targetFilePath, targetFileBase, sSuffix);
        sourceFileStream =
            new FileOutputStream(srcFileName);
        sourceStream =
            new PrintStream(sourceFileStream);
        generator.setTarget(sourceStream);

        // Generate the header file first.
        if (headerGenerator != null && headerFileStream != null)
        {
            fsm.accept(headerGenerator);
            headerFileStream.flush();
            headerFileStream.close();

            if (sVerbose == true)
            {
                System.out.print("[wrote ");
                System.out.print(headerFileName);
                System.out.println("]");
            }
        }

        // Now output the FSM in the target language.
        fsm.accept(generator);
        sourceFileStream.flush();
        sourceFileStream.close();

        if (sVerbose == true)
        {
            System.out.print("[wrote ");
            System.out.print(srcFileName);
            System.out.println("]");
        }

        return;
    } // end of generateCode(SmcFSM)

//---------------------------------------------------------------
// Inner classes
//

    /**
     * This class explicitly stores each target language's
     * properties:
     * <ul>
     *   <li>
     *     the <em>start</em> of the command line option,
     *   </li>
     *   <li>
     *     the language's full name,
     *   </li>
     *   <li>
     *     the language's {@code SmcCodeGenerator} subclass, and
     *   </li>
     *   <li>
     *     whether the language also generates a header file and
     *     the header fill {@code SmcCodeGenerator} subclass.
     *   </li>
     * </ul>
     * <p>
     * The reason the above information is stored in this
     * class and not in the {@code TargetLanguage} enum is due
     * to the code generator classes. If these classes were
     * stored in {@code TargetLanguage}, this would create a
     * circular dependency between the model and generator
     * modules.
     * </p>
     */
    public static final class Language
    {
    //-----------------------------------------------------------
    // Member data.
    //

        //-------------------------------------------------------
        // Locals.
        //

        private final TargetLanguage mLanguage;
        private final String mOptionFlag;
        private final String mName;
        private final Constructor mGenerator;
        private final Constructor mHeaderGenerator;

    //-----------------------------------------------------------
    // Member methods.
    //

        //-------------------------------------------------------
        // Constructors.
        //

        /**
         * Creates a new language instance for the given
         * properties.
         * @param language the target language enum value.
         * @param optionFlag command line option flag for this
         * language.
         * @param name language print name.
         * @param generator code generator class.
         * @param headerGenerator header code generator class.
         */
        @SuppressWarnings("unchecked")
        public Language(final TargetLanguage language,
                        final String optionFlag,
                        final String name,
                        final Class generator,
                        final Class headerGenerator)
        {
            Constructor sourceCtor = null;
            Constructor headerCtor = null;

            mLanguage = language;
            mOptionFlag = optionFlag;
            mName = name;

            if (generator != null)
            {
                try
                {
                    sourceCtor =
                        generator.getDeclaredConstructor(
                            SmcOptions.class);
                }
                catch (NoSuchMethodException methodex)
                {}
            }

            if (headerGenerator != null)
            {
                try
                {
                    headerCtor =
                        headerGenerator.getDeclaredConstructor(
                            SmcOptions.class);
                }
                catch (NoSuchMethodException methoex)
                {}
            }

            mGenerator = sourceCtor;
            mHeaderGenerator = headerCtor;
        } // end of Language(...)

        //
        // end of Constructors.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Object Method Overrides.
        //

        /**
         * Returns {@code true} if {@code o} is a
         * {@code Language} instance referencing the same
         * {@code TargetLanguage}.
         * @param o check equality with {@code this Language}
         * instance.
         * @return {@code true} if {@code o} is for the same
         * {@code TargetLanguage} as {@code this} instance.
         */
        @Override
        public boolean equals(final Object o)
        {
            boolean retcode = (o == this);

            if (retcode == false && o instanceof Language)
            {
                retcode =
                    (mLanguage == ((Language) o).mLanguage);
            }

            return (retcode);
        } // end of equals(Object)

        /**
         * Returns the {@code TargetLanguage} ordinal value as
         * the hash code.
         * @return unique hash code for this {@code Language}.
         */
        @Override
        public int hashCode()
        {
            return (mLanguage.ordinal());
        } // end of hashCode()

        /**
         * Returns the full language name.
         * @return print name.
         */
        @Override
        public String toString()
        {
            return (mName);
        } // end of toString()

        //
        // end of Object Method Overrides.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Get Methods.
        //

        /**
         * Returns the associated {@code TargetLanguage}.
         * @return {@code TargetLanguage}.
         */
        public TargetLanguage language()
        {
            return (mLanguage);
        } // end of language()

        /**
         * Returns the command line option string for this
         * language.
         * @return language command line option.
         */
        public String optionFlag()
        {
            return (mOptionFlag);
        } // end of optionFlag()

        /**
         * Returns the full language name.
         * @return print name.
         */
        public String name()
        {
            return (mName);
        } // end of name()

        /**
         * Returns an instance of the source code generator.
         * @param options SMC command line options.
         * @return source code generator instance.
         */
        public SmcCodeGenerator generator(final SmcOptions options)
        {
            SmcCodeGenerator retval = null;

            try
            {
                retval =
                    (SmcCodeGenerator)
                        mGenerator.newInstance(options);
            }
            catch (IllegalAccessException |
                   IllegalArgumentException |
                   InstantiationException |
                   InvocationTargetException jex)
            {
                System.err.print(options.srcfileBase());
                System.err.print(".sm: failed to create ");
                System.err.print(mLanguage);
                System.err.println(" generator:");
                jex.printStackTrace(System.err);
            }

            return (retval);
        } // end of generator(SmcOptions)

        /**
         * Returns {@code true} if this language uses header
         * files.
         * @return {@code true} if language has headers.
         */
        public boolean hasHeaderFile()
        {
            return (mHeaderGenerator != null);
        } // end of hasHeaderFile()

        /**
         * Returns an instance of the header code generator.
         * @param options SMC command line options.
         * @return header code generator instance.
         */
        public SmcCodeGenerator headerGenerator(final SmcOptions options)
        {
            SmcCodeGenerator retval = null;

            try
            {
                retval =
                    (SmcCodeGenerator)
                        mHeaderGenerator.newInstance(options);
            }
            catch (IllegalAccessException |
                   IllegalArgumentException |
                   InstantiationException |
                   InvocationTargetException jex)
            {
                // Ignore. Return null.
            }

            return (retval);
        } // end of headerGenerator(Options)

        //
        // end of Get Methods.
        //-------------------------------------------------------
    } // end of class Language
} // end of class Smc

//
// CHANGE LOG
// Log: Smc.java,v
// Revision 1.45  2013/12/15 16:31:41  fperrad
// full refactor of JavaScript
//
// Revision 1.44  2013/09/02 14:45:57  cwrapp
// SMC 6.3.0 commit.
//
// Revision 1.43  2013/07/14 14:32:37  cwrapp
// check in for release 6.2.0
//
// Revision 1.42  2011/11/20 16:29:53  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.41  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.40  2011/02/14 21:29:56  nitin-nizhawan
// corrected some build errors
//
// Revision 1.38  2010/02/15 18:03:17  fperrad
// fix 2950619 : make distinction between target filename (*.sm) and target filename.
//
// Revision 1.37  2009/12/17 19:51:43  cwrapp
// Testing complete.
//
// Revision 1.36  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.35  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.34  2009/09/12 21:26:58  kgreg99
// Bug #2857745 resolved. Messages are printed not in case of an error but if there are any.
//
// Revision 1.33  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.32  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.31  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.30  2008/08/15 22:20:40  fperrad
// + move method escape in SmcGraphGenerator.java
//
// Revision 1.29  2008/05/20 18:31:14  cwrapp
// ----------------------------------------------------------------------
//
// Committing release 5.1.0.
//
// Modified Files:
// 	Makefile README.txt smc.mk tar_list.txt bin/Smc.jar
// 	examples/Ant/EX1/build.xml examples/Ant/EX2/build.xml
// 	examples/Ant/EX3/build.xml examples/Ant/EX4/build.xml
// 	examples/Ant/EX5/build.xml examples/Ant/EX6/build.xml
// 	examples/Ant/EX7/build.xml examples/Ant/EX7/src/Telephone.java
// 	examples/Java/EX1/Makefile examples/Java/EX4/Makefile
// 	examples/Java/EX5/Makefile examples/Java/EX6/Makefile
// 	examples/Java/EX7/Makefile examples/Ruby/EX1/Makefile
// 	lib/statemap.jar lib/C++/statemap.h lib/Java/Makefile
// 	lib/Php/statemap.php lib/Scala/Makefile
// 	lib/Scala/statemap.scala net/sf/smc/CODE_README.txt
// 	net/sf/smc/README.txt net/sf/smc/Smc.java
// ----------------------------------------------------------------------
//
// Revision 1.28  2008/04/22 16:05:24  fperrad
// - add PHP language (patch from Toni Arnold)
//
// Revision 1.27  2008/03/21 14:03:16  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.26  2008/02/04 10:32:49  fperrad
// + Added Scala language generation.
//
// Revision 1.25  2008/01/14 19:59:23  cwrapp
// Release 5.0.2 check-in.
//
// Revision 1.24  2008/01/04 20:40:40  cwrapp
// Corrected minor misspellings and incorrect information.
//
// Revision 1.23  2007/08/05 13:53:09  cwrapp
// Version 5.0.1 check-in. See net/sf/smc/CODE_README.txt for more information.
//
// Revision 1.22  2007/07/16 06:28:06  fperrad
// + Added Groovy generator.
//
// Revision 1.21  2007/02/21 13:53:38  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.20  2007/02/13 18:43:19  cwrapp
// Reflect options fix.
//
// Revision 1.19  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.18  2007/01/03 15:37:38  fperrad
// + Added Lua generator.
// + Added -reflect option for Lua, Perl, Python and Ruby code generation
//
// Revision 1.17  2006/09/23 14:28:18  cwrapp
// Final SMC, v. 4.3.3 check-in.
//
// Revision 1.16  2006/09/16 15:04:28  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.15  2006/07/11 18:20:00  cwrapp
// Added -headerd option. Improved command line processing.
//
// Revision 1.14  2006/04/22 12:45:26  cwrapp
// Version 4.3.1
//
// Revision 1.13  2005/11/07 19:34:54  cwrapp
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
// Revision 1.12  2005/09/19 15:20:03  cwrapp
// Changes in release 4.2.2:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (C#) -csharp not generating finally block closing brace.
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
// Revision 1.9  2005/07/07 12:08:44  fperrad
// Added C, Perl & Ruby generators.
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
// Revision 1.8  2005/02/21 15:34:25  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.7  2005/02/21 15:09:07  charlesr
// Added -python and -return command line options. Also added an
// undocuments option -vverbose which causes the SmcParser and
// SmcLexer FSMs to enter verbose mode.
//
// Revision 1.6  2005/02/03 16:26:44  charlesr
// SMC now implements the Visitor pattern. The parser returns
// an SmcFSM object which is an SmcElement subclass. SMC then
// creates the appropriate visitor object based on the target
// language and passes the visitor to SmcElement.accept().
// This starts the code generation process.
//
// One minor point: the lexer and parser objects no longer
// write warning and error messages directly to System.err.
// Instead, these messages are collected as SmcMessage objects.
// It is then up to the application calling the parser to
// decide how to display this information. Now the SMC
// application writes these messages to System.err as before.
// This change allows the parser to be used in different
// applications.
//
// Revision 1.5  2004/10/30 16:02:24  charlesr
// Added Graphviz DOT file generation.
// Changed version to 3.2.0.
//
// Revision 1.4  2004/10/08 18:56:07  charlesr
// Update version to 3.1.2.
//
// Revision 1.3  2004/10/02 19:50:24  charlesr
// Updated version string.
//
// Revision 1.2  2004/09/06 16:39:16  charlesr
// Added -verbose and -d options. Added C# support.
//
// Revision 1.1  2004/05/31 13:52:56  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:02:45  charlesr
// Initial revision
//
