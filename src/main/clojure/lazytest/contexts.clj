(ns lazytest.contexts
  (:use [lazytest.arguments
         :only (nil-or)]))

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
     {:pre [(nil-or vector? parents)
            (every? context? parents)
            (nil-or fn? before)
            (nil-or fn? after)
            (nil-or map? metadata)]
      :post [(context? %)]}
     (Context. parents before after metadata nil)))

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
