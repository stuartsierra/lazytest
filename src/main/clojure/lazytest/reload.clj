(ns lazytest.reload)

(defn reload
  "Removes all namespaces named by symbols, then reloads them all."
  [& symbols]
  (doseq [sym symbols]
    (remove-ns sym))
  (doseq [sym symbols]
    (require sym :reload)))
