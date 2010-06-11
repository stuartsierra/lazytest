(ns com.stuartsierra.lazytest.groups
  (:use [com.stuartsierra.lazytest.arguments
	 :only (or-nil)]
	[com.stuartsierra.lazytest.contexts
         :only (context?)]))

(defrecord Group [contexts examples subgroups])

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
     (Group. contexts examples subgroups nil metadata)))
