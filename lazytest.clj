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
  (success? [r] "Returns true if the TestResult or 
  SuiteResult was 100% successful."))

(defprotocol TestCompilable
  (compile [x active-contexts] "Compiles the Test or Suite to a fn."))

(defprotocol TestRunnable
  (run [x] "Executes the Test or Suite."))

(deftype Test [locals contexts assertions])

(deftype Context [before after])

(deftype Suite [contexts children])

(deftype AssertionSuccess [form]
  Successful (success? [] true))

(deftype AssertionFailure [form]
  Successful (success? [] false))

(deftype AssertionThrown [form throwable]
  Successful (success? [] false))

(deftype TestResult [test assertion-results]
  Successful (success? [] (every? success? assertion-results)))

(deftype SuiteResult [suite child-results]
  Successful (success? [] (every? success? child-results)))

(defmulti report-start type)

(defmulti report-done type)

(defmethod report-start TestResult [tr]
  (println "Starting test" (:name (meta (:test tr)))))

(defmethod report-done TestResult [tr]
  (println "Finished test" (:name (meta (:test tr)))))

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
          (AssertionSuccess '~expr)
          (AssertionFailure '~expr))
        (catch Throwable t#
          (AssertionThrown '~expr t#))))

(defn- format-test [t]
  {:pre [(test? t)
         (vector? (:locals t))
         (every? symbol? (:locals t))]}
  `(fn ~(:locals t)
     (lazy-seq
      ~(loop [r nil, as (reverse (:assertions t))]
         (if (seq as)
           (recur (list 'cons (format-assertion (first as)) r)
                  (next as))
           r)))))

(defn- compile-test
  "Compile a Test to a function of its locals."
  [t]
  (or (::compiled (meta t))
      (eval (format-test t))))

(defn- open-context 
  "Calls Context c's :before function, returns result."
  [active-contexts c]
  (or (active-contexts c)
      (when-let [f (:before c)]
        (f))))

(defn- close-context
  "Calls Context c's :after function on state."
  [active-contexts c state]
  (when-not (contains? active-contexts c)
    (when-let [f (:after c)]
      (f state))))

(defn- call-with-context
  "Opens all contexts, applies f to their state, closes all contexts
  in reverse order, and returns f's return value.  Executes strictly
  if any contexts has an :after function; otherwise executes lazily."
  [active-contexts f contexts]
  {:pre [(every? context? contexts) (fn? f)]}
  (if (some :after contexts)
    ;; strict (non-lazy)
    (let [states (doall (map (partial open-context active-contexts)
                             contexts))
          result (doall (apply f states))]
      (doall (map (partial close-context active-contexts)
                  (reverse contexts) (reverse states)))
      result)
    ;; lazy
    (apply f (map open-context contexts))))

(defn- compile-suite [active-contexts s]
  (or (::compiled (meta s))
      (fn [& _] (map (partial active-contexts run)
                     (:children s)))))

(extend-protocol TestCompilable
  ::Test
  (run [active-contexts t]
       (TestResult t (call-with-context
                        active-contexts
                        (compile-test s) (:contexts t))))
  ::Suite
  (run [active-contexts s]
       (SuiteResult s (call-with-context
                        active-contexts
                        (compile-suite s) (:contexts s)))))

(defn run-with-active-context [active-contexts x]
  )


;;; RAW API USAGE:

(def c1 (Context (fn [] (prn "Opening context c1.") 1)
                 (fn [_] (prn "Closing context c1.") 1)))

(def c2 (Context (fn [] (prn "Opening context c2.") 2)
                 (fn [_] (prn "Closing context c2."))))

(def c3 (Context (fn [] (prn "Opening lazy context c3.") 3)
                 nil))

(def c4 (Context (fn [] (prn "Opening lazy context c4.") 4)
                 nil))

(def c5 (Context (fn [] (prn "Opening lazy context c5.") 3)
                 nil))

(def c6 (Context (fn [] (prn "Opening lazy context c6.") 4)
                 nil))

(def t1 (Test '[a b] [c1 c2]
              '[(do (prn "Evaluating test t1-a.") (integer? a))
                (do (prn "Evaluating test t1-b.") (integer? b))]))

(def t2 (Test '[a b] [c1 c2]
              '[(do (prn "Evaluating test t2-a.") (integer? a))
                (do (prn "Evaluating test t2-b.") (integer? b))]))

(def t3 (Test '[a b] [c3 c4]
              '[(do (prn "Evaluating test t3-a.") (integer? a))
                (do (prn "Evaluating test t3-b.") (integer? b))]))

(def t4 (Test '[a b] [c5 c6]
              '[(do (prn "Evaluating test t4-a.") (integer? a))
                (do (prn "Evaluating test t4-b.") (integer? b))]))

(def s1 (Suite [] [t1 t2]))

;; Including context c2 in the Suite means it's only executed once for
;; all tests in the suite.
(def s2 (Suite [c1] [t1 t2]))

(def s3 (Suite [c1 c2] [t1 t2]))

;; Tests and Suites are evaluated lazily if and only if none of their
;; Contexts has an :after function.
(def s4 (Suite [] [t3 t4]))

(def s5 (Suite [c3 c4] [t3 t4]))

;; Suites may be nested.

;; user=> (run s2)
;; ...
;; user=> (success? *1)
;; true
