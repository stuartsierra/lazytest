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

    (defcontext name []
       ... body of before function ...
       :after [x]
       ... body of after function ...)

The body of the "before" function will consist of the expressions
inside defcontext up to the keyword :after.  Expressions following
:after will become the body of the "after" function.

The "before" function returns a value, the "state" of the context.
The keyword :after must be followed by a vector containing a single
symbol.  When the "after" function runs, that symbol will be bound to
the state returned by the "before" function.
