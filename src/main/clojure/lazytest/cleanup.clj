(ns lazytest.cleanup)

(defn with-cleanup
  "Set f as a cleanup function in x's metadata."
  [x f]
  {:pre [(fn? f)]}
  (vary-meta x assoc ::cleanup f))

(defn do-cleanup
  "Execute all cleanup functions associated with x."
  [x]
  (when-let [f (::cleanup (meta x))]
    (f)))
