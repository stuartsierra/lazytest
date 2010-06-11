(ns com.stuartsierra.lazytest.attach
  (:use [com.stuartsierra.lazytest.groups :only (group?)]))

(defn groups-var
  "Creates or returns the Var storing Groups in namespace n."
  [n]
  (or (ns-resolve n '*lazytest-groups*)
      (intern n '*lazytest-groups* #{})))

(defn groups
  "Returns the Groups for namespace n"
  [n]
  (var-get (groups-var n)))

(defn add-group
  "Adds Group g to namespace n."
  [n g]
  {:pre [(group? g)
	 (the-ns n)]}
  {:post [(some #{g} (seq (groups n)))]}
  (alter-var-root (groups-var (the-ns n)) conj g))

