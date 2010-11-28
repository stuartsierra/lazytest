(ns lazytest.focus)

(defn focused?
  "True if x has focus metadata set to true."
  [x]
  (boolean (:focus (meta x))))

(defn filter-focused
  "If any items in sequence s are focused, return them, with focus
  metadata added to the sequence; else return s unchanged."
  [s]
  (if-let [fs (seq (filter focused? s))]
    (with-meta fs (assoc (meta s) :focus true))
    s))

(defn filter-tree
  "If any item or sequence in the tree rooted at s has focus metadata
  set to true, returns just the focused items while preserving their
  position in the tree.  Otherwise returns s unchanged."
  [s]
  (if (and (seq? s) (not (focused? s)))
    (filter-focused
     (with-meta (map filter-tree s)
       (meta s)))
    s))
