;;; TEST2: a new testing framework for Clojure

;; by Stuart Sierra, http://stuartsierra.com/
;; February 19, 2010

;; Copyright (C) 2010 Stuart Sierra.

;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0
;; <http://opensource.org/licenses/eclipse-1.0.php>.  By using this
;; software in any fashion, you are agreeing to be bound by the terms
;; of this license.  You must not remove this notice, or any other,
;; from this software.


;; Second draft: Lazy results


(ns clojure.test2)

(defprotocol Successful
  (success? [r] "Returns true if the TestResult or 
  SuiteResult was 100% successful."))

(defprotocol TestRunnable
  (run-tests [x actives]
   "Executes the Test or Suite with a map of active contexts."))

(deftype Test [locals contexts assertions])

(deftype Context [before after])

(deftype Suite [contexts children])

(deftype AssertionSuccess [form]
  Successful (success? [] true))

(deftype AssertionFailure [form]
  Successful (success? [] false))

(deftype AssertionThrown [form throwable]
  Successful (success? [] false))

(deftype TestResult [test child-results]
  Successful (success? [] (every? success? child-results)))

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
  [actives c]
  (or (actives c)
      (when-let [f (:before c)]
        (f))))

(defn- close-context
  "Calls Context c's :after function on state."
  [actives c state]
  (when-not (contains? actives c)
    (when-let [f (:after c)]
      (f state))))

(defn- unchunked-map
  "Purely lazy map that processes chunked sequences one at a time."
  [f coll]
  (lazy-seq
   (when (seq coll)
     (cons (f (first coll))
           (unchunked-map f (next coll))))))

(extend-protocol TestRunnable
  ::Test
  (run-tests [t actives]
    (let [f (compile-test t)]
      (if (some :after (:contexts t))
        ;; strict (non-lazy)
        (let [states (doall (map (partial open-context actives) (:contexts t)))
              result (doall (apply f states))]
          (doall (map (partial close-context actives)
                      (reverse (:contexts t)) (reverse states)))
          (TestResult t result))
        ;; lazy
        (let [states (map (partial open-context actives) (:contexts t))]
          (TestResult t (apply f states))))))
  ::Suite
  (run-tests [s actives]
    (if (some :after (:contexts s))
      ;; strict (non-lazy)
      (let [states (doall (map (partial open-context actives) (:contexts s)))
            new-actives (merge actives (zipmap (:contexts s) states))
            result (doall (map #(run-tests % new-actives) (:children s)))]
        (doall (map (partial close-context actives)
                    (reverse (:contexts s)) (reverse states)))
        (SuiteResult s result))
      ;; lazy
      (let [states (map (partial open-context actives) (:contexts s))
            new-actives (merge actives (zipmap (:contexts s) states))]
        (SuiteResult s (unchunked-map #(run-tests % new-actives) (:children s)))))))


(defn run "Executes the Test or Suite."
  [x] (run-tests x {}))

(derive ::TestResult ::CollectedResult)
(derive ::SuiteResult ::CollectedResult)

(defn doall-results
  "Forces all lazy test results to be evaluated."
  [x]
  (cond (isa? (type x) ::CollectedResult)
          (dorun (map doall-results (:child-results x)))
        (seq? x)
          (dorun x)
        :else x)
  x)

;;; RAW API USAGE:

(def c1 (Context (fn [] (prn "Opening context c1.") 1)
                 (fn [_] (prn "Closing context c1."))))

(def c2 (Context (fn [] (prn "Opening context c2.") 2.5)
                 (fn [_] (prn "Closing context c2."))))

(def c3 (Context (fn [] (prn "Opening lazy context c3.") 3)
                 nil))

(def c4 (Context (fn [] (prn "Opening lazy context c4.") 4.5)
                 nil))

(def c5 (Context (fn [] (prn "Opening lazy context c5.") 3)
                 nil))

(def c6 (Context (fn [] (prn "Opening lazy context c6.") 4.5)
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

(def t4 (Test '[a b] [c3 c4]
              '[(do (prn "Evaluating test t4-a.") (integer? a))
                (do (prn "Evaluating test t4-b.") (integer? b))]))

(def t5 (Test '[a b] [c5 c6]
              '[(do (prn "Evaluating test t5-a.") (integer? a))
                (do (prn "Evaluating test t5-b.") (integer? b))]))

(def s1 (Suite [] [t1 t2]))

;; Attaching a Context to a Suite ensures that Context is evaluated
;; only once for the entire Suite.
(def s2 (Suite [c1] [t1 t2]))

(def s3 (Suite [c1 c2] [t1 t2]))

;; Tests and Suites are evaluated lazily if and only if none of their
;; Contexts has an :after function.
(def s4 (Suite [] [t3 t4]))

(def s5 (Suite [c3 c4] [t3 t4]))

(def s6 (Suite [c3 c4] [t4 t5]))

;; Suites may be nested.
(def s7 (Suite [] [s3 s5]))

(def s8 (Suite [c1 c2 c3 c4] [s3 s5]))

;; user=> (run s2)
;; ...
;; user=> (success? *1)
;; true
