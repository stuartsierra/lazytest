;; I want to use deftype, but literal deftype'd objects cannot be
;; 9embedded in generated code.

(defn TestCase [contexts locals children]
  (with-meta {:contexts contexts
              :locals locals
              :children children}
    {:type ::TestCase}))

(defn Context [before after]
  (with-meta {:before before
              :after after}
    {:type ::Context}))

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
  `(try (if ~a
          (AssertionPassed '~a)
          (AssertionFailed '~a))
        (catch Throwable t#
          (AssertionThrown '~a t#))))

(defn format-tests [t]
  (let [active (gensym "active")]
    `(fn [~active ~@(:locals t) & states#]
       (lazy-seq
        ~(loop [r nil
                children (reverse (:children t))]
           (if (seq children)
             (let [c (first children)]
               (recur (if (= ::TestCase (type c))
                        `(cons (run-tests '~c ~active) ~r)
                        `(cons ~(format-assertion c) ~r))
                      (next children)))
             r))))))

(defn compile-tests [t]
  (eval (format-tests t)))

(defn unchunked-map [f coll]
  (lazy-seq
   (when (seq coll)
     (cons (f (first coll))
           (unchunked-map f (next coll))))))

(defn run-tests [t active]
  (let [contexts (:contexts t)
        states (map (fn [c] (or (active c) ((:before c)))) contexts)
        ;;_ (prn "States are " states)
        merged (merge active (zipmap contexts states))
        ;;_ (prn "Merged is " merged)
        results (apply (compile-tests t) merged states)
        closed (map (fn [c state]
                      (when-not (contains? active c) ((:after c))))
                    (reverse contexts) (reverse states))]
    (TestResult t results)))

(defn doall-results [tr]
  (doall (tree-seq :children :children tr)))

(def c1 (Context (fn [] (prn "Opening lazy context c1.") 1) nil))

(def c2 (Context (fn [] (prn "Opening lazy context c2.") 2.5) nil))

(def t1 (TestCase [c1 c2] '[a b]
                  '[(do (prn "Evaluating test t1-a") (integer? a))
                    (do (prn "Evaluating test t1-b") (integer? b))]))

(def t2 (TestCase [c1 c2] '[a b]
                  '[(do (prn "Evaluating test t2-a") (integer? a))
                    (do (prn "Evaluating test t2-b") (integer? b))]))

(def s1 (TestCase [] [] [t1 t2]))

(def s2 (TestCase [c1 c2] [] [t1 t2]))

(def s3 (TestCase [] [] [t1 t2 t1 t2 t1 t2]))
