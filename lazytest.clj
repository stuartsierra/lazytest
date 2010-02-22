;;; TEST2: a new testing framework for Clojure

;; by Stuart Sierra, http://stuartsierra.com/
;; February 18, 2010

;; Copyright (C) 2010 Stuart Sierra.

;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0
;; <http://opensource.org/licenses/eclipse-1.0.php>.  By using this
;; software in any fashion, you are agreeing to be bound by the terms
;; of this license.  You must not remove this notice, or any other,
;; from this software.


(ns clojure.test2)

(def *active-contexts* {})

(def *defining-context-bindings* [])

(defprotocol Successful
  (success? [r] "Returns true if the result r was 100% successful."))

(deftype Assertion [form])

(deftype Test [locals contexts assertions])

(deftype Context [before after])

(deftype Suite [contexts children])

(deftype AssertionSuccess [assertion]
  Successful (success? [] true))

(deftype AssertionFailure [assertion]
  Successful (success? [] false))

(deftype AssertionThrown [assertion throwable]
  Successful (success? [] false))

(deftype TestResult [test assertion-results]
  Successful (success? [] (every? success? assertion-results)))

(deftype SuiteResult [suite test-results]
  Successful (success? [] (every? success? test-results)))

(defn context?
  "Returns true if x is a Context."
  [x]
  (= ::Context (type x)))

(defn test?
  "Returns true if x is a Test."
  [x]
  (= ::Test (type x)))

(defn suite?
  "Returns true if x is a Suite."
  [x]
  (= ::Suite (type x)))

(defn- format-assertion [expr]
  `(try (if ~expr
          (AssertionSuccess (quote ~expr))
          (AssertionFailure (quote ~expr)))
        (catch Throwable t#
          (AssertionThrown (quote ~expr) t#))))

;; TODO: Lazy evaluation
(defn- format-test [t]
  {:pre [(test? t)
         (vector? (:locals t))
         (every? symbol? (:locals t))]}
  `(fn ~(:locals t)
     (list ~@(map format-assertion (:assertions t)))))

(defn- compile-test* [t]
  (eval (format-test t)))

(def #^{:doc "Compile a Test to a function of its locals. (memoized)"
        :arglists '([t])}
     compile-test (memoize compile-test*))

(defn- open-context 
  "Calls Context c's :before function, returns result."
  [c]
  (or (*active-contexts* c)
      (when-let [f (:before c)]
        (f))))

(defn- close-context
  "Calls Context c's :after function on state."
  [c state]
  (when-not (contains? *active-contexts* c)
    (when-let [f (:after c)]
      (f state))))

(defn call-with-contexts
  "Applies f to the result of opening all contexts,
  then closes contexts in reverse order."
  [contexts f]
  {:pre [(every? context? contexts)]}
  (let [states (map open-context contexts)
        result (binding [*active-contexts*
                         (merge *active-contexts*
                                (zipmap contexts states))]
                 (apply f states))]
    (dorun (map close-context
                (reverse contexts) (reverse states)))
    result))

(defn run-test
  "Run a Test t, return a TestResult."
  [t]
  {:pre [(test? t)]}
  (TestResult t (call-with-contexts (:contexts t)
                  (compile-test t))))

(declare run)

(defn run-suite
  "Run a Suite s, return a SuiteResult."
  [s]
  (SuiteResult s (call-with-contexts (:contexts s)
                   (fn [& _]
                     (doall (map run (:children s)))))))


(defn run [x]
  (cond (suite? x) (run-suite x)
        (test? x) (run-test x)
        :else (throw (IllegalArgumentException.
                      "Suites may only contain Tests and other Suites"))))



;;; CONVENIENCE MACROS

