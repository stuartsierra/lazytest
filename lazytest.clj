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

(defprotocol Successful
  (success? [r] "Returns true if the result r was 100% successful."))

(deftype Assertion [form])

(deftype Test [locals contexts assertions])

(deftype Context [before after])

(deftype Suite [contexts tests])

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

(defn run-suite
  "Run a Suite s, return a SuiteResult."
  [s]
  (SuiteResult s (call-with-contexts (:contexts s)
                   (fn [& _]
                     (doall (map run-test (:tests s)))))))


;;; USAGE:

(def c1 (Context (fn [] 1) nil))

(def c2 (Context (fn [] 2) nil))

(def t1 (Test '[a b] [c1 c2] '[(integer? a) (integer? b)]))

(def t2 (Test '[a b] [c1 c2] '[(> b a) (< a b)]))

(def s1 (Suite [c1] [t1 t2]))

;; user=> (run-suite s1)
;; ...
;; user=> (success? *1)
;; true


;;; STILL TO DO:

;; * Come up with convenient macro syntax.
