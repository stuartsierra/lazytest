Notes on Possible New Syntax
============================

The main macro will be `describe` and everything will be controlled by
that.  `describe` creates an object called an ExampleGroup, which
links examples and contexts.

Examples are ordinary functions, with arguments.

Contexts are before/after function pairs that supply arguments to the
examples.


Syntax of describe
------------------

After parsing all optional arguments, any remaining expressions inside
`describe` are compiled into examples, i.e. anonymous functions.

Any example expression may be preceeded by a documentation string.

To create an ExampleGroup and add it to the metadata of the Var or
namespace named by `sym`:

    (describe sym "optional doc string" ...)

To create an ExampleGroup and add it to the metadata of the current
namespace:

    (describe "optional doc string" ...)

To create an ExampleGroup in which the local variable `x` is bound to
the state returned by the "before" function of `context1`, a context
created with `defcontext`:

    (describe ... :using [x context1] ...)

To create an ExampleGroup in which the local variable `x` is bound to
the result of evaluating `expression`:

    (describe ... :given [x expression] ...)


Nested describe
---------------

To create an ExampleGroup nested inside another ExampleGroup:

    (describe target "optional doc string"
      ...
      (describe "optional doc string" ... ))

The "inner" `describe` inherits the contexts and local variables of
the "outer" `describe`.

Only the "outer" `describe` may have a target Var or namespace; the
"inner" `describe` may only have a documentation string.

Documentation strings of nested ExampleGroups will be concatenated as
in RSpec.



Examples
========

   (describe + "The addition function"
     "should compute the sum of two numbers"
     (= 5 (+ 2 3))

     "should return 0 with no arguments"
     (zero? (+))

     "should throw on non-numeric argument"
     (thrown? Exception (+ 3 :a)))


   (describe get "The get function"
     :given [m {:a 1 :b 2}]

     "should return the value for the key"
     (= 1 (get m :a))

     "should return nil if the key is not present"
     (= nil (get m :c))

     "should return a default value if the key is not present"
     (= 3 (get m :c 3)))