(defn- reformat-context [x]
  (if (symbol? x) x
      `(Context (fn [] ~x) nil)))

(defmacro make-test
  "args => docstring? [bindings*] assertions*

  bindings => local-name context

  If context is an expression, will create an anonymous 
  context using that expression in its :before function."
  [& args]
  (let [docstring (if (string? (first args))
                    (first args)
                    nil)
        args (if (string? (first args))
                (next args) args)
        context-bindings (if (vector? (first args))
                           (first args)
                           [])
        assertions (if (vector? (first args))
                     (next args) args)]
    `(Test (quote ~(vec (map first (partition 2 context-bindings))))
           ~(vec (map (comp reformat-context second)
                      (partition 2 context-bindings)))
           (quote ~(vec assertions))
           {:doc ~docstring} {})))

(defmacro deftest [name & args]
  `(def ~name (let [t# (make-test ~@args)]
                (with-meta t# (assoc (meta t#)
                                :name '~name
                                :ns *ns*
                                :file *file*
                                :line @clojure.lang.Compiler/LINE)))))

(declare reformat-child)

(defmacro make-child-test [parent-bindings & args]
  (let [docstring (if (string? (first args))
                    (first args)
                    nil)
        args (if (string? (first args))
                (next args) args)
        context-bindings (if (vector? (first args))
                           (vec (concat parent-bindings (first args)))
                           parent-bindings)
        assertions (if (vector? (first args))
                     (next args) args)]
    `(Test (quote ~(vec (map first (partition 2 context-bindings))))
           ~(vec (map (comp reformat-context second)
                      (partition 2 context-bindings)))
           (quote ~(vec assertions))
           {:doc ~docstring} {})))

(defmacro make-child-suite [parent-bindings & args]
  (let [docstring (if (string? (first args))
                    (first args)
                    nil)
        args (if (string? (first args))
                (next args)
                args)
        context-bindings (if (vector? (first args))
                           (vec (concat parent-bindings (first args)))
                           parent-bindings)
        children (if (vector? (first args))
                   (next args) args)]
    `(Suite ~(vec (map (comp reformat-context second) (partition 2 context-bindings)))
            ~(vec (map (partial reformat-child context-bindings) children))
            {:doc ~docstring} {})))

(defn reformat-child [parent-bindings args]
  (cond (symbol? args) args
        (= :test (first args)) `(make-child-test ~parent-bindings ~@(rest args))
        (= :suite (first args)) `(make-child-suite ~parent-bindings ~@(rest args))
        :else args))

(defmacro make-suite [& args]
  (let [docstring (if (string? (first args))
                    (first args)
                    nil)
        args (if (string? (first args))
                (next args)
                args)
        context-bindings (if (vector? (first args))
                           (first args) [])
        children (if (vector? (first args))
                   (next args) args)]
    `(Suite ~(vec (map (comp reformat-context second) (partition 2 context-bindings)))
            ~(vec (map (partial reformat-child context-bindings) children))
            {:doc ~docstring} {})))

(defmacro defsuite [name & args]
  `(def ~name (let [s# (make-suite ~@args)]
                (with-meta s# (assoc (meta s#)
                                :name '~name
                                :ns *ns*
                                :file *file*
                                :line @clojure.lang.Compiler/LINE)))))


;;; RAW API USAGE:

(def c1 (Context (fn [] 1) nil))

(def c2 (Context (fn [] (Thread/sleep 1000) 2) nil))

(def t1 (Test '[a b] [c1 c2] '[(integer? a) (integer? b)]))

(def t2 (Test '[a b] [c1 c2] '[(> b a) (< a b)]))

;; Including context c2 in the Suite means it's only executed once for
;; all tests in the suite.
(def s1 (Suite [c2] [t1 t2]))

;; Suites may be nested.
(def s2 (Suite nil [s1]))

;; user=> (run-suite s2)
;; ...
;; user=> (success? *1)
;; true


;;; DEFSUITE USAGE

(defsuite s3 [a c1 b 2]
  t1 t2
  (:test [b 2] (> b a)) 
  (:suite [c 3]
          (:test (integer? c))
          (:test (< a c))))
