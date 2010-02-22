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

An Assertion is a function, created with defassert.  The body of the
function should return true if the assertion passes or false if it
fails.

    (defassert positive [x] (pos? x))

You can call an assertion like an ordinary function; it returns a
result object.  The success? function tells you if the result was
successful or not.

    (success? (positive 1))
    ;;=> true
    (success? (positive -1))
    ;;=> false

It also catches errors and wraps them in a result object as well.

    (success? (positive "hello"))
    ;;=> false

Usually you want to make several assertions about the same value.  You
can group Assertions into Test Cases with deftest.

    (deftest addition [a 1, b 2]
      (integer? a)
      (integer? b)
      (integer? (+ a b)))

The deftest macro takes a vector of bindings, like "let".  Each
expression in the body of the deftest will be compiled into an
assertion using those bindings.

Test Cases can also be called like functions, with no arguments:

    (success? (addition))
    ;;=> true

A Context is a pair of functions that will be called "around" a Test
Case.  Contexts are created with defcontext:

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

    (defsuite all-tests [] addition random-addition)

    (success? (all-tests))
    ;;=> true
