(deftype Context [before after])

(deftype Assertion [locals form])

(deftype TestCase [contexts children])

(defprotocol TestCompilable
  (test-compile [x]))

(defprotocol TestSuccess
  (success? [r] "Returns true if r is a 100% successful result."))

(deftype TestResult [source children]
  TestSuccess (success? [] (every? success? children)))

(deftype AssertionPassed [source]
  TestSuccess (success? [] true))

(deftype AssertionFailed [source]
  TestSuccess (success? [] false))

(deftype AssertionThrown [source error]
  TestSuccess (success? [] false))

(defn format-assertion [a]
  {:pre [(= ::Assertion (type a))
         (vector? (:locals a))]}
  (let [form (:form a)]
    `(fn ~(vec (cons (gensym) (:locals a)))
       (try (if ~form
              (AssertionPassed '~form)
              (AssertionFailed '~form))
            (catch Throwable t#
              (AssertionThrown '~form t#))))))

(defn unchunked-map [f coll]
  (lazy-seq
   (when (seq coll)
     (cons (f (first coll))
           (unchunked-map f (next coll))))))

(def mtest-compile (memoize test-compile))

(extend-protocol TestCompilable
 ::Assertion
 (test-compile [a] (eval (format-assertion a)))
 ::Context
 (test-compile [c] (fn ([] (when-let [f (:before c)] (f)))
                     ([state] (when-let [f (:after c)] (f state)))))
 ::TestCase
 (test-compile [t]
  (let [cfns (map mtest-compile (:contexts t))
        tfns (map mtest-compile (:children t))
        open (fn [active] (map (fn [f]
                                 (prn "Trying to open cfn" f)
                                 (or (active f) (f))) cfns))
        close (fn [active states]
                  (map (fn [c state]
                         (when-not (contains? active c) (c state)))
                       (reverse cfns) (reverse states)))]
    (fn [active & states]
      (prn "Active is" active)
       (let [states (open active)
             merged (merge active (zipmap cfns states))
             _ (prn "Merged is" merged)
             results (unchunked-map #(apply % merged states) tfns)]
         (close active states)
         (TestResult (str t) results))))))

(defn results-seq [r]
  (tree-seq :children :children r))

(def c1 (Context (fn [] (prn "Opening lazy context c1.") 1) nil))

(def a1 (Assertion '[a] '(do (prn "Executing assertion a1.")
                             (pos? a))))

(def t1 (TestCase [c1] [a1]))

(def t2 (TestCase [c1] [a1 a1 a1]))

(def t3 (TestCase [c1] [t1 t1 t1]))