Notes on Possible New Syntax
============================

Examples are ordinary functions, with arguments.

Contexts are before/after function pairs that supply arguments to the
examples.

The main macro will be `describe` and everything will be controlled by
that.  `describe` creates an object called an Group, which links
examples and contexts.



Syntax of describe
------------------

To create an Group:

    (describe symbol "doc string" ...)

Both symbol and doc string are optional.  The symbol, if present, must
name a Var mapping int he current namespace. 

To create an Group in which the local variable `x` is bound to
the state returned by the "before" function of `context1`, a context
created with `defcontext`:

    (describe ... :using [x context1] ...)

To create an Group in which the local variable `x` is bound to
the result of evaluating `expression`:

    (describe ... :given [x expression] ...)

Assertion expressions are placed inside the `it` macro.

    (describe ... (it ... examples ...))

Each expression inside `it` will be compiled into an example function.
Any expression inside `it` may optionally be preceded by a string,
which will become its documentation string.


Nested describe
---------------

To create an Group nested inside another Group:

    (describe target "optional doc string"
      ...
      (describe "optional doc string" ... ))

The "inner" `describe` inherits the contexts and local variables of
the "outer" `describe`.

Only the "outer" `describe` may have a target Var or namespace; the
"inner" `describe` may only have a documentation string.

Documentation strings of nested Groups will be concatenated as
in RSpec.



Examples
========

    (describe + "The addition function"
      (it
        "should compute the sum of two numbers"
        (= 5 (+ 2 3))

        "should return 0 with no arguments"
        (zero? (+))

        "should throw on non-numeric argument"
        (thrown? Exception (+ 3 :a))))


    (describe get "The get function"
      :given [m {:a 1 :b 2}]

      (it
        "should return the value for the key"
        (= 1 (get m :a))

        "should return nil if the key is not present"
        (= nil (get m :c))

        "should return a default value if the key is not present"
        (= 3 (get m :c 3))))
