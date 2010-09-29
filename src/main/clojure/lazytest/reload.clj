(ns lazytest.reload
  (:require clojure.set))

(defn remove-from-loaded-libs
  "Removes symbols from the set in the private Ref clojure.core/*loaded-libs*"
  [& syms]
  (dosync
   (alter @#'clojure.core/*loaded-libs*
	  clojure.set/difference (set syms))))

(defn reload
  "Removes all namespaces named by symbols, then reloads them."
  [& symbols]
  {:pre (every? symbol? symbols)}
  (doseq [sym symbols] (remove-ns sym))
  (apply remove-from-loaded-libs symbols)
  (apply require symbols))
