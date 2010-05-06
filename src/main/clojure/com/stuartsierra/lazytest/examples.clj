(ns com.stuartsierra.lazytest.examples
  (:use [com.stuartsierra.lazytest.arguments
         :only (or-nil standard-metadata)]
        [com.stuartsierra.lazytest.contexts
         :only (context?)]))

(defrecord Group [contexts examples])

(defn group?
  "True if x is an example group."
  [x]
  (isa? (type x) Group))

(defn new-group
  "Creates a Group."
  ([contexts examples subgroups metadata]
     {:pre [(or-nil vector? contexts)
            (or-nil vector? examples)
            (or-nil vector? subgroups)
            (every? context? contexts)
            (every? fn? examples)
            (every? group? subgroups)
            (or-nil map? metadata)]
      :post [(group? %)]}
     (Group. contexts examples nil metadata)))

(defmacro with-context
  "Establishes a local binding of sym to the state returned by context
  c in all groups and examples found within body."
  [sym c & body]
  `(let [~(with-meta sym {::local true}) ~c]
     ~@body))

(defn find-locals
  "Returns a vector of locals bound by with-context in the
  environment."
  [env]
  (vec (filter #(::local (meta %)) (keys env))))

(defmacro example
  ([expr] `(example nil ~expr))
  ([docstring expr]
     `(vary-meta (fn ~(find-locals &env) ~expr)
                 merge ~(standard-metadata &form docstring))))

(defmacro group-examples [& examples]
  (loop [result [], exs examples]
    (if (seq exs)
      (if (string? (first exs))
        (recur (conj result `(example ~(first exs) ~(second exs)))
               (nnext exs))
        (recur (conj result `(example ~(first exs)))
               (next exs)))
      result)))

(defmacro group [bindings examples subgroups]
  {:pre [(or-nil vector? bindings)
         (even? (count bindings))]}
  (if (seq bindings)
    `(with-context ~(first bindings) ~(second bindings)
       (group ~(nnext bindings) ~examples ~subgroups))
    `(new-group ~(find-locals &env)
                (group-examples ~@examples)
                ~subgroups
                ~(standard-metadata &form nil))))


;;; Assertions

;; example
(let [e (example "hello" (= 1 1))]
  (assert (fn? e))
  (assert (true? (e)))
  (let [m (meta e)]
    (assert (= "hello" (:doc m)))
    (assert (= *ns* (:ns m)))
    (assert (string? (:file m)))
    (assert (integer? (:line m)))
    (assert (nil? (:name m)))))

(let [c1 (com.stuartsierra.lazytest.contexts/context [] 1)
      e (with-context x c1
          (example "foo" (= x 1)))]
  (assert (fn? e))
  (assert (true? (e 1)))
  (let [m (meta e)]
    (assert (= "foo" (:doc m)))
    (assert (= *ns* (:ns m)))
    (assert (string? (:file m)))
    (assert (integer? (:line m)))
    (assert (nil? (:name m)))))