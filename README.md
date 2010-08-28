LAZYTEST IS ALPHA AND SUBJECT TO CHANGE
=======================================

Lazytest: behavior-driven development/testing framework for Clojure

by Stuart Sierra, http://stuartsierra.com/

Copyright (c) Stuart Sierra, 2010. All rights reserved.  The use and
distribution terms for this software are covered by the Eclipse Public
License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file LICENSE.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.  You must not remove this notice, or any
other, from this software.


Test Examples and Groups
========================

Use the `describe` macro to create a group of tests.  Start the group
with a documentation string.

    (use '[lazytest.describe :only (describe it)])

    (describe "This application" ...)

If you put a symbol before (or instead of) the string, the full name
of the Var or Class to which that symbol resolves will be prepended to
the doc string:

    (describe + "with integers" ...)
    ;; resulting doc string is "#'clojure.core/+ with integers"

Within a `describe` group, use the `it` macro to create a single test
example.  Start your example with a documentation string describing
what should happen, followed by an expression to test what you think
should be true.

    (describe + "with integers"
      (it "computes the sum of 1 and 2"
        (= 3 (+ 1 2)))
      (it "computes the sum of 3 and 4"
        (= 7 (+ 3 4))))

Each `it` example may only contain *one* expression, which must return
logical true to indicate the test passed or logical false to indicate
it failed.



Arbitrary Code in an Example
============================

You can create an example that executes arbitrary code with the
`do-it` macro.  Wrap each assertion expression in the `expect` macro.

    (use '[lazytest.describe :only (describe it do-it)]
         '[lazytest.expect :only (expect)])

    (describe "Arithmetic"
      (do-it "after printing"
        (println "Hello, World!")
        (expect (= 4 (+ 2 2)))))



Getting Started with Leiningen
==============================

These instructions require JDK 6.

Put the following in `project.clj`

    (defproject your-project-name "1.0.0-SNAPSHOT"
      :description "Your project description"
      :dependencies [[org.clojure/clojure "1.2.0"]
                     [org.clojure/clojure-contrib "1.2.0"]
                     [com.stuartsierra/lazytest "1.0.0-SNAPSHOT"]]
      :repositories {"stuartsierra.com" "http://stuartsierra.com/m2snapshots"})

Put your test sources in `test/`

Then run:

    lein clean
    lein deps
    java -cp "src:test:classes:lib/*" lazytest.watch src test

And watch your tests run automatically whenever you save a file.

Type CTRL+C to stop.



Getting Started with Maven
==========================

Put the following in your `pom.xml` file's `<dependencies>` section:

    <dependency>
      <groupId>com.stuartsierra</groupId>
      <artifactId>lazytest</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>

And the following in the `pom.xml` file's `<repositories>` section:

    <repository>
      <id>stuartsierra-snapshots</id>
      <url>http://stuartsierra.com/m2snapshots</url>
      <releases>
	<enabled>false</enabled>
      </releases>
      <snapshots>
	<enabled>true</enabled>
      </snapshots>
    </repository>

Put your test sources in `src/test/clojure/`

Then run:

    mvn clojure:repl

And type:

    (use 'lazytest.watch)
    (start ["src"])

And watch your tests run automatically whenever you save a file.

Type CTRL+C to stop.



Lazytest Internals
==================

The smallest unit of testing is a *test case*, which is a function
(see `lazytest.test-case/test-case`).  When the function is called, it
may throw an exception to indicate failure.  If it does not throw an
exception, it is assumed to have passed.  The return value of a test
case is always ignored.  Running a test case may have side effects.

Tests cases are organized into *suites*.  A test suite is a function
(see `lazytest.suite/suite`) that returns a *test sequence*.  A test
sequence (see `lazytest.suite/test-seq`) is a sequence, possibly lazy,
of test cases and/or suites.  Suites, therefore, may be nested inside
other suites, but nothing may be nested inside a test case.

A test suite function may NOT have side effects; it is only used to
generate test cases and/or other test suites.

A test *runnner* is responsible for expanding suites (see
`lazytest.suite/expand-suite`) and running test cases (see
`lazytest.test-case/try-test-case`).  It may also provide feedback on
the success of tests as they run.  Two built-in runners are provided,
see `lazytest.runner.console/run-tests` and
`lazytest.runner.debug/run-tests`.

The test runner also returns a sequence of *results*, which are either
*suite results* (see `lazytest.suite/suite-result`) or *test case
results* (see `lazytest.test-case/test-case-result`).  That sequence
of results is passed to a *reporter*, which formats results for
display to the user.  One example reporter is provided, see
`lazytest.report.nested/report`.



Making Emacs Indent Tests Properly
==================================

Put the following in `.emacs`

    (eval-after-load 'clojure-mode
      '(define-clojure-indent
         (describe 'defun)
         (it 'defun)))



Known Defects
=============

* Changing an applicaton source file does not automatically reload
  the associated test source file.
