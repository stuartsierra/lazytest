(deftype Context [before after])

(deftype Assertion [locals form])

(deftype TestCase [contexts children])

(defprotocol TestSuccess
  (success? [r] "Returns true if r is a 100% successful result."))

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

(defmacro assertion [locals form]
  (format-assertion (Assertion locals form)))

(defn- unchunked-map
  "Like map but does not chunk results; slower but lazier than map."
  [f coll]
  (lazy-seq
   (when (seq coll)
     (cons (f (first coll))
           (unchunked-map f (next coll))))))

(defn run-test-case
  ([t] (run-test-case t {}))
  ([t active]
     {:pre [(= ::TestCase (type t))
            (every? #(= ::Context (type %)) (:contexts t))]}
     (try
      (let [contexts (:contexts t)
            states (map (fn [c] (when-let [b (:before c)]
                                  (or (active c) (b)))) contexts)
            merged (merge active (zipmap contexts states))
            ;; Prevent chunking for truly lazy execution:
            results (unchunked-map (fn [c] (if (fn? c)
                                             (apply c states)
                                             (run-test-case c merged)))
                                   (:children t))]
        ;; Force non-lazy execution to handle shutdown properly:
        (when (some :after contexts)
          (dorun results)
          (dorun (map (fn [c state] (when-not (active c) ((:after c) state)))
                      (reverse contexts) (reverse states))))
        (TestResult t results))
      (catch Throwable e (TestThrown t e)))))

(defn result-seq [r]
  (tree-seq :children :children r))

(defprotocol Testable
  (run-tests [x]))

(defn var-test-case [v]
   #^{:name (:name (meta v))}
   (TestCase [] [(assertion [] (clojure.core/test v))]))

(defn test-var [v]
  (run-test-case (var-test-case v)))

(defn ns-test-case [n]
  #^{:name (ns-name n)}
  (TestCase [] (map var-test-case (ns-interns n))))

(defn test-ns [n]
  (run-test-case (ns-test-case n)))

(extend ::TestCase Testable {:run-tests run-test-case})
(extend clojure.lang.Var Testable {:run-tests test-var})
(extend clojure.lang.Namespace Testable {:run-tests test-ns})

(defmacro defcontext
  "decl => docstring? before-forms* :after [arg] after-forms*"
  [name & decl]
  (let [docstring (if (string? (first decl)) (first decl) nil)
        decl      (if (string? (first decl)) (next decl) decl)
        before    (take-while #(not= :after %) decl)
        decl      (next (drop-while #(not= :after %) decl))
        argv      (first decl)
        after     (next decl)]
    `(def ~name
          (with-meta (Context ~(when (seq before) `(fn [] ~@before))
                              ~(when (seq after) `(fn ~argv ~@after)))
            {:name '~name, :doc ~docstring}))))

;;; USAGE

(def c1 (Context (fn [] (println "Opening lazy context c1.") 1) nil))

(def c2 (Context (fn [] (println "Opening lazy context c2.") 2) nil))

(def c3 (Context (fn [] (println "Opening strict context c3.") 3)
                 (fn [x] (println "Closing strict context c3." x))))

(def c4 (Context (fn [] (println "Opening strict context c4.") 4)
                 (fn [x] (println "Closing strict context c4." x))))

(def c5 (Context nil nil))

(def c6 (Context (fn [] (throw (ArithmeticException.))) nil))

(def c7 (Context nil (fn [x] (throw (ArithmeticException.)))))

(def a1 (assertion [a] (do (println "Executing assertion a1.")
                           (pos? a))))

(def a2 (assertion [a] (do (println "Executing assertion a2.")
                           (pos? a))))

(def a3 (assertion [a] (do (println "Executing assertion a3.")
                           (pos? a))))

(def a4 (assertion [a b] (do (println "Executing assertion a4.")
                             (< a b))))

(def a5 (assertion [a b] (do (println "Executing assertion a5.")
                             (= a b))))

(def t1 (TestCase [c1] [a1]))

(def t2 (TestCase [c1] [a1 a2 a3]))

(def t3 (TestCase [] [t1 t2]))

(def t4 (TestCase [c1] [t1 t2]))

(def t5 (TestCase [c1 c2] [a4 a5]))

(def t6 (TestCase [] [t5 t4]))

(def t7 (TestCase [c3] [a1 a2 a3]))

(def t8 (TestCase [c3 c4] [a4 a5]))

(def t9 (TestCase [] [t8 t8]))

(def t10 (TestCase [c5] [a1 a2]))

(def t11 (TestCase [c6] [a1 a2]))

(def t12 (TestCase [c7] [a1 a2]))
