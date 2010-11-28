package com.stuartsierra.lazytest;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal which runs Lazytest once
 *
 * @goal run
 * 
 * @phase test
 * @requiresDependencyResolution test
 */
public class LazytestSingleRunMojo extends AbstractLazytestMojo {
    public void execute() throws MojoExecutionException {
	runLazytest("lazytest.main");
    }
}
