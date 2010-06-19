(ns lazytest.plan
  (:use [lazytest.arguments :only (nil-or)]))

(defn has-tag?
  "True if object x has tag t in its metadata :tags."
  [x t]
  {:pre [(nil-or set? (:tags (meta x)))
	 (keyword? t)]}
  (contains? (:tags (meta x)) t))

(defn filter-examples
  "Returns a flat sequence of RunnableExamples for all examples in Groups gs.
  Examples are filtered by the following options:

     :tags [tags]
        Every example must have at least one of the tags.
     :exclude [tags]
        Every example must NOT have any of the tags.
  "
  [examples & options]
  {:pre [(coll? examples)]}
  (let [{:keys [tags exclude]} options]
    (filter (fn [ex]
	      (and (not-any? #(has-tag? ex %) exclude)
		   (every? #(has-tag? ex %) tags)))
	    examples)))

(defn default-filter [examples]
  (let [examples (filter-examples examples :exclude [:skip])
	focused (filter-examples examples :tags [:focus])]
    (if (seq focused) focused examples)))
