Lazytest
=========

 ** Not under active development **

 ** Latest stable release is on the [stable branch](https://github.com/stuartsierra/lazytest/tree/stable) **

Lazytest: Generic testing backend for Clojure

by Stuart Sierra, http://stuartsierra.com/

Copyright (c) Stuart Sierra, 2010. All rights reserved.  The use and
distribution terms for this software are covered by the Eclipse Public
License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file LICENSE.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.  You must not remove this notice, or any
other, from this software.


One Backend, Many Testing Styles
========================================

Lazytest aims to be a generic library that can support many different
modes and styles of testing. It defines a few generic representations
for executable tests (see "Lazytest Internals", below). Any testing
code that can be compiled into these representations can take
advantage of Lazytest's running and reporting tools.


Getting Started with Leiningen
========================================

Note: These instructions require JDK 6.

Copy the sample project in [modules/sample-leiningen-project](https://github.com/stuartsierra/lazytest/tree/master/modules/sample-leiningen-project)

Put your app sources in `src/` and your test sources in `test/`

Then run:

    lein clean
    lein deps
    java -cp "src:test:classes:lib/*" lazytest.watch src test

And watch your tests run automatically whenever you save a file.

Type CTRL+C to stop.

To run your tests **just once** and stop, run:

    java -cp "src:test:classes:lib/*" lazytest.main src test



Getting Started with Maven
========================================

Copy the sample project in [modules/sample-maven-project](https://github.com/stuartsierra/lazytest/tree/master/modules/sample-maven-project)

Put your app sources in `src/main/clojure/` and your test sources in `src/test/clojure/`

Then run:

    mvn lazytest:watch

And watch your tests run automatically whenever you save a file.

Type CTRL+C to stop.

To run your tests **just once** and stop, run:

    mvn lazytest:run



Testing with 'deftest'
========================================

The `lazytest.deftest` namespace is a drop-in replacement for the
[clojure.test](http://clojure.github.com/clojure/clojure.test-api.html)
library.  Each test is a function defined with the `deftest` macro,
making assertions with the `is` macro:

     (ns examples.readme.deftest
       (:use [lazytest.deftest :only (deftest it thrown? thrown-with-msg?)]))

     (deftest t-addition-with-integers
       ;; arbitrary code may be executed here
       (is (= 4 (+ 2 2)))
       (is (= 7 (+ 3 4))))



Testing with 'describe'
========================================

The `lazytest.describe` namespace mimics the behavior-driven testing
style popularized by libraries such as [RSpec](http://rspec.info/).

Use the `describe` macro to create a group of tests.  Start the group
with a documentation string.

    (ns examples.readme.groups
      (:use [lazytest.describe :only (describe it)]))

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



Nested Test Groups
------------------------------

Test groups may be nested inside other groups with the `testing`
macro, which has the same syntax as `describe` but does not define a
top-level Var:

    (ns examples.readme.nested
      (:use [lazytest.describe :only (describe it testing)]))

    (describe "Addition"
      (testing "of integers"
        (it "computes small sums"
          (= 3 (+ 1 2)))
        (it "computes large sums"
          (= 7000 (+ 3000 4000))))
      (testing "of floats"
        (it "computes small sums"
          (> 0.00001 (Math/abs (- 0.3 (+ 0.1 0.2)))))
        (it "computes large sums"
          (> 0.00001 (Math/abs (- 3000.0 (+ 1000.0 2000.0)))))))



Arbitrary Code in an Example
------------------------------

You can create an example that executes arbitrary code with the
`do-it` macro.  Wrap each assertion expression in the
`lazytest.expect/expect` macro.

    (ns examples.readme.do-it
      (:use [lazytest.describe :only (describe do-it)]
            [lazytest.expect :only (expect)]))

    (describe "Arithmetic"
      (do-it "after printing"
        (expect (= 4 (+ 2 2)))
        (println "Hello, World!")
        (expect (= -1 (- 4 5)))))

The `expect` macro is like `assert` but carries more information about
the failure.  It throws an exception if the expression does not
evaluate to logical true.

If the code inside the `do-it` macro runs to completion without
throwing an exception, the test example is considered to have passed.



Focusing on Individual Tests and Suites
========================================

The `describe`, `testing`, `it`, and `do-it` macros all take an
optional metadata map immediately after the docstring.

Adding `:focus true` to this map will cause *only* that test/suite to
be run.  Removing it will return to the normal behavior (run all
tests).

When using `deftest`, you can put `:focus true` metadata on the symbol
name of your test:

    (deftest ^:focus my-test
      ...)



Generating Random Test Data
========================================

The `lazytest.random` namespace provides functions for generating
random input data for your tests.



Lazytest Internals
========================================

The smallest unit of testing is a *test case*, which is a function
(see `lazytest.test-case/test-case`).  When the function is called, it
may throw an exception to indicate failure.  If it does not throw an
exception, it is assumed to have passed.  The return value of a test
case is always ignored.  Running a test case may have side effects.
The macros `lazytest.describe/it` and `lazytest.describe/do-it` create
test cases.

Tests cases are organized into *suites*.  A test suite is a function
(see `lazytest.suite/suite`) that returns a *test sequence*.  A test
sequence (see `lazytest.suite/test-seq`) is a sequence, possibly lazy,
of test cases and/or test suites.  Suites, therefore, may be nested
inside other suites, but nothing may be nested inside a test case.
The macros `lazytest.describe/describe` and
`lazytest.describe/testing` create test suites.

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
========================================

Put the following in `.emacs`

    (eval-after-load 'clojure-mode
      '(define-clojure-indent
         (describe 'defun)
         (testing 'defun)
         (given 'defun)
         (using 'defun)
         (with 'defun)
         (it 'defun)
         (do-it 'defun)))
