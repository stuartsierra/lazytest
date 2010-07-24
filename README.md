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


Expectations
============

Use the `expect` macro to make statements about things that should be
true.  For example:

    (use '[lazytest.expect :only (expect thrown?)])

    (expect (= 3 (+ 1 2)))
    (expect (thrown? ArithmeticException (/ 5 0)))

If the expression inside the `expect` macro returns logical true, the
expectation passes.  If it returns false or nil, the expectation
throws an exception.


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
what should happen, followed by code implementing your expectations.

    (describe + "with integers"
      (it "computes the sum"
        (expect (= 3 (+ 1 2))
	        (= 7 (+ 3 4)))))

    ;; The resulting doc string for this example is
    ;; "#'clojure.core/+ with integers computers the sum"


Givens
======

Use the `given` macro to share a computed value among several
examples.  `given` takes a vector of symbol/value pairs.  The syntax
is similar to `let`, including destructuring support.

The `given` macro can be used in place of `describe`, and takes an
optional documentation string.

    (use '[lazytest.describe :only (given it)])

    (given "The square root of 2" [s (Math/sqrt 2)]
      (it "is less than 2"
        (expect (< s 2)))
      (it "is greater than 1"
        (expect (> s 1))))


Nested Groups
=============

The `describe` and `given` macros may be nested to any depth, but
neither may appear inside the `it` macro.
        

Getting Started with Leiningen
==============================

These instructions require JDK 6.

Put the following in `project.clj`

    (defproject your-project-name "1.0.0-SNAPSHOT"
      :description "Your project description"
      :dependencies [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                     [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
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

Put the following in `pom.xml`

    <dependencies>
      <dependency>
        <groupId>org.clojure</groupId>
        <artifactId>clojure</artifactId>
        <version>1.2.0-master-SNAPSHOT</version>
      </dependency>
      <dependency>
        <groupId>org.clojure</groupId>
        <artifactId>clojure-contrib</artifactId>
        <version>1.2.0-SNAPSHOT</version>
      </dependency>
      <dependency>
        <groupId>com.stuartsierra</groupId>
        <artifactId>lazytest</artifactId>
        <version>1.0.0-SNAPSHOT</version>
      </dependency>
    </dependencies>
    <repositories>
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
      <repository>
        <id>stuartsierra-releases</id>
        <url>http://clojure.org/maven2</url>
        <releases>
          <enabled>true</enabled>
        </releases>
        <snapshots>
          <enabled>false</enabled>
        </snapshots>
      </repository>
      <repository>
        <id>clojars</id>
        <url>http://clojars.org/repo</url>
      </repository>
      <repository>
        <id>clojure-snapshots</id>
        <url>http://build.clojure.org/snapshots</url>
        <releases>
          <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
      </repository>
      <repository>
        <id>clojure-releases</id>
        <url>http://build.clojure.org/snapshots</url>
        <releases>
          <enabled>true</enabled>
        </releases>
        <snapshots>
          <enabled>false</enabled>
        </snapshots>
      </repository>
    </repositories>
    <build>
      <plugins>
        <plugin>
          <groupId>com.theoryinpractise</groupId>
          <artifactId>clojure-maven-plugin</artifactId>
          <version>1.3.3</version>
        </plugin>
      </plugins>
    </build>

Put your test sources in `src/test/clojure/`

Then run:

    mvn clojure:repl

And type:

    (use 'lazytest.watch)
    (start ["src"])

And watch your tests run automatically whenever you save a file.

Type CTRL+C to stop.



Making Emacs Indent Tests Properly
==================================

Put the following in `.emacs`

    (eval-after-load 'clojure-mode
      '(define-clojure-indent (describe 'defun) (it 'defun)))



Known Defects
=============

* Changing an applicaton source file does not automatically recompile
  the associated test source file.
