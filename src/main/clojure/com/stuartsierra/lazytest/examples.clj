(ns com.stuartsierra.lazytest.examples
  (:use [com.stuartsierra.lazytest.arguments
         :only (or-nil standard-metadata)]
        [com.stuartsierra.lazytest.contexts
         :only (context?)]))

(defrecord Group [contexts examples])

(defn group? [x]
  (isa? (type x Group)))

(defn new-group
  "Creates a Group."
  ([contexts examples] (new-group contexts examples nil))
  ([contexts examples metadata]
     {:pre [(or-nil vector? contexts)
            (or-nil vector? examples)
            (every? context? contexts)
            (every? fn? examples)
            (or-nil map? metadata)]
      :post [(group? %)]}
     (Group. contexts examples nil metadata)))

(defmacro with-context [sym cntxt & body]
  `(let [~(with-meta sym {::local true}) ~cntxt]
     ~@body))

(defn find-locals [env]
  (vec (filter #(::local (meta %)) (keys env))))

(defmacro example
  ([expr] `(example nil ~expr))
  ([docstring expr]
     `(vary-meta (fn ~(find-locals &env) ~expr)
                 merge ~(standard-metadata &form docstring))))


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