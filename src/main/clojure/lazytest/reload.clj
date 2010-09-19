(ns lazytest.reload)

(defn reload
  "Removes all namespaces named by symbols, then reloads those
  namespaces and all their dependencies."
  [& symbols]
  {:pre (every? symbol? symbols)}
  (doseq [sym symbols] (remove-ns sym))
  (apply require :reload-all symbols))
