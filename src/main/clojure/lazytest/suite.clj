(ns lazytest.suite)

(defn suite [f]
  {:pre [(fn? f)]}
  (vary-meta f assoc ::suite true))

(defn suite? [x]
  (and (fn? x) (::suite (meta x))))
