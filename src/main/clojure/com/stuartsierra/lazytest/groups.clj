(ns com.stuartsierra.lazytest.groups
  (:use [com.stuartsierra.lazytest.arguments
         :only (or-nil standard-metadata)]
        [com.stuartsierra.lazytest.contexts
         :only (context? with-context find-locals)]))

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

(defmacro example
  "Creates an example function, using current context locals for
  arguments, with expr as its body."
  ([expr] `(example nil ~expr))
  ([docstring expr]
     (let [locals (find-locals &env)]
       `(vary-meta (fn ~locals ~expr)
                   merge ~(standard-metadata expr docstring)
                   {:locals '~locals}))))

(defmacro group-examples
  "Creates a vector of example functions from body.  A string in body
  will be attached as a doc string on the following example."
  [& body]
  (loop [result [], forms body]
    (if (seq forms)
      (if (string? (first forms))
        (recur (conj result `(example ~(first forms) ~(second forms)))
               (nnext forms))
        (recur (conj result `(example ~(first forms)))
               (next forms)))
      result)))

(defmacro group
  "Creates an example group.  

  bindings is a vector of symbol-context pairs, symbols will become
  locals in the examples.

  examples is a vector of compiled example functions.

  subgroups is a vector of child example groups."
  [bindings examples subgroups]
  {:pre [(or-nil vector? bindings)
         (even? (count bindings))]}
  (if (seq bindings)
    `(with-context ~(first bindings) ~(second bindings)
       (group ~(vec (nnext bindings)) ~examples ~subgroups))
    `(new-group ~(find-locals &env)
                ~examples
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
    (assert (nil? (:name m)))
    (assert (= [] (:locals m)))))

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
    (assert (nil? (:name m)))
    (assert (= '[x] (:locals m)))))

(let [c1 (com.stuartsierra.lazytest.contexts/context [] 1)
      g (group [x c1] (group-examples (= 1 x) "foo" (= x 2)) [])]
  (assert (group? g))
  (assert (= [c1] (:contexts g)))
  (let [exs (:examples g)]
    (assert (vector? exs))
    (assert (= 2 (count exs)))
    (assert (every? fn? exs))
    (let [[ex1 ex2] exs]
      (assert (true? (ex1 1)))
      (assert (false? (ex2 1)))
      (let [m (meta ex1)]
        (assert (nil? (:doc m)))
        (assert (= *ns* (:ns m)))
        (assert (string? (:file m)))
        (assert (integer? (:line m)))
        (assert (nil? (:name m)))
        (assert (= '[x] (:locals m))))
      (let [m (meta ex2)]
        (assert (= "foo" (:doc m)))
        (assert (= *ns* (:ns m)))
        (assert (string? (:file m)))
        (assert (integer? (:line m)))
        (assert (nil? (:name m)))
        (assert (= '[x] (:locals m)))))))

(let [c1 (com.stuartsierra.lazytest.contexts/context [] 1)
      c2 (com.stuartsierra.lazytest.contexts/context [] 2)
      g (group [x c1 y c2] (group-examples (= 1 x) "foo" (= y 2)) [])]
  (assert (group? g))
  (assert (= [c1 c2] (:contexts g)))
  (let [exs (:examples g)]
    (assert (vector? exs))
    (assert (= 2 (count exs)))
    (assert (every? fn? exs))
    (let [[ex1 ex2] exs]
      (assert (true? (ex1 1 2)))
      (assert (true? (ex2 1 2)))
      (let [m (meta ex1)]
        (assert (nil? (:doc m)))
        (assert (= *ns* (:ns m)))
        (assert (string? (:file m)))
        (assert (integer? (:line m)))
        (assert (nil? (:name m)))
        (assert (= '[x y] (:locals m))))
      (let [m (meta ex2)]
        (assert (= "foo" (:doc m)))
        (assert (= *ns* (:ns m)))
        (assert (string? (:file m)))
        (assert (integer? (:line m)))
        (assert (nil? (:name m)))
        (assert (= '[x y] (:locals m)))))))
