(ns lazytest.reload)

(defn reload
  "Removes all namespaces named by symbols, then reloads them all."
  [& symbols]
  {:pre (every? symbol? symbols)}
  (doseq [sym symbols]
    (remove-ns sym))
  (apply require :reload symbols))
