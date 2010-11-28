package com.stuartsierra.lazytest;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal which runs the Lazytest watcher: runs tests when source files changes
 *
 * @goal watch
 * 
 * @phase test
 * @requiresDependencyResolution test
 */
public class LazytestWatchMojo extends AbstractLazytestMojo {
    public void execute() throws MojoExecutionException {
	runLazytest("lazytest.watch");
    }
}
