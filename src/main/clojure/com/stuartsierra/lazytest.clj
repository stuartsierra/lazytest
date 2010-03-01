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

(defprotocol TestSuccess
  (success? [r] "Returns true if r is a 100% successful result."))

(defprotocol Testable
  (run-tests [x] "Runs tests defined for the Namespace, Var, or TestCase."))

(defprotocol TestInvokable
  (invoke-test [t states strategy active]
               "(private) Executes the TestCase or Assertion."))

(extend-class clojure.lang.Fn TestInvokable
              (invoke-test [f states strategy active] (apply f states)))

;;; DATATYPES

(declare run-test-case compile-assertion)

(deftype Context [parents before after])

(deftype TestCase [contexts children] :as this
  clojure.lang.IFn
  (invoke [] (run-test-case this))
  (invoke [strategy] (run-test-case this strategy))
  (invoke [strategy active] (run-test-case this strategy active))
  TestInvokable (invoke-test [states strategy active]
                             (run-test-case this strategy active)))

(deftype Assertion [locals form] :as this
  TestInvokable
  (invoke-test [states strategy active]
               (invoke-test (compile-assertion this)
                            states strategy active)))

(deftype TestResult [source children]
  TestSuccess (success? [] (every? success? children)))
 
(deftype TestThrown [source error]
  TestSuccess (success? [] false))
 
(deftype AssertionPassed [source]
  TestSuccess (success? [] true))
 
(deftype AssertionFailed [source]
  TestSuccess (success? [] false))
 
(deftype AssertionThrown [source error]
  TestSuccess (success? [] false))


;;; ASSERTION HANDLING

(defn- format-assertion [a]
  {:pre [(= ::Assertion (type a))
         (vector? (:locals a))]}
  (let [form (:form a)]
    `(fn ~(:locals a)
       (try (if ~form
              (AssertionPassed '~form)
              (AssertionFailed '~form))
            (catch Throwable t#
              (AssertionThrown '~form t#))))))
 
(defn compile-assertion [a]
  (eval (format-assertion a)))
 
(defmacro assertion [locals & body]
  (format-assertion (Assertion locals (if (= 1 (count body))
                                        (first body) `(do ~@body)))))

(defmacro defassert
  "Defines an Assertion.
  decl => docstring? [locals*] body*"
  [name & decl]
  (let [m {:name name, :ns *ns*}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        argv (first decl)
        m (assoc m :locals argv)
        body (next decl)]
    (assert (vector? argv))
    `(def ~name (with-meta (assertion ~argv ~@body)
                  '~m))))


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

(defn- coerce-context [c]
  (if (and (symbol? c) (= ::Context (type (var-get (resolve c)))))
    c (Context [] (fn [] c) nil)))

(defmacro defcontext
  "Defines a context.
  decl => docstring? [bindings*] before-body* (:after [state] after-body*)?"
  [name & decl]
  (let [m {:name name, :ns *ns*}
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
        `(def ~name (with-meta (Context ~contexts ~before-fn ~after-fn)
                      '~m))))))


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

(defn parallel-strategy []
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
  active Contexts.  strategy is a function that determines how tests
  are executed."
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

(defmacro deftest
  "Defines a test case containing assertions that share the same contexts.
  decl => docstring? [binding*] assertion*
  binding => symbol context
  assertion => docstring? expression"
  [name & decl]
  (let [m {:name name, :ns *ns*}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        bindings (first decl)
        assertions (next decl)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [locals (vec (map first (partition 2 bindings)))
          contexts (vec (map (comp coerce-context second) (partition 2 bindings)))]
      (assert (every? symbol locals))
      `(def ~name
            (with-meta (TestCase ~contexts
                                 ~(loop [r [], as assertions]
                                    (if (seq as)
                                      (if (string? (first as))
                                        (recur (conj r `(with-meta (assertion ~locals ~(next as))
                                                          {:doc ~(first as), :form '~(next as)}))
                                               (nnext as))
                                        (recur (conj r `(with-meta (assertion ~locals ~(first as))
                                                          {:form '~(first as)}))
                                               (next as)))
                                      r)))
              '~m)))))

(defmacro defsuite
  "Defines a test suite containing other test cases or suites.
  decl => docstring? [context*] children*"
  [name & decl]
  (let [m {:name name, :ns *ns*}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        contexts (first decl)
        children (next decl)]
    (assert (vector? contexts))
    `(def ~name (with-meta (TestCase ~contexts ~(vec children))
                  '~m))))


;;; TEST RESULT HANDLING

(defn result-seq
  "Given a single TestResult, returns a depth-first sequence of that
  TestResult and all its children."
  [r]
  (tree-seq :children :children r))

(require 'clojure.stacktrace)

(defn result-identifier [r]
  (let [m (meta (:source r))]
    (or (:name m) (:form m)
        (:source r))))

(defn simple-report
  ([r] (simple-report r ""))
  ([r indent]
     (cond (= ::TestResult (type r))
           (if (success? r)
             (do (println indent "OK" (result-identifier r))
                 (doseq [c (:children r)]
                   (simple-report c (str indent "   "))))
             (do (println indent "FAIL" (result-identifier r))
                 (when-let [d (:doc (meta (:source r)))] (prn d))
                 (doseq [c (:children r)]
                   (simple-report c (str indent "     ")))))

           (= ::TestThrown (type r))
           (do (println indent "ERROR" (result-identifier r))
               (when-let [d (:doc (meta r))] (prn d))
               (clojure.stacktrace/print-cause-trace (:error r)))

           (= ::AssertionPassed (type r))
           (println indent "OK" (result-identifier r))

           (= ::AssertionFailed (type r))
           (do (println indent "FAIL" (result-identifier r))
               (when-let [d (:doc (meta r))] (prn d)))

           (= ::AssertionThrown (type r))
           (do (println indent "ERROR" (result-identifier r))
               (when-let [d (:doc (meta r))] (prn d))
               (clojure.stacktrace/print-cause-trace (:error r))))))


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
        (with-meta (TestCase [] [(assertion [] ((:test (meta v))))])
          {:doc "Var :test metadata function."
           :name (:name (meta v))
           :ns (:ns (meta v))})

        :else nil))
 
(defn test-var
  "Finds and runs a TestCase for the Var."
  [v]
  (run-test-case (var-test-case v)))
 
(defn ns-test-case
  "Returns a TestCase for a Namespace.  If the namespace's :test
  metadata is a TestCase, returns that.  If the namespace's :test
  metadata is another namespace, recurses on that.  If the
  namespace's :test metadata is a function, generates a TestCase with
  an assertion that calls the function.  If none of these are true,
  returns nil."  [n]
  (cond (= ::TestCase (type (:test (meta n))))
        (:test (meta n))

        (instance? clojure.lang.Namespace (:test (meta n)))
        (ns-test-case (:test (meta n)))

        (fn? (:test (meta n)))
        (with-meta (TestCase [] [(assertion [] ((:test (meta n))))])
          {:doc "Namespace :test metadata function."
           :name (ns-name n)})

        :else nil))
 
(defn test-ns
  "Finds and runs a TestCase for the namespace."
  [n]
  (run-test-case (ns-test-case n)))
 
(extend ::TestCase Testable {:run-tests run-test-case})
(extend clojure.lang.Var Testable {:run-tests test-var})
(extend clojure.lang.Namespace Testable {:run-tests test-ns})
