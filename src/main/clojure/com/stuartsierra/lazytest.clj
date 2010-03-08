;;; lazytest.clj

;; by Stuart Sierra, http://stuartsierra.com/

;; Copyright (c) 2010 Stuart Sierra. All rights reserved.  The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0, which can be found at
;; http://opensource.org/licenses/eclipse-1.0.php
;; and in the file LICENSE.html at the root if this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound
;; by the terms of this license.  You must not remove this notice, or
;; any other, from this software.


(ns #^{:doc "Lazy Testing Framework"
       :author "Stuart Sierra"}
  com.stuartsierra.lazytest)

;;; PROTOCOLS

;; Generic reporting function for any Test/Assertion Result
(defprotocol TestSuccess
  (success? [r] "Returns true if r is a 100% successful result."))

;; Generic entry-point for running tests.
(defprotocol Testable
  (run-tests [x] "Runs tests defined for the Namespace, Var, or TestCase."))

;; Internal generic Test/Assertion invocation.
(defprotocol TestInvokable
  (invoke-test [t states strategy active]
               "(private) Executes the TestCase or Assertion."))

(declare AssertionPassed AssertionFailed AssertionThrown)

;; Fn is assumed to be an assertion predicate
(extend-class clojure.lang.Fn TestInvokable
  (invoke-test [f states strategy active]
    (try (if (apply f states)
           (AssertionPassed f states)
           (AssertionFailed f states))
         (catch Throwable t#
           (AssertionThrown f states t#)))))

;;; DATATYPES

(declare run-test-case)

;; Context sets up state for TestCases that depend on it.  parents is
;; a vector of parent Contexts.  before and after are functions.
;; before takes the same number of arguments as there are parent
;; Contexts.  after takes the same arguments, plus an additional first
;; argument, which is the state returned by the before function.
(deftype Context [parents before after])

;; TestCase represents either a "test", which associates Contexts
;; and Assertions, or a "test suite", which is a collection of tests.
;;
;; contexts is a vector of Contexts.  children is a vector of
;; Assertions (optionally compiled into Fns) or a vector of other
;; TestCases.
(deftype TestCase [contexts children] :as this
  clojure.lang.IFn
  (invoke [] (run-test-case this))
  (invoke [strategy] (run-test-case this strategy))
  (invoke [strategy active] (run-test-case this strategy active))
  TestInvokable
  (invoke-test [states strategy active]
    (run-test-case this strategy active)))

;; TestResult represents the result of executing a TestCase.  source
;; is the TestCase.  children is a sequence of results from child
;; TestCases or Assertions.
(deftype TestResult [source children]
  TestSuccess (success? [] (every? success? children)))
 
;; TestThrown represents that a TestCase threw an exception somewhere
;; NOT in an Assertion, such as during setup or in a Context function.
;; source is the TestCase, error is the java.lang.Throwable.
(deftype TestThrown [source error]
  TestSuccess (success? [] false))

;; AssertionPassed is returned by an Assertion whose expression
;; evaluates logical true.
(deftype AssertionPassed [source states]
  TestSuccess (success? [] true))

;; AssertionFailed is returned by an Assertion whose expression
;; evaluates logical false without throwing an exception.
(deftype AssertionFailed [source states]
  TestSuccess (success? [] false))

;; AssertionThrown is returned by an Assertion whose expression threw
;; an exception.  error is the java.lang.Throwable.
(deftype AssertionThrown [source states error]
  TestSuccess (success? [] false))


;;; CONTEXT HANDLING

(defn open-context
  "Opens context c, and all its parents, unless it is already active."
  [active c]
  (let [active (reduce open-context active (:parents c))
        states (map active (:parents c))]
    (if-let [f (:before c)]
      (assoc active c (or (active c) (apply f states)))
      active)))

(defn close-context
  "Closes context c and removes it from active."
  [active c]
  (let [states (map active (:parents c))]
    (when-let [f (:after c)]
      (apply f (active c) states))
    (let [active (reduce close-context active (:parents c))]
      (dissoc active c))))

(defn- coerce-context
  "Make a Context out of c, at compile time."
  [c]
  (if (and (symbol? c) (= ::Context (type (var-get (resolve c)))))
    c (Context [] (fn [] c) nil)))

(defmacro defcontext
  "Defines a context.
  decl => docstring? [bindings*] before-body* after-fn?
  after-fn => :after [state] after-body*"
  [name & decl]
  (let [m {:name name, :ns *ns*, :file *file*, :line @Compiler/LINE}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        bindings (first decl)
        bodies (next decl)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [locals (vec (map first (partition 2 bindings)))
          contexts (vec (map (comp coerce-context second) (partition 2 bindings)))
          before (take-while #(not= :after %) bodies)
          after (next (drop-while #(not= :after %) bodies))
          before-fn `(fn ~locals ~@before)]
      (when after (assert (vector? (first after))))
      (let [after-fn (when after
                       `(fn ~(vec (concat (first after) locals))
                          ~@after))]
        `(def ~name (Context ~contexts ~before-fn ~after-fn '~m nil))))))


;;; TEST RUNNING STRATEGIES

(defn- map1
  "Like map but does not chunk results; slower but lazier than map."
  [f coll]
  (lazy-seq
   (when (seq coll)
     (cons (f (first coll))
           (map1 f (next coll))))))

(defn default-strategy []
  "Default test execution strategy; uses (chunked) map."
  [map default-strategy])

(defn lazy-strategy []
  "Truly lazy test execution strategy; uses (unchunked) map1."
  [map1 lazy-strategy])

(defn parallel-strategy
  "Parallel execution strategy; uses pmap."
  []
  [pmap parallel-strategy])

(defn parallel-upto
  "Returns a strategy that is parallel up to n levels of recursion,
  then reverts to default-strategy."
  [n]
  (if (zero? n)
    default-strategy
    [pmap #(parallel-upto (dec n))]))


;;; TEST CASE HANDLING

(defn- has-after?
  "True if Context c or any of its parents has an :after function."
  [c]
  (or (:after c)
      (some has-after? (:parents c))))

(defn run-test-case
  "Executes a test case in context.  active is the map of currently
  active Contexts, empty by default.  strategy is a function that
  determines how tests are executed, default-strategy by default."
  ([t] (run-test-case t default-strategy))
  ([t strategy] (run-test-case t strategy {}))
  ([t strategy active]
     {:pre [(= ::TestCase (type t))
            (every? #(= ::Context (type %)) (:contexts t))]}
     (try
      (let [[mapper child-strategy] (strategy)
            merged (reduce open-context active (:contexts t))
            states (map merged (:contexts t))
            results (mapper
                     #(invoke-test % states child-strategy merged)
                     (:children t))]
        ;; Force non-lazy execution to handle shutdown properly:
        (when (some has-after? (:contexts t))
          (dorun results)
          (dorun (reduce close-context merged
                         ;; Only close contexts that weren't active at start:
                         (filter #(not (contains? active %))
                                 (reverse (:contexts t))))))
        (TestResult t results))
      (catch Throwable e (TestThrown t e)))))

(defmacro test-case
  "Defines a test case containing assertions that share the same contexts.
  decl => docstring? [binding*] assertion*
  binding => symbol context
  assertion => docstring? expression"
  [name & decl]
  (let [m {:name name, :ns *ns*, :file *file*, :line @Compiler/LINE}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        bindings (first decl)
        assertions (next decl)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [locals (vec (map first (partition 2 bindings)))
          contexts (vec (map (comp coerce-context second)
                             (partition 2 bindings)))]
      (assert (every? symbol locals))
      `(TestCase
        ~contexts
        ~(loop [r [], as assertions]
           (if (seq as)
             (if (string? (first as))
               (recur (conj r `(with-meta (fn ~locals ~(second as))
                                 {:doc ~(first as),
                                  :form '~(second as),
                                  :file *file*,
                                  :line @Compiler/LINE}))
                      (nnext as))
               (recur (conj r `(with-meta (fn ~locals ~(first as))
                                 {:form '~(first as),
                                  :file *file*,
                                  :line @Compiler/LINE}))
                      (next as)))
             r))
        '~m nil))))

(defmacro deftest [name & decl]
  `(def ~name (test-case ~name ~@decl)))

(defmacro suite
  "Return a test suite containing other test cases or suites.
  decl => docstring? [context*] children*"
  [name & decl]
  (let [m {:name name, :ns *ns*, :file *file*, :line @Compiler/LINE}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        contexts (first decl)
        children (next decl)]
    (assert (vector? contexts))
    `(TestCase ~contexts ~(vec children) '~m nil)))

(defmacro defsuite [name & decl]
  `(def ~name (suite ~name ~@decl)))


;;; TEST RESULT HANDLING

(defn assertion-result?
  "True if r is an assertion result (pass, fail, or throw)"
  [r]
  (#{::AssertionPassed ::AssertionFailed ::AssertionThrown}
   (type r)))

(defn result-seq
  "Given a single TestResult, returns a depth-first sequence of that
  TestResult and all its children."
  [r]
  (tree-seq :children :children r))


;;; TESTABLE IMPLEMENTATIONS

(defn var-test-case
  "Returns a TestCase for a Var.  If the Var's value is a TestCase,
  returns that.  If the Var's :test metadata is a TestCase, returns
  that.  If the Var's :test metadata is a function, generates a
  TestCase with an assertion that calls the function.  If none of
  these are true, returns nil."  [v]
  (cond (= ::TestCase (type (var-get v)))
        (var-get v)

        (= ::TestCase (type (:test (meta v))))
        (:test (meta v))

        (fn? (:test (meta v)))
        (TestCase [] [(:test (meta v))]
                  {:doc "Var :test metadata function."
                   :name (:name (meta v))
                   :ns (:ns (meta v))}
                  nil)

        :else nil))
 
(defn test-var
  "Finds and runs a TestCase for the Var."
  [v]
  (run-test-case (var-test-case v)))
 
(defn ns-test-case
  "Returns a TestCase for a Namespace.  If the namespace's :test
  metadata is a TestCase, returns that.  If the namespace's :test
  metadata is another namespace, recurses on that.  If the
  namespace's :test metadata is a symbol, require's that namespace and
  recurses.  If the namespace's :test metadata is a function,
  generates a TestCase with an assertion that calls the function.  If
  none of these are true, returns nil."  [n]
  (let [t (:test (meta n))]
    (cond (= ::TestCase (type t))
          t

          (instance? clojure.lang.Namespace t)
          (ns-test-case t)

          (symbol? t)
          (do (require t)
              (ns-test-case (the-ns t)))

          (fn? t)
          (TestCase [] [t]
                    {:doc "Namespace :test metadata function."
                     :name (ns-name n)}
                    nil)

;; todo: generate test-all-vars testcase
          :else nil)))
 
(defn test-ns
  "Finds and runs a TestCase for the namespace."
  [n]
  (run-test-case (ns-test-case n)))
 
(extend ::TestCase Testable {:run-tests run-test-case})
(extend clojure.lang.Var Testable {:run-tests test-var})
(extend clojure.lang.Namespace Testable {:run-tests test-ns})

(defn all-ns-test-case
  "Returns a TestCase for all Namespaces."
  []
  (TestCase [] (vec (map (fn [n] (ns-test-case n)) (all-ns)))
            {:name 'all-ns-tests,
             :doc "Generated by all-ns-test-case"}
            nil))