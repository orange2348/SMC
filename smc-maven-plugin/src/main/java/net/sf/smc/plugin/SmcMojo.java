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
package net.sf.smc.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.sf.smc.Smc;
import net.sf.smc.Smc.Language;
import net.sf.smc.SmcSyntaxChecker;
import net.sf.smc.generator.SmcCodeGenerator;
import net.sf.smc.generator.SmcOptions;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.TargetLanguage;
import net.sf.smc.parser.SmcMessage;
import net.sf.smc.parser.SmcParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Responsible for executing the state machine compiler against
 * one or more {@code .sm} files found in the configured
 * {@link #setSourceDirectory} using the configured {@code SMC}
 * parameters. The generated file(s) are placed in
 * {@link #setTargetDirectory}.
 * <p>
 * This mojo has the side effect of adding the
 * {@code targetDirectory} to the project compile source root.
 * This allows application code to successfully reference the
 * generated code.
 * </p>
 *
 * @author <a href="mailto:rapp@acm.org">Charles W. Rapp</a>
 */

@Mojo(name = "smc",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class SmcMojo
    extends AbstractMojo
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * SMC target file names must end with {@value}.
     */
    public static final String SM_SUFFIX = ".sm";

    /**
     * Set {@link #debugLevel} to {@value} to silence SMC debug
     * output.
     */
    public static final String NO_DEBUG_OUTPUT = "-1";

    /**
     * The default graph detail level is zero (least detail).
     */
    public static final String DEFAULT_GRAPH_LEVEL = "0";

    /**
     * There are {@value} nanoseconds per millisecond.
     */
    private static final int NANOS_PER_MILLI = 1_000_000;

    //-----------------------------------------------------------
    // Locals.
    //

    //
    // NOTE: Since this is a maven plug-in, the following
    // local data members are configured via injection by
    // the maven build process.
    //

    /**
     * Output the generated finite state machine in this target
     * language. The defined target languages are:
     * <ul>
     *   <li>
     *     c
     *   </li>
     *   <li>
     *     c++
     *   </li>
     *   <li>
     *     csharp (C#)
     *   </li>
     *   <li>
     *     graph (Generates Graphviz DOT file)
     *   </li>
     *   <li>
     *     groovy
     *   </li>
     *   <li>
     *     java
     *   </li>
     *   <li>
     *     java7 (generates Java code using transition table)
     *   </li>
     *   <li>
     *     js (Javascript)
     *   </li>
     *   <li>
     *     lua
     *   </li>
     *   <li>
     *     objc (Objective-C)
     *   </li>
     *   <li>
     *     perl
     *   </li>
     *   <li>
     *     php
     *   </li>
     *   <li>
     *     python
     *   </li>
     *   <li>
     *     ruby
     *   </li>
     *   <li>
     *     scala
     *   </li>
     *   <li>
     *     tcl (as [incr Tcl])
     *   </li>
     *   <li>
     *     vb (VB.Net)
     *   </li>
     * </ul>
     * When selecting a target language, use the names as shown.
     */
    @Parameter(property = "targetLanguage",
               required = true)
    private String targetLanguage;

    /**
     * Source directory containing the {@code .sm} files.
     */
    @Parameter(property = "sourceDirectory",
               required = true,
               defaultValue = "${project.basedir}/src/main/smc")
    private File sourceDirectory;

    /**
     * Place generated files into this directory. This directory
     * is automatically added to the project compile target root.
     */
    @Parameter(property = "targetDirectory",
               required = true,
               defaultValue = "${project.build.directory}/generated-sources/smc")
    private File targetDirectory;

    /**
     * Compiles the following target files. If this array is
     * empty, then it is filled with all {@code .sm} files found
     * in {@link #sourceDirectory}. Specified target files names
     * must be relative to {@link #sourceDirectory}.
     */
    @Parameter(property = "sources")
    private String[] sources;

    /**
     * Append generated target file(s) with this suffix. If not
     * specified, then the default suffix for
     * {@link #targetLanguage} is used.
     */
    @Parameter(property = "suffix")
    private String suffix;

    /**
     * Append generated <em>header</em> target file(s) with this
     * suffix. If not specified, then the default header suffix
     * for {@link #targetLanguage} is used. This parameter is
     * ignored if {@link #targetLanguage} does not use header
     * files.
     * <p>
     * Available for c, c++, and objc only.
     * </p>
     */
    @Parameter(property = "hsuffix")
    private String hsuffix;

    /**
     * Places generated header files into this directory. This
     * directory is automatically added to the project compile
     * target root.
     * <p>
     * Available for c, c++, and objc only.
     * </p>
     */
    @Parameter(property = "headerd")
    private File headerd;

    /**
     * Adds SMC debug output to generated code. There are two
     * levels:
     * <ul>
     *   <li>
     *     0: state entry, exit, and transitions.
     *   </li>
     *   <li>
     *     1: level 0 output plus state entry, exit actions.
     *   </li>
     * </ul>
     * This is turned off by default.
     */
    @Parameter(property = "debugLevel",
               defaultValue = NO_DEBUG_OUTPUT)
    private int debugLevel;

    /**
     * If {@code true}, do not use C++ iostreams for debugging.
     * Application code must provide a {@code TRACE} macro to
     * output debug messages.
     * <p>
     * Available for c++ only.
     * </p>
     */
    @Parameter(property = "nostreams", defaultValue = "false")
    private boolean nostreams;

    /**
     * If {@code true} user supplied class must be derives from
     * state machine.
     * <p>
     * See CRTP ("curiously recurring template pattern").
     * </p>
     * <p>
     * Available for c++ only.
     * </p>
     */
    @Parameter(property = "crtp", defaultValue = "false")
    private boolean crtp;

    /**
     * If {@code true} generates thread-safe code. This is not
     * necessary if application guarantees FSM access is
     * thread-safe.
     * <p>
     * Available for csharp, java, java7, groovy, scala, and vb
     * only.
     * </p>
     */
    @Parameter(property = "sync", defaultValue = "false")
    private boolean sync;

    /**
     * If {@code true}, do <em>not</em> generate C++ exception
     * throws.
     * <p>
     * Available for c++ only.
     * </p>
     */
    @Parameter(property = "noex", defaultValue = "false")
    private boolean noex;

    /**
     * If {@code true}, do <em>not</em> generate
     * {@code try/catch/rethrow} code. The {@code try/catch}
     * is used to guarantee that the current state is set before
     * allowing the exception throw to continue. If current FSM
     * state is left {@code null}, then the FSM is broken and
     * ceases to function.
     * <p>
     * Setting this property to {@code true} is
     * <em><strong>not</strong></em> recommended.
     * </p>
     */
    @Parameter(property = "nocatch", defaultValue = "false")
    private boolean nocatch;

    /**
     * If &gt; zero, state stack has a fixed size defined by this
     * setting. Otherwise, state stack size is unlimited. This
     * setting means that dynamic memory allocation is not used
     * when setting the stack.
     * <p>
     * Available for c++ only.
     * </p>
     */
    @Parameter(property = "stack", defaultValue = "0")
    private int stateStackSize;

    /**
     * If {@code true}, generate unique integer identifiers for
     * each state. These identifiers are used to efficiently
     * serialize the current state stack.
     */
    @Parameter(property = "serial", defaultValue = "false")
    private boolean serial;

    /**
     * If {@code true}, generate {@code getTransitions()} method
     * for each state, allowing the state to be interrogated
     * about its supported transition.
     * <p>
     * Available for csharp, groovy, java, java7, js, lua, perl,
     * php, python, ruby, scala, tcl, and vb only.
     * </p>
     */
    @Parameter(property = "reflect", defaultValue = "false")
    private boolean reflection;

    /**
     * If {@code true}, use a
     * <code>Map&lt;String, Integer&gt;</code> for reflection
     * map.
     * <p>
     * Available for csharp, java, or vb only when
     * {@code reflect} property is set to {@code true}.
     * </p>
     */
    @Parameter(property = "generic", defaultValue = "false")
    private boolean generic;

    /**
     * Use Java 7 generic collections for reflection map.
     * <p>
     * Available for java, or java7 only when
     * {@code reflect} property is set to {@code true}.
     * </p>
     */
    @Parameter(property = "generic7", defaultValue = "false")
    private boolean generic7;

    /**
     * If {@code true}, SMC outputs more detailed messages.
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose;

    /**
     * If {@code true}, SMC outputs even more detailed messages.
     */
    @Parameter(property = "vverbose", defaultValue = "false")
    private boolean vverbose;

    /**
     * Specifies the Graphviz output detail level. Ranges from
     * 0 (least detail) to 2 (most detail). See SMC Programmer's
     * Manual, section 10 for an explanation on how to generate
     * Graphviz-based FSM diagram.
     * <p>
     * Available for graph only.
     * </p>
     */
    @Parameter(property = "glevel",
               defaultValue = DEFAULT_GRAPH_LEVEL)
    private int glevel;

    /**
     * When generating C++ code, use this cast type
     * ({@code dynamic_cast}, {@code static_cast}, or
     * {@code reinterpret_cast}).
     * <p>
     * Available for c++ only.
     * </p>
     */
    @Parameter(property = "cast")
    private String cast;

    /**
     * Access identifier for generated classes.
     * <p>
     * Available for java, java7 only.
     * </p>
     */
    @Parameter(property = "access", defaultValue = "")
    private String access;

    /**
     * User-defined FSM context class extends {@code @protocol}.
     * Generated code references context class via protocol.
     * <p>
     * Available for objc only.
     * </p>
     */
    @Parameter(property = "protocol", defaultValue = "false")
    private boolean protocol;

    /**
     * This parameter is set by maven.
     */
    @Parameter(defaultValue = "${project}",
               readonly = true)
    private MavenProject project;

    //
    // The remaining local data members are set by this class.
    //

    /**
     * Language record associated with {@link #targetLanguage}.
     * This value is indirectly set by
     * {@link #validateSettings()}.
     */
    private Language mTargetLanguage;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a new SMC maven mojo instance.
     */
    public SmcMojo()
    {}

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // AbstractMojo Abstract Method Overrides.
    //

    /**
     * Firstly validates the mojo settings. Secondly compiles
     * the .sm files, validates the parsed code, and finally
     * emits the target language code to the specified source
     * file.
     * <p>
     * Executing this mojo has the side effect of adding the
     * target directory to the project compile source root. This
     * allows application code to reference this generated
     * source code.
     * </p>
     * @throws MojoExecutionException
     * if an invalid setting is detected or the FSM compilation
     * fails. No target code is emitted.
     * @throws MojoFailureException
     * this exception is never thrown.
     */
    @Override
    public void execute()
        throws MojoExecutionException,
               MojoFailureException
    {
        Instant startTime = Instant.now();

        if (getLog().isDebugEnabled())
        {
            outputSettings();
        }

        // 1. Validate that the parameters are valid. If the
        //    settings are *not* valid, then validSettings()
        //    throws a MojoExecutionException.
        validateSettings();

        // 2. Compile the target files, each in turn.
        compileAll();

        if (verbose)
        {
            final Instant finishTime = Instant.now();
            final Duration delta =
                Duration.between(startTime, finishTime);

            getLog().info(
                String.format(
                    "[total %d.%03d ms]",
                    delta.getSeconds(),
                    (delta.getNano() / NANOS_PER_MILLI)));
        }

        // If the target language uses headers and the header
        // target directory is different than the target target
        // directory, then add the header target directory to the
        // maven target root.
        // Otherwise the target target directory will be added
        // next.
        if (mTargetLanguage.hasHeaderFile() &&
            !sourceDirectory.equals(headerd))
        {
            project.addCompileSourceRoot(
                headerd.getAbsolutePath());
        }

        // Add the target directory to the maven target root.
        project.addCompileSourceRoot(
            targetDirectory.getAbsolutePath());

        return;
    } // end of execute()

    //
    // end of AbstractMojo Abstract Method Overrides.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set Methods.
    //

    /**
     * Output the generated finite state machine in this target
     * language. This setting is required.
     * <p>
     * The defined target languages are:
     * </p>
     * <ul>
     *   <li>
     *     c
     *   </li>
     *   <li>
     *     c++
     *   </li>
     *   <li>
     *     csharp (C#)
     *   </li>
     *   <li>
     *     graph (Generates Graphviz DOT file)
     *   </li>
     *   <li>
     *     groovy
     *   </li>
     *   <li>
     *     java
     *   </li>
     *   <li>
     *     java7 (generates Java code using transition table)
     *   </li>
     *   <li>
     *     js (Javascript)
     *   </li>
     *   <li>
     *     lua
     *   </li>
     *   <li>
     *     objc (Objective-C)
     *   </li>
     *   <li>
     *     perl
     *   </li>
     *   <li>
     *     php
     *   </li>
     *   <li>
     *     python
     *   </li>
     *   <li>
     *     ruby
     *   </li>
     *   <li>
     *     scala
     *   </li>
     *   <li>
     *     tcl (as [incr Tcl])
     *   </li>
     *   <li>
     *     vb (VB.Net)
     *   </li>
     * </ul>
     * When selecting a target language, use the names as shown.
     * @param language target language name.
     */
    public void setTargetLanguage(final String language)
    {
        this.targetLanguage = language;
    } // end of setTargetLanguage(String)

    /**
     * Source directory containing the {@code .sm} files. This
     * setting is optional and defaults to
     * {@code ${project.basedir}/src/main/smc}.
     * @param dir source directory.
     */
    public void setSourceDirectory(final File dir)
    {
        this.sourceDirectory = dir;
    } // end of setSourceDirectory(File)

    /**
     * Place generated files into this directory. This directory
     * is automatically added to the project compile target root.
     * This setting is optional and defaults to
     * {@code ${project.build.directory}/generated-sources/smc}.
     * @param dir target directory.
     */
    public void setTargetDirectory(final File dir)
    {
        this.targetDirectory = dir;
    } // end of setTargetDirectory(File)

    /**
     * Compiles the following target files. If this array is
     * empty, then it is filled with all {@code .sm} files found
     * in {@link #setSourceDirectory}. Specified target files
     * names must be relative to {@link #setSourceDirectory}.
     * @param sources source file names.
     */
    public void setSources(final String[] sources)
    {
        this.sources = sources;
    } // end of setSources(String[])

    /**
     * Append generated target file(s) with this suffix. If not
     * specified, then the default suffix for
     * {@link #setTargetLanguage} is used.
     * @param suffix generated source file name suffix.
     */
    public void setSuffix(final String suffix)
    {
        this.suffix = suffix;
    } // end of setSuffix(String)

    /**
     * Appends generated <em>header</em> target file(s) with this
     * suffix. If not specified, then the default header suffix
     * for {@link #setTargetLanguage} is used. This parameter is
     * ignored if {@link #setTargetLanguage} does not use header
     * files.
     * <p>
     * Available for c, c++, and objc only.
     * </p>
     * @param headerSuffix generated header file name suffix.
     */
    public void setHsuffix(final String headerSuffix)
    {
        this.hsuffix = headerSuffix;
    } // end of setHsuffix(String)

    /**
     * Places generated header files into this directory. This
     * directory is automatically added to the project compile
     * target root.
     * <p>
     * Available for c, c++, and objc only.
     * </p>
     * @param dir target header file directory.
     */
    public void setHeaderd(final File dir)
    {
        this.headerd = dir;
    } // end of setHeaderd(File)

    /**
     * Adds SMC debug output to generated code. There are two
     * levels:
     * <ul>
     *   <li>
     *     0: state entry, exit, and transitions.
     *   </li>
     *   <li>
     *     1: level 0 output plus state entry, exit actions.
     *   </li>
     * </ul>
     * This is turned off by default.
     * @param debugLevel SMC debug output level.
     */
    public void setDebugLevel(final int debugLevel)
    {
        this.debugLevel = debugLevel;
    } // end of setDebugLevel(int)

    /**
     * If {@code true}, do not use C++ iostreams for debugging.
     * Application code must provide a {@code TRACE} macro to
     * output debug messages. Default setting is {@code false}.
     * <p>
     * Available for c++ only.
     * </p>
     * @param nostreams flag to turn C++ iostreams output on or
     * off.
     */
    public void setNostreams(final boolean nostreams)
    {
        this.nostreams = nostreams;
    } // end of setNostreams(boolean)

    /**
     * If {@code true} user supplied class must be derives from
     * state machine. The default is {@code false}.
     * <p>
     * See CRTP ("curiously recurring template pattern").
     * </p>
     * <p>
     * Available for c++ only.
     * </p>
     * @param crtp turn CRTP code generation on or off.
     */
    public void setCrtp(final boolean crtp)
    {
        this.crtp = crtp;
    } // end of setCrtp(boolean)

    /**
     * If {@code true} generates thread-safe code. This is not
     * necessary if application guarantees FSM access is
     * thread-safe. Default setting is {@code false}.
     * <p>
     * Available for csharp, java, java7, groovy, scala, and vb
     * only because these languages provide thread
     * synchronization constructs in the language.
     * </p>
     * @param sync turns thread synchronization code generation
     * on or off.
     */
    public void setSync(final boolean sync)
    {
        this.sync = sync;
    } // end of setSync(boolean)

    /**
     * If {@code true}, do <em>not</em> generate C++ exception
     * throws. Default setting is {@code false}.
     * <p>
     * Available for c++ only.
     * </p>
     * @param noex turn C++ exception throw code generation on or
     * off.
     */
    public void setNoex(final boolean noex)
    {
        this.noex = noex;
    } // end of setNoex(boolean)

    /**
     * If {@code true}, do <em>not</em> generate
     * {@code try/catch/rethrow} code. The {@code try/catch}
     * is used to guarantee that the current state is set before
     * allowing the exception throw to continue. If current FSM
     * state is left {@code null}, then the FSM is broken and
     * ceases to function. Default setting is {@code false}.
     * <p>
     * Setting this property to {@code true} is
     * <em><strong>not</strong></em> recommended.
     * </p>
     * @param nocatch turns {@code try/catch} block code
     * generation on or off.
     */
    public void setNocatch(final boolean nocatch)
    {
        this.nocatch = nocatch;
    } // end of setNocatch(boolean)

    /**
     * If &gt; zero, state stack has a fixed size defined by this
     * setting. Otherwise, state stack size is unlimited. This
     * setting means that dynamic memory allocation is not used
     * when setting the stack. Default setting is an unlimited
     * stack size.
     * <p>
     * Available for c++ only.
     * </p>
     * @param stateStackSize state stack fixed size.
     */
    public void setStateStackSize(final int stateStackSize)
    {
        this.stateStackSize = stateStackSize;
    } // end of setStateStackSize(int)

    /**
     * If {@code true}, generate unique integer identifiers for
     * each state. These identifiers are used to efficiently
     * serialize the current state stack. Default setting is
     * {@code false}.
     * @param serial turns state identifier generation on or off.
     */
    public void setSerial(final boolean serial)
    {
        this.serial = serial;
    } // end of setSerial(boolean)

    /**
     * If {@code true}, generate {@code getTransitions()} method
     * for each state, allowing the state to be interrogated
     * about its supported transition. Default setting is
     * {@code false}.
     * <p>
     * Available for csharp, groovy, java, java7, js, lua, perl,
     * php, python, ruby, scala, tcl, and vb only.
     * </p>
     * @param reflection turns state machine reflection on or
     * off.
     */
    public void setReflection(final boolean reflection)
    {
        this.reflection = reflection;
    } // end of setReflection(boolean)

    /**
     * If {@code true}, use a
     * <code>Map&lt;String, Integer&gt;</code> for reflection
     * map. Default setting is {@code false}.
     * <p>
     * Available for csharp, java, or vb only when
     * {@code reflect} property is set to {@code true}.
     * </p>
     * @param generic turn generic reflection code generation on
     * or off.
     */
    public void setGeneric(final boolean generic)
    {
        this.generic = generic;
    } // end of setGeneric(boolean)

    /**
     * Use Java 7 generic collections for reflection map.
     * <p>
     * Available for java, or java7 only when
     * {@code reflect} property is set to {@code true}.
     * </p>
     * @param generic7 turns Java 7 generic collections on or
     * off.
     */
    public void setGeneric7(final boolean generic7)
    {
        this.generic7 = generic7;
    } // end of setGeneric7(boolean)

    /**
     * If {@code true}, {@link #execute()} outputs more detailed
     * messages. Default setting is {@code false}.
     * @param verbose turn verbose execution output on or off.
     */
    public void setVerbose(final boolean verbose)
    {
        this.verbose = verbose;
    } // end of setVerbose(boolean)

    /**
     * If {@code true}, {@link #execute()} outputs even more
     * detailed messages. Default setting is {@code false}.
     * @param vverbose turn very verbose execution output on or
     * off.
     */
    public void setVverbose(final boolean vverbose)
    {
        this.vverbose = vverbose;
    } // end of setVverbose(boolean)

    /**
     * Specifies the Graphviz output detail level. Ranges from
     * 0 (least detail) to 2 (most detail). See SMC Programmer's
     * Manual, section 10 for an explanation on how to generate
     * Graphviz-based FSM diagram. The default setting is 0 -
     * least detail.
     * <p>
     * Available for graph only.
     * </p>
     * @param glevel Graphviz detail level.
     */
    public void setGraphLevel(final int glevel)
    {
        this.glevel = glevel;
    } // end of setGraphLevel(int)

    /**
     * When generating C++ code, use this cast type
     * ({@code dynamic_cast}, {@code static_cast}, or
     * {@code reinterpret_cast}). Default setting is
     * {@code dynamic_cast}.
     * <p>
     * Available for c++ only.
     * </p>
     * @param cast generated C++ cast code.
     */
    public void setCast(final String cast)
    {
        this.cast = cast;
    } // end of setCast(String)

    /**
     * Access identifier for generated classes. Default setting
     * is an empty string.
     * <p>
     * Available for java, java7 only.
     * </p>
     * @param access Java access level.
     */
    public void setAccess(final String access)
    {
        this.access = access;
    } // end of setAccess(String)

    /**
     * User-defined FSM context class extends {@code @protocol}.
     * Generated code references context class via protocol.
     * Default setting is {@code false}.
     * <p>
     * Available for objc only.
     * </p>
     * @param protocol turns Objective-C {@code @protocol}
     * support on or off.
     */
    public void setProtocol(final boolean protocol)
    {
        this.protocol = protocol;
    } // end of setProtocol(boolean)

    /**
     * Maven uses this method to set the target project. Default
     * setting is {@code ${project}}.
     * @param project mojo is working for this maven project.
     */
    public void setProject(final MavenProject project)
    {
        this.project = project;
    } // end of setProject(MavenProject)

    //
    // end of Set Methods.
    //-----------------------------------------------------------

    /**
     * Outputs the parameter configuration as a debug log.
     */
    private void outputSettings()
    {
        final StringBuilder output = new StringBuilder(2_048);

        output.append("SmcMojo configuration:")
              .append("\n targetLanguage=").append(targetLanguage)
              .append("\nsourceDirectory=").append(sourceDirectory)
              .append("\ntargetDirectory=").append(targetDirectory)
              .append("\n        sources=").append(Arrays.toString(sources))
              .append("\n         suffix=").append(suffix)
              .append("\n        hsuffix=").append(hsuffix)
              .append("\n        headerd=").append(headerd)
              .append("\n     debugLevel=").append(debugLevel)
              .append("\n      nostreams=").append(nostreams)
              .append("\n           crtp=").append(crtp)
              .append("\n           sync=").append(sync)
              .append("\n           noex=").append(noex)
              .append("\n        nocatch=").append(nocatch)
              .append("\n stateStackSize=").append(stateStackSize)
              .append("\n         serial=").append(serial)
              .append("\n     reflection=").append(reflection)
              .append("\n        generic=").append(generic)
              .append("\n       generic7=").append(generic7)
              .append("\n        verbose=").append(verbose)
              .append("\n       vverbose=").append(vverbose)
              .append("\n     graphLevel=").append(glevel)
              .append("\n           cast=").append(cast)
              .append("\n         access=").append(access)
              .append("\n       protocol=").append(protocol);

        getLog().debug(output.toString());

        return;
    } // end of outputSettings()

    //
    // Plugin configuration validation methods.
    //

    /**
     * Validates the MOJO configuration prior to executing the
     * state machine compiler. This method is called for effect
     * only. This means the method returns if the configuration
     * is valid; otherwise it throws
     * {@code MojoExecutionException} which contains a message
     * explaining the incorrect configuration.
     * @throws MojoExecutionException
     * if an invalid MOJO configuration is detected.
     */
    private void validateSettings()
        throws MojoExecutionException
    {
        mTargetLanguage = Smc.findTargetLanguage(targetLanguage);
        if (mTargetLanguage == null)
        {
            throw (
                new MojoExecutionException(
                    "SMC does not support " +
                    targetLanguage +
                    " target language"));
        }

        // The target language record is used to validate the
        // remaining configurations because configuration is
        // target language dependent.

        // sourceDirectory must reference an existing, readable
        // directory.
        isValidDirectory(sourceDirectory, false, true, false);

        // outputDirectory must reference an existing, writable
        // directory.
        isValidDirectory(targetDirectory, true, false, true);

        // Is sources empty?
        if (sources.length == 0)
        {
            // Yes. Fill sources with all known .sm files.
            sources =
                sourceDirectory.list(
                    new FilenameFilter()
                    {
                        @Override
                        public boolean accept(final File dir,
                                              final String name)
                        {
                            return (name.endsWith(SM_SUFFIX));
                        }
                    });
        }

        // Now verify sources contains valid SMC target files.
        final int numSources = sources.length;

        for (int i = 0; i < numSources; ++i)
        {
            isValidSource(sources[i]);
        }

        // If suffix or header suffix is an empty string, then
        // reset to null because that is what SMC expects.
        suffix = (suffix != null && suffix.isEmpty() ?
                  null :
                  suffix);
        hsuffix = (hsuffix != null &&
                        hsuffix.isEmpty() ?
                        null :
                        hsuffix);

        // Does the target language support header files?
        if (hsuffix != null &&
            !Smc.supportsOption(Smc.HEADER_SUFFIX_FLAG, mTargetLanguage))
        {
            // Strip the leading "-" from the property name.
            throw (
                new MojoExecutionException(
                    targetLanguage +
                    " does not support " +
                    (Smc.HEADER_SUFFIX_FLAG).substring(1) +
                    " property"));
        }

        // Was a target header directory provided?
        // Is it valid?
        if (headerd != null)
        {
            if (!Smc.supportsOption(Smc.HEADER_FLAG, mTargetLanguage))
            {
                // Strip the leading "-" from the property name.
                throw (
                    new MojoExecutionException(
                        targetLanguage +
                        " does not support " +
                        (Smc.HEADER_FLAG).substring(1) +
                        " property"));
            }

            isValidDirectory(headerd, true, false, true);
        }

        // Sets debugLevel to either minimum or maximum allowed
        // value if user setting is out of range.
        debugLevel = isValidDebugLevel(debugLevel);

        isValidProperty(Smc.NO_STREAMS_FLAG, nostreams);
        isValidProperty(Smc.CRTP_FLAG, crtp);
        isValidProperty(Smc.SYNC_FLAG, sync);
        isValidProperty(Smc.NO_EXCEPTIONS_FLAG, noex);
        isValidProperty(Smc.NO_CATCH_FLAG, nocatch);
        isValidStackSize();
        isValidProperty(Smc.SERIAL_FLAG, serial);
        isValidProperty(Smc.REFLECT_FLAG, reflection);
        isValidProperty(Smc.GENERIC_FLAG, generic);
        isValidProperty(Smc.GENERIC7_FLAG, generic7);
        isValidCast();
        isValidAccessLevel();
        isValidGraphLevel();
        isValidProperty(Smc.USE_PROTOCOL_FLAG, protocol);

        return;
    } // end of validateSettings()

    /**
     * Validates that the direct exists and is either readable or
     * writable.
     * @param path file path instance.
     * @param create if {@code path} does not exist and this flag
     * is {@code true}, then create the directory.
     * @param readable expected {@link File#canRead()} result.
     * @param writable expected {@link File#canWrite()} result.
     * @throws MojoExecutionException
     * if {@code path} does not meet the expected results.
     */
    private void isValidDirectory(final File path,
                                  final boolean create,
                                  final boolean readable,
                                  final boolean writable)
        throws MojoExecutionException
    {
        try
        {
            // If the desired directory does not exist, then
            // create it. Still need to make sure the directory
            // is configured properly.
            if (create &&
                !path.exists() &&
                !path.mkdirs())
            {
                throw (
                    new MojoExecutionException(
                        "failed to create " + path));
            }

            if (!path.exists())
            {
                throw
                    (new MojoExecutionException(
                        "\"" + path + "\" does not exist"));
            }

            if (path.isDirectory() == false)
            {
                throw (
                    new MojoExecutionException(
                        "\"" + path + "\" is not a directory"));
            }

            if (readable && !path.canRead())
            {
                throw (
                    new MojoExecutionException(
                        "\"" + path + "\" is not readable"));
            }

            if (writable && !path.canWrite())
            {
                throw (
                    new MojoExecutionException(
                        "\"" + path + "\" is not writable"));
            }
        }
        catch (SecurityException securex)
        {
            throw (
                new MojoExecutionException(
                    "unable to access \"" + path + "\"",
                    securex));
        }

        return;
    } // end of isValidDirectory(String, boolean, boolean)

    private void isValidSource(final String srcName)
        throws MojoExecutionException
    {
        final File srcFile = new File(sourceDirectory, srcName);

        if (!srcName.endsWith(SM_SUFFIX))
        {
            throw (
                new MojoExecutionException(
                    "\"" + srcName + "\" suffix is not \"" + SM_SUFFIX + "\""));
        }

        if (!srcFile.exists())
        {
            throw (
                new MojoExecutionException(
                    "\"" + srcFile + "\" does not exist"));
        }

        if (!srcFile.canRead())
        {
            throw (
                new MojoExecutionException(
                    "\"" + srcFile + "\" is not readable"));
        }

        return;
    } // end of isValidSource(String)

    private int isValidDebugLevel(final int level)
    {
        int retval = level;

        if (level < SmcCodeGenerator.NO_DEBUG_OUTPUT)
        {
            retval = SmcCodeGenerator.NO_DEBUG_OUTPUT;
        }
        else if (level > SmcCodeGenerator.DEBUG_LEVEL_1)
        {
            if (getLog().isWarnEnabled())
            {
                getLog().warn(
                    "debugLevel " +
                    level +
                    " > max allowed; resetting to maximum.");
            }

            retval = SmcCodeGenerator.DEBUG_LEVEL_1;
        }

        return (retval);
    } // end of isValidDebugLevel(int)

    /**
     * Checks if the access level is set to a valid access level
     * for the target language. Does nothing if access level is
     * not set.
     * @throws MojoExecutionException
     * if target language does not support the specified access
     * level or access level is not valid for the target
     * language.
     */
    private void isValidAccessLevel()
        throws MojoExecutionException
    {
        if (access == null || access.isEmpty())
        {
            // An un-set access is valid.
        }
        else if (!Smc.supportsOption(Smc.ACCESS_FLAG, mTargetLanguage))
        {
            throw (
                new MojoExecutionException(
                    targetLanguage +
                    " does not support access property"));
        }
        else if (!Smc.isValidAccessLevel(access, mTargetLanguage))
        {
            throw (
                new MojoExecutionException(
                    targetLanguage +
                    " does not support " +
                    access +
                    " level"));
        }

        return;
    } // end of isValidAccessLevel()

    /**
     * Checks if cast type is configured, the target programming
     * language supports this property, and cast type is valid.
     * Does nothing if cast type is not set.
     * @throws MojoExecutionException
     * if cast property is not configured correctly.
     */
    private void isValidCast()
        throws MojoExecutionException
    {
        if (cast == null || cast.isEmpty())
        {
            // An un-set cast is valid.
        }
        else if (!Smc.supportsOption(Smc.CAST_FLAG, mTargetLanguage))
        {
            throw (
                new MojoExecutionException(
                    targetLanguage +
                    " does not support cast property"));
        }
        else if (!Smc.isValidCast(cast))
        {
            throw (
                new MojoExecutionException(
                    targetLanguage +
                    " does not support " +
                    cast));
        }

        return;
    } // end of isValidCast()

    private void isValidStackSize()
        throws MojoExecutionException
    {
        // Ignore if target language is not C++.
        if (!targetLanguage.equals(
                TargetLanguage.C_PLUS_PLUS.suffix()))
        {
            // ignore.
        }
        // Is stack size valid?
        if (stateStackSize < 0)
        {
            throw (
                new MojoExecutionException(
                    "stack must >= 0"));
        }

        return;
    }

    /**
     * Checks if target language is graph and that the graph
     * level setting is valid (0, 1, or 2).
     * @throws MojoExecutionException
     * if graph level property is not configured correctly.
     */
    private void isValidGraphLevel()
        throws MojoExecutionException
    {
        // Ignore if target language is not graph.
        if (!targetLanguage.equals(
                TargetLanguage.GRAPH.suffix()))
        {
            // ignore.
        }
        else if (!Smc.isValidGraphLevel(glevel))
        {
            throw (
                new MojoExecutionException(
                    "glevel must be 0, 1, or 2"));
        }

        return;
    } // end of isValidGraphLevel()

    /**
     * Fails MOJO if {@code property} is {@code true} and is not
     * supported by the specified target language.
     * @param property check if target language supports this SMC
     * property.
     * @param flag check only if {@code true}.
     * @throws MojoExecutionException
     * if target language does not support {@code property}.
     */
    private void isValidProperty(final String property,
                                 final boolean flag)
        throws MojoExecutionException
    {
        if (flag &&
            !Smc.supportsOption(property, mTargetLanguage))
        {
            // Strip the leading "-" from the property name.
            throw (
                new MojoExecutionException(
                    targetLanguage +
                    " does not support " +
                    property.substring(1) +
                    " property"));
        }

        return;
    } // end of isValidProperty(String, boolean)

    //
    // SMC parse and emit methods.
    //

    /**
     * Compiles each of the FSM target files in turn.
     */
    private void compileAll()
        throws MojoExecutionException
    {
        final int numSources = sources.length;
        int i;
        boolean failureFlag = false;

        for (i = 0; i < numSources; ++i)
        {
            try
            {
                compile(sources[i]);
            }
            catch (MojoFailureException mojoex)
            {
                failureFlag = true;

                getLog().error(mojoex.getLocalizedMessage());

                if (mojoex.getCause() != null)
                {
                    getLog().error(mojoex.getCause());
                }
            }
        }

        if (failureFlag)
        {
            throw (
                new MojoExecutionException(
                    "SMC compile failures"));
        }

        return;
    } // end of compileAll()

    /**
     * Parses FSM target file, performs syntax check, and emits
 FSM in configured target language.
     * @param source FSM target file.
     * @throws MojoFailureException
     * if the FSM compile failed.
     */
    private void compile(final String source)
        throws MojoFailureException
    {
        // 1. Parse the target file.
        final SmcFSM fsm = parse(source, Smc.getFileName(source));

        // 2. Check the parsed state machine.
        syntaxCheck(source, fsm);

        // 3. Emit the state machine in the target langauge.
        emit(source, fsm);

        return;
    } // end of compile(String)

    /**
     * Returns the FSM model parsed from the given target file.
     * @param source parse this target file.
     * @param baseName target file base name.
     * @return FSM model.
     * @throws MojoFailureException
     * if the parse failed for any reason.
     */
    private SmcFSM parse(final String source,
                         final String baseName)
        throws MojoFailureException
    {
        final Instant startTime = Instant.now();
        final File sourceFile =
            new File(sourceDirectory, source);
        SmcParser parser = null;
        final SmcFSM retval;

        if (verbose)
        {
            getLog().info(
                "[parsing started " + source + "]");
        }

        try
        {
            parser =
                new SmcParser(baseName,
                              new FileInputStream(sourceFile),
                              mTargetLanguage.language(),
                              vverbose);

            retval = parser.parse();
        }
        catch (IOException |
               IllegalAccessException |
               InvocationTargetException jex)
        {
            final String message;

            // If there is no parser or parser
            if (parser == null ||
                (parser.getMessages()).isEmpty())
            {
                message = jex.getLocalizedMessage();
            }
            else
            {
                message =
                    outputMessages(source, parser.getMessages());
            }

            throw (new MojoFailureException(message, jex));
        }

        if (verbose)
        {
            final Instant finishTime = Instant.now();
            final Duration delta =
                Duration.between(startTime, finishTime);

            getLog().info(
                String.format(
                    "[parsing completed %s %d.%03d ms]",
                    source,
                    delta.getSeconds(),
                    (delta.getNano() / NANOS_PER_MILLI)));
        }

        // Did the parse fail?
        if (retval == null)
        {
            // Yes. Output the messages and throw a MOJO failure.
            for (SmcMessage message : parser.getMessages())
            {
                System.err.println(message);
            }

            throw (new MojoFailureException("SMC parse failed"));
        }

        return (retval);
    } // end of parse(String, String)

    /**
     * Performs additional syntax checks on the FSM model.
     * @param source FSM target file.
     * @param fsm parsed FSM target.
     * @throws MojoFailureException
     * if state machine fails additional syntax check.
     */
    private void syntaxCheck(final String source,
                             final SmcFSM fsm)
        throws MojoFailureException
    {
        final SmcSyntaxChecker checker =
            new SmcSyntaxChecker(
                source, mTargetLanguage.language());
        final Instant startTime = Instant.now();

        if (verbose)
        {
            getLog().info("[checking started " + source + "]");
        }

        fsm.accept(checker);

        // Did the FSM pass the checks?
        if (!checker.isValid())
        {
            // No. Bad mojo.
            throw (
                new MojoFailureException(
                    outputMessages(
                        source, checker.getMessages())));
        }

        if (verbose)
        {
            final Instant finishTime = Instant.now();
            final Duration delta =
                Duration.between(startTime, finishTime);

            getLog().info(
                String.format(
                    "[checking completed %s %d.%03d ms]",
                    source,
                    delta.getSeconds(),
                    (delta.getNano() / NANOS_PER_MILLI)));
        }

        return;
    } // end of syntaxCheck(String, SmcFSM)

    /**
     * Emits the target generated source file and, if needed,
     * header file.
     * @param source source file name.
     * @param fsm parsed finite state machine model.
     * @throws MojoFailureException
     * if target code emit failed.
     */
    private void emit(final String source,
                      final SmcFSM fsm)
        throws MojoFailureException
    {
        final Instant startTime = Instant.now();
        final String baseName = fsm.getFsmClassName();
        final SmcOptions options = setOptions(source, fsm);
        SmcCodeGenerator emitter = null;

        if (verbose)
        {
            getLog().info("[emitting started " + source + "]");
        }

        try
        {
            // Generate the header file first - if the target
            // language uses header files.
            if (mTargetLanguage.hasHeaderFile())
            {
                emitter = headerEmitter(fsm, options);

                try (PrintStream hstream = emitter.target())
                {
                    fsm.accept(emitter);

                    if (verbose)
                    {
                        getLog().info(
                            String.format(
                                "[wrote %s]",
                                emitter.targetFile()));
                    }
                }
            }

            emitter = sourceEmitter(fsm, options);

            try (PrintStream srcStream = emitter.target())
            {
                // Generate the target target file.
                fsm.accept(emitter);
            }

            if (verbose)
            {
                getLog().info(
                    String.format(
                        "[wrote %s]", emitter.targetFile()));
            }
        }
        catch (IOException ioex)
        {
            throw (
                new MojoFailureException(
                    "error emitting " +
                    (emitter == null ?
                     "(not set)" :
                     emitter.targetFile()),
                    ioex));
        }

        if (verbose)
        {
            final Instant finishTime = Instant.now();
            final Duration delta =
                Duration.between(startTime, finishTime);

            getLog().info(
                String.format(
                    "[emitting completed %s %d.%03d ms]",
                    source,
                    delta.getSeconds(),
                    (delta.getNano() / NANOS_PER_MILLI)));
        }

        return;
    } // end of emit(String, SmcFSM)

    /**
     * Returns the SMC emitter options as per the plug-in
     * configuration.
     * @param source emitting target code for this FSM target
 file.
     * @param baseName target file base name.
     * @return SMC emitter options.
     */
    private SmcOptions setOptions(final String source,
                                  final SmcFSM fsm)
    {
        final boolean java7Flag =
            (mTargetLanguage.language() == TargetLanguage.JAVA7);

        // If the target language supports headers, check if
        // the header suffix and target directory are set. If
        // not, then set to defaults.
        if (mTargetLanguage.hasHeaderFile())
        {
            if (hsuffix == null)
            {
                hsuffix = SmcCodeGenerator.DEFAULT_HEADER_SUFFIX;
            }

            // If no target header directory is specified, then
            // place the headers in the same target directory as
            // the generate sources.
            if (headerd == null)
            {
                headerd = targetDirectory;
            }
        }

        // If the access level is "package", then change this to
        // "/* package */".
        if (access == null || access.isEmpty())
        {
            access = "public";
        }
        else if (access.equalsIgnoreCase(Smc.PACKAGE_LEVEL))
        {
            access = Smc.PACKAGE_ACCESS;
        }

        return (new SmcOptions(Smc.APP_NAME,
                               Smc.VERSION,
                               source,
                               fsm.getTargetFileName(),
                               sourceDirectory.getPath(),
                               (headerd == null ?
                                "" :
                                headerd.getPath()),
                               hsuffix,
                               cast,
                               glevel,
                               serial,
                               debugLevel,
                               noex,
                               nocatch,
                               nostreams,
                               crtp,
                               stateStackSize,
                               reflection,
                               sync,
                               generic,
                               java7Flag,
                               access,
                               protocol));
    } // end of setOptions()

    /**
     * Returns target code emitter configured as per
     * {@code options}.
     * @param fsm finite state machine model.
     * @param options emitter options.
     * @return target code emitter.
     * @throws IOException
     * if input target file failed to open.
     */
    private SmcCodeGenerator sourceEmitter(final SmcFSM fsm,
                                           final SmcOptions options)
        throws IOException
    {
        final String targetFilePath =
            targetPath(targetDirectory, fsm);
        final String targetFileBase = fsm.getTargetFileName();
        final SmcCodeGenerator retval =
            mTargetLanguage.generator(options);

        // Make sure the target directory exists.
        (new File(targetFilePath)).mkdirs();

        retval.setTarget(
            new PrintStream(
                new FileOutputStream(
                    retval.setTargetFile(
                        targetFilePath, targetFileBase, suffix))));

        return (retval);
    } // end of sourceEmitter(SmcFSM, SmcOptions)

    /**
     * Returns header file emitter configured as per
     * {@code options}.
     * @param fsm finite state machine model.
     * @param options emitter options.
     * @return header file emitter.
     * @throws IOException
     * if input target file failed to open.
     */
    private SmcCodeGenerator headerEmitter(final SmcFSM fsm,
                                           final SmcOptions options)
        throws IOException
    {
        final String targetFilePath = targetPath(headerd, fsm);
        final String targetFileBase = fsm.getTargetFileName();
        final SmcCodeGenerator retval =
            mTargetLanguage.headerGenerator(options);

        retval.setTarget(
            new PrintStream(
                new FileOutputStream(
                    retval.setTargetFile(
                        targetFilePath, targetFileBase, hsuffix))));

        return (retval);
    } // end of headerEmitter(String, SmcOptions)

    private String targetPath(final File targetDir,
                              final SmcFSM fsm)
    {
        String pkgName = fsm.getPackage();
        final StringBuilder retval =
            new StringBuilder(targetDir.getPath());

        // Was a package name provided?
        if (pkgName == null)
        {
            // No. Use an empty string.
            pkgName = "";
        }

        // Replace the package name periods with OS-specific file
        // name separator and then append the result to the
        // top-level target directory. Finish up with a final
        // separator at the end.
        retval.append(File.separatorChar)
              .append(pkgName.replace('.', File.separatorChar))
              .append(File.separatorChar);

        return (retval.toString());
    } // end of targetPath(SmcFSM)

    /**
     * Returns SMC output messages as a single text block.
     * @param srcFileName SMC target file name.
     * @param messages output these messages.
     * @return SMC output messages.
     */
    private String outputMessages(final String source,
                                  final List<SmcMessage> messages)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final Iterator<SmcMessage> mIt = messages.iterator();
        SmcMessage msg;

        while (mIt.hasNext())
        {
            msg = mIt.next();
            pw.format("%s:%d: %s - %s%n",
                      source,
                      msg.getLineNumber(),
                      (msg.getLevel() == SmcMessage.WARNING ?
                           "warning" :
                           "error"),
                      msg.getText());
        }

        return (sw.toString());
    } // end of outputMessages(String, PrintWriter, List<>)
} // end of class SmcMojo
