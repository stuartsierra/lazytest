(ns lazytest.groups
  (:use [lazytest.arguments
	 :only (nil-or)]
	[lazytest.contexts
         :only (context?)]))

(defrecord Group [contexts examples subgroups])

(defn group?
  "True if x is an example group."
  [x]
  (isa? (type x) Group))

(defn new-group
  "Creates a Group."
  ([contexts examples subgroups metadata]
     {:pre [(nil-or vector? contexts)
            (nil-or vector? examples)
            (nil-or vector? subgroups)
            (every? context? contexts)
            (every? fn? examples)
            (every? group? subgroups)
            (nil-or map? metadata)]
      :post [(group? %)]}
     (Group. contexts examples subgroups metadata nil)))
