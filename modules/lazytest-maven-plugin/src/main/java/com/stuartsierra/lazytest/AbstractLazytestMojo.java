package com.stuartsierra.lazytest;

import org.apache.commons.exec.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLazytestMojo extends AbstractMojo {
    /**
     * The current toolchain maanager instance
     *
     * @component
     */
    private ToolchainManager toolchainManager;

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * Base directory of the project.
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File baseDirectory;

    /**
     * Project classpath.
     *
     * @parameter default-value="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> classpathElements;

    /**
     * Project test classpath.
     *
     * @parameter default-value="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> testClasspathElements;

    /**
     * Directory for test output files.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     */
    protected File testOutputDirectory;

    /**
     * Location of main source files.
     *
     * @parameter
     */
    protected String[] sourceDirectories = new String[]{"src/main/clojure"};

    /**
     * Location of test source files.
     *
     * @parameter
     */
    protected String[] testSourceDirectories = new String[]{"src/test/clojure"};

    /**
     * Default location of test source files.
     *
     * @parameter default-value="${project.build.testSourceDirectory}"
     * @required
     */
    protected File baseTestSourceDirectory;

    /**
     * Location of generated source files.
     *
     * @parameter default-value="${project.build.outputDirectory}/../generated-sources"
     * @required
     */
    protected File generatedSourceDirectory;

    private String getJavaExecutable() throws MojoExecutionException {
        Toolchain tc = toolchainManager.getToolchainFromBuildContext("jdk", //NOI18N
                session);

        if (tc != null) {
            getLog().info("Toolchain used in lazytest-maven-plugin: " + tc);
            String foundExecutable = tc.findTool("java");
            if (foundExecutable != null) {
                return foundExecutable;
            } else {
                throw new MojoExecutionException("Failed to find 'java' executable for toolchain: " + tc);
            }
        }

        return "java";
    }

    protected Set<String> getClasspathElements() {
	Set<String> cp = new HashSet<String>();
	cp.addAll(classpathElements);
	cp.addAll(testClasspathElements);
	for (int i = 0; i < sourceDirectories.length; i++)
	    cp.add(sourceDirectories[i]);
	for (int i = 0; i < testSourceDirectories.length; i++)
	    cp.add(testSourceDirectories[i]);
	cp.add(baseTestSourceDirectory.getAbsolutePath());
	cp.add(generatedSourceDirectory.getAbsolutePath());
	return cp;
    }

    protected String getClasspath() {
	List<String> elements = new ArrayList<String>(getClasspathElements());
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < elements.size(); i++) {
	    sb.append(elements.get(i));
	    sb.append(":");
	}
	return sb.toString();
    }

    protected List<String> getJavaCommandLineArgs() {
	List<String> args = new ArrayList<String>();
	args.add("-cp");
	args.add(getClasspath());
	return args;
    }

    protected List<String> getSourceDirectories() {
	Set<String> dirs = new HashSet<String>();
	for (int i = 0; i < sourceDirectories.length; i++)
	    dirs.add(sourceDirectories[i]);
	for (int i = 0; i < testSourceDirectories.length; i++)
	    dirs.add(testSourceDirectories[i]);
	dirs.add(baseTestSourceDirectory.getAbsolutePath());
	dirs.add(generatedSourceDirectory.getAbsolutePath());

	return new ArrayList<String>(dirs);
    }

    public void runLazytest(String mainClass) throws MojoExecutionException {
	String javaExecutable = getJavaExecutable();
	List<String> args = getJavaCommandLineArgs();
	List<String> dirs = getSourceDirectories();

	getLog().debug("Java executable: " + javaExecutable);
	getLog().debug("Command-line arguments: " + args.toString());
	getLog().debug("Main class: " + mainClass);
	getLog().debug("Source directories: " + dirs.toString());

	CommandLine cl = new CommandLine(javaExecutable);
	for (int i = 0; i < args.size(); i++)
	    cl.addArgument(args.get(i));

	cl.addArgument(mainClass);

	for (int i = 0; i < dirs.size(); i++)
	    cl.addArgument(dirs.get(i));
	
        Executor exec = new DefaultExecutor();
        Map<String, String> env = new HashMap<String, String>(System.getenv());

        ExecuteStreamHandler handler = new PumpStreamHandler(System.out, System.err, System.in);
        exec.setStreamHandler(handler);
        exec.setWorkingDirectory(session.getCurrentProject().getBasedir());

        int status;
        try {
            status = exec.execute(cl, env);
        } catch (ExecuteException e) {
            status = e.getExitValue();
        } catch (IOException e) {
            status = 1;
        }

        if (status != 0) {
            throw new MojoExecutionException("Lazytest failed.");
        }
    }
}
