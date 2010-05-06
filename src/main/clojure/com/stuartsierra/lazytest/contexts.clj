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

(let [c (context [] 1)]
  (assert (fn? (:before c)))
  (assert (= 1 ((:before c))))
  (assert (nil? (:after c))))
