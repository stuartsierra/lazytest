(ns com.stuartsierra.lazytest.contexts
  (:use [com.stuartsierra.lazytest.arguments
         :only (get-arg get-options or-nil standard-metadata
                        firsts seconds)]))

(defrecord Context [parents before after])

(defn context?
  "True if x is a Context object."
  [x]
  (isa? (type x) Context))

(defn new-context
  "Creates a new Context object.  parents is a vector of parent
  contexts.  before and after are functions."
  ([parents before after]
     (new-context parents before after nil))
  ([parents before after metadata]
     {:pre [(or-nil vector? parents)
            (every? context? parents)
            (or-nil fn? before)
            (or-nil fn? after)
            (or-nil map? metadata)]
      :post [(context? %)]}
     (Context. parents before after nil metadata)))

(defmacro context [bindings & bodies]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  (let [locals (firsts bindings)
        contexts (seconds bindings)
        [before-body divider after-body] (partition-by #(= :after %) bodies)
        before-fn `(fn ~locals ~@before-body)
        after-fn (when after-body
                   (let [[state & after-body] after-body]
                     (assert (symbol? state))
                     `(fn ~(apply vector state locals) ~@after-body)))]
    `(new-context ~contexts ~before-fn ~after-fn)))

(defmacro defcontext
  [name & decl]
  (let [[docstring decl] (get-arg string? decl)
        [options decl] (get-options decl)
        bindings (:bindings options [])]
    `(def ~name (with-meta (context ~bindings ~@decl)
                  (standard-metadata &form docstring name)))))


(defn open-context
  "Opens context c, and all its parents, unless it is already active."
  [active c]
  (let [active (reduce open-context active (:parents c))
        states (map active (:parents c))]
    (if-let [f (:before c)]
      (assoc active c (or (active c) (apply f states)))
      active)))

(defn close-context
  "Closes context c, and all its parents, and removes it from active."
  [active c]
  (let [states (map active (:parents c))]
    (when-let [f (:after c)]
      (apply f (active c) states))
    (let [active (reduce close-context active (:parents c))]
      (dissoc active c))))



;;; Assertions

(let [c (context [] (+ 1 1))]
  (assert (fn? (:before c)))
  (assert (= 2 ((:before c))))
  (assert (nil? (:after c))))

(let [c1 (context [] (+ 1 0)
                 :after x
                 (assert (= x 1)))]
  (assert (fn? (:before c1)))
  (assert (fn? (:after c1)))
  (assert (= {c1 1} (open-context {} c1)))
  (assert (= {} (close-context {c1 1} c1)))

  (let [c2 (context [x c1] (+ x 1)
                    :after y
                    (assert (= y 2)))]
    (assert (fn? (:before c2)))
    (assert (fn? (:after c2)))
    (assert (= {c1 1 c2 2} (open-context {} c2)))
    (assert (= {} (close-context {c1 1 c2 2} c2)))))