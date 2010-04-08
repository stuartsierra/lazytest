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

Specs ("specifications") are always defined within the `spec` macro:

    (use 'com.stuartsierra.lazytest)

    (spec name? "docstring?" body...)

`spec` takes an optional name (a symbol), which will be def'd in
the current namespace.  You can call that symbol like a function.

After the name comes an optional doc string.

The body consists of test or assertion expressions.  Simple assertions
are created with the `is` macro:

    (is assertions...)

Each assertion is an isolated expression that returns logical true or
false:

    (spec simple-addition
       (is (= 2 (+ 1 1))
           (= 4 (+ 2 2))))

Within `is`, any assertion may be preceded by a doc string:

    (spec confused-addition
       (is "Two plus two is four"
           (= 4 (+ 2 2))
           "Two plus two is five?!"
           (= 5 (+ 2 2))))

`spec` expressions may be nested to any depth:

    (spec minus "The minus function"
      (spec "when called with one argument"
        (spec "negates that argument"
          (is (= -1 (- 1))
              (= -2 (- 2)))))
      (spec "when called with two arguments"
        (spec "subtracts"
          (is (= 0 (- 5 5))
              (= 1 (- 3 2))))))

Documentation strings for nested specs will be joined together in
reports.

Any spec may have a symbol name, making it a callable function.  So,
for example:

    (spec top "This is the top-level spec"
      (spec one "with one second-level spec"
        (spec one-a "with a third-level spec"
          ...))
      (spec two "and another second-level spec" 
          ...))

This example creates FOUR functions: top, one, one-a, and two.  Any
one of them can be called as a function, with no arguments, to run the
specs it contains.  So call `(top)` to run all the specs, or `(one)` to
run just those specs inside `(spec one ...)`.


Running Specs, Reporting Results
--------------------------------

Running a spec returns a lazy sequence of test results.

To get a nicely-formatted report of those results, use the
`spec-report` function:

    (use '[com.stuartsierra.lazytest.report :only (spec-report)])

    (spec-report (confused-addition))

The report uses ANSI color codes by default.  If your environment does
not support ANSI terminal commands, turn colorizing off:

    (use '[com.stuartsierra.lazytest.color :only (set-colorize)])

    (set-colorize false)


Finding Specs
-------------

To attach a spec to a namespace, use the `describe` macro:

    (describe target-ns ...)

The body of `describe` is exactly the same as `spec`; it attaches the
spec as `:spec` metadata on the target-ns.

The best way to use `describe` is with the current namespace:

    (describe *ns* "specs for this namespace")

I recommend storing specs and "main" code in separate namespaces; link
them together by putting `:spec` metadata on the "main" namespace,
giving the symbol name of the "specs" namespace.  For example, if your
specs look like this:

    (ns com.example.foo-spec)

    (describe *ns* "The Foo library"
      (spec "should work" ...))

then define your main namespace like this:

    (ns #^{:spec 'com.example.foo-spec} 
        com.example.foo)

To find and run specs, use the `run-spec` function, which loads a
namespace and runs the specs associated with it.  Following the above
example, you could run the specs for the Foo library using either of
the following:

    (spec-report (run-spec 'com.example.foo))

    (spec-report (run-spec 'com.example.foo-spec))

The `run-spec` function accepts the same `:reload` and `:reload-all`
options as `require` or `use`.

To load specs without running them, use `load-spec` instead.  To find
specs without loading them, use `find-spec`.

To run all specs in all loaded namespaces, use:

    (spec-report (run-spec (all-ns)))

Alternatively, to load and run specs in all namespaces found under a
certain directory, call `run-spec` with the name of the directory:

    (spec-report (run-spec "src/test/clojure"))



Contexts
========

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

Contexts are used in specs with the `given` macro:

    (spec ... (given [bindings...] assertions...))

`given` is like `is` but starts with a binding vector.  The binding
vector consists of name-value pairs, like `let`, where each value is a
context object.  When the assertions are run, the names will be
locally bound to the state returned by the contexts' "before"
functions.  Example:

    (defcontext calculate-pi []
      Math/PI)

    (spec pi-tests
      (given [pi (calculate-pi)]
        (< (* pi pi) 10)))

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
