(ns com.stuartsierra.lazytest.contexts
  (:use [com.stuartsierra.lazytest.arguments
         :only (or-nil)]))

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
