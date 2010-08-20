(ns lazytest.focus)

(defn focused?
  "True if x has focus metadata set to true."
  [x]
  (boolean (::focus (meta x))))

(defn filter-focused
  "If any items in sequence s are focused, return them; else return s."
  [s]
  (or (seq (filter focused? s)) s))

(defn focus
  "Adds focus metadata to x."
  [x]
  (vary-meta x assoc ::focus true))
