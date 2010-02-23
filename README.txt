LazyTest : a new Clojure testing framework

by Stuart Sierra, http://stuartsierra.com/



WHY?

Why another test framework?  The clojure.test (formerly
clojure.contrib.test-is) library is pretty good.  But it isn't
perfect.  It relies heavily on dynamic context, making it difficult to
parallelize.  It doesn't support lazy evaluation.  Finally, it
provides insufficient separation of assertions from the contexts in
which they are run.



THE BASICS

A Test Case is a collection of assertions about one or more values.
Create a Test Case with deftest:

    (deftest addition [a 1, b 2]
      (integer? a)
      (integer? b)
      (integer? (+ a b)))

The deftest macro takes a vector of bindings, like "let".  Each
expression in the body of the deftest is an assertion about the values
of those bindings.

Test Cases can be called like functions, with no arguments.  They
return a result object.  Use the success? function to find out if the
test passed:

    (success? (addition))
    ;;=> true

A Context is a function that supplies values to a Test Case.  Contexts
are created with defcontext:

    (defcontext random-int []
      (rand-int Integer/MAX_VALUE))

And used as values in Test Cases:

    (deftest random-addition [a random-int, b random-int]
      (integer? (+ a b))
      (= (+ a b) (+ b a)))

    (success? (random-addition))
    ;;=> true

Multiple Test Cases can be combined into Test Suites with defsuite.
Test Suites can also be run as functions:

    (deftest failure "This test always fails." [] (= 1 0))

    (defsuite all-tests [] addition random-addition failure)

    (success? (all-tests))
    ;;=> false

You probably want some more information that just whether or not the
tests passed.  Try the simple-report function:

    (simple-report (all-tests))
     FAIL all-tests
          OK addition
             OK (integer? a)
             OK (integer? b)
             OK (integer? (+ a b))
          OK random-addition
             OK (integer? (+ a b))
             OK (= (+ a b) (+ b a))
          FAIL failure
               FAIL (= 1 0)



MORE ADVANCED

A Context is actually a *pair* of functions, one that runs before the
test and one that runs after it.  These correspond to the
"setup/teardown" functions in other test frameworks.

The body of the "before" function will consist of the expressions
inside defcontext up to the keyword :after.  Expressions following
:after will become the body of the "after" function.

The "before" function returns a value representing some state.  That
value will be passed to Test Cases that use the Context.

The keyword :after must be followed by a vector containing a single
symbol.  When the "after" function runs, that symbol will be bound to
the state returned by the "before" function.

    (defcontext name []
       ... body of "before" function ...
       ... returns some state ...
       :after [x]
       ... body of "after" function ...
       ... where x is the state ...)

Contexts may be composed.  The vector immediately following the name
in defcontext is a bindings vector, just like in deftest.  These
bindings are available in both the "before" and "after" functions.

    (defcontext name [a context-one
                      b context-two]
       ... body of "before" function ...
       ... a and b are the states of contexts one and two ...
       :after [x]
       ... body of "after" function ...
       ... where x is the state ...
       ... a and b are also available here ...)


Contexts may also be attached to Test Suites, by placing them in the
vector following the name in defsuite:

    (defsuite name [contexts...] ... test cases ...)

When a Context is attached to a Test Suite, its before/after functions
execute only ONCE for the entire suite.

   (defcontext context-one [] 1)

   (deftest my-test [x context-one] (pos? x) (= x 1))

   (defsuite long-suite []
      my-test my-test my-test)

   (long-suite) ;; context-one runs three times

   (defsuite short-suite [context-one]
      my-test my-test my-test)

   (short-suite) ;; context-one runs once



REALLY ADVANCED

Test Cases are both instances of the datatype TestCase.  You can
create instances of TestCase containing assertions:

    (TestCase [contexts...] [assertions...])

Assertions can be created with the macros defassert (like defn) or
assertion (like fn):

    (defassert name [args...]  ... body ...)

    (assertion [args...]  ... body ...)

When the TestCase is run, the results of its contexts' "before"
functions will passed as arguments to each assertion.  Each assertion,
therefore, must have the same number of arguments as there are
contexts in its parent TestCase.

Test Suites are also instances of TestCase, containing other TestCases
instead of assertions:

   (TestCase [contexts...] [TestCases...])



REPORTING

The reporting functions look for :name and :doc metadata on the
TestCases and assertions.  The macros defassert, deftest, and defsuite
add this metadata automatically.



PARALLEL TEST EXECUTION

Finally, the reason why I wrote this library.  Tests are run with a
Test Execution Strategy, passed as an argument to the Test Case or
Test Suite.  There are three built-in strategies:

 * default-strategy  - mostly lazy, uses map
 * lazy-strategy     - completely lazy, avoids chunked sequences
 * parallel-strategy - uses pmap

The function parallel-upto takes an integer, n, and returns a strategy
that will be parallel only n levels deep in the tree of test cases.
After that, the default strategy resumes.  This is useful when you
have several large test suites that you want to run in parallel, but
the overhead of pmap is not worthwhile for tests within each suite.
