(ns lazytest.attach)

(defn groups
  "Returns the Groups for namespace n"
  [n]
  (map var-get (filter #(::group (meta %)) (vals (ns-interns n)))))

(defn all-groups
  "Returns a sequence of all Groups in all namespaces."
  []
  (mapcat groups (all-ns)))

(defn add-group
  "Adds Group g to namespace n."
  [n g]
  (intern n (with-meta (gensym "lazytest-group-") {::group true}) g))

(defn clear-groups
  "Removes all test groups from namespace n."
  [n]
  (doseq [[sym v] (ns-interns n)]
    (when (::group (meta v))
      (ns-unmap n sym))))
