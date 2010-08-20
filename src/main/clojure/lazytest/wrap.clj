(ns lazytest.wrap)

(defn before
  "Set f as a 'before' function in x's metadata."
  [x f]
  {:pre [(fn? f)]}
  (vary-meta x assoc ::before f))

(defn do-before
  "Execute any 'before' function associated with x."
  [x]
  (when-let [f (::before (meta x))]
    (f)))

(defn after
  "Set f as a 'after' function in x's metadata."
  [x f]
  {:pre [(fn? f)]}
  (vary-meta x assoc ::after f))

(defn do-after
  "Execute any 'after' function associated with x."
  [x]
  (when-let [f (::after (meta x))]
    (f)))

