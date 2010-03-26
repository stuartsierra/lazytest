LazyTest : a new Clojure testing framework

by Stuart Sierra, http://stuartsierra.com/



Why?
====

Why another test framework?  The `clojure.test` (formerly
clojure.contrib.test-is) library is pretty good.  But it isn't
perfect.  It relies heavily on dynamic context, making it difficult to
parallelize.  It doesn't support lazy evaluation.  Finally, it
provides insufficient separation of assertions from the contexts in
which they are run.



The Basics
==========

Tests are always defined within the `testing` macro:

    (testing name? "docstring?" body...)

`testing` takes an optional name (a symbol), which will be def'd in
the current namespace.  You can call that symbol like a function.

After the name comes an optional doc string.

The body consists of test or assertion expressions.  Simple assertions
are created with the `is` macro:

    (is assertions...)

Each assertion is an isolated expression that returns logical true or
false:

    (testing simple-addition
       (is (= 2 (+ 1 1))
           (= 4 (+ 2 2))))

Within `is`, any assertion may be preceded by a doc string:

    (testing confused-addition
       (is "Two plus two is four"
           (= 4 (+ 2 2))
           "Two plus two is five?!"
           (= 5 (+ 2 2))))

`testing` expressions may be nested to any depth:

    (testing minus "The minus function"
      (testing "when called with one argument"
        (testing "negates that argument"
          (is (= -1 (- 1))
              (= -2 (- 2)))))
      (testing "when called with two arguments"
        (testing "subtracts"
          (is (= 0 (- 5 5))
              (= 1 (- 3 2))))))


Contexts
--------

A Context is a pair of before/after functions that are run around a
test.

    (defcontext name "docstring?" []
       ... body of "before" function ...
       ... returns some state ...
       :after [x]
       ... body of "after" function ...
       ... the state is in 'x' ...
       ... return value is ignored ...)

The :after function is optional.

Contexts are used in tests with the `given` macro:

    (testing ... (given [bindings...] assertions...))

`given` is like `is` but starts with a binding vector.  The binding
vector consists of name-value pairs, like `let`, where each value is a
context object.  When the assertions are run, the names will be
locally bound to the state returned by the contexts' "before"
functions.  Example:

    (defcontext calculate-pi []
      Math/PI)

    (testing pi-tests
      (given [pi (calculate-pi)]
        (is (< (* pi pi) 10))))

Contexts may be composed.  The vector argument after the docstring in
`defcontext` defines "parent contexts" and is composed of name-context
pairs as in `given`.  The full syntax is:

    (defcontext name [a context-one
                      b context-two]
       ... body of "before" function ...
       ... a and b are the states of contexts one and two ...
       :after [x]
       ... body of "after" function ...
       ... where x is the state ...
       ... a and b are also available here ...)

Example:

    (defcontext database "Open a database connection" []
       (open-and-return-database-connection)
       :after [conn]
       (close-database-connection conn))

    (defcontext tables "Create some tables" [db database]
       (create-tables db)
       :after [x]
       (drop-tables db))
