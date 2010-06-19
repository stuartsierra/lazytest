(ns lazytest.describe.group
  (:use [lazytest.arguments :only (nil-or)]
	[lazytest.run :only (RunnableTest run-tests)]
	[lazytest.results :only (pass fail thrown skip pending container)]
	[lazytest.contexts :only (context? open-context close-context)]))

(defrecord RunnableExample [f contexts]
  RunnableTest
  (run-tests
   [this]
   (let [active (reduce open-context {} contexts)
	 states (map active contexts)
	 m (meta f)
	 result (cond (:pending m)
			(pending this (when (string? (:pending m)) (:pending m)))
		      (:skip m)
			(skip this (when (string? (:skip m)) (:skip m)))
		      :else
                        (try (if (apply f states)
			       (pass this states)
			       (fail this states))
			     (catch Throwable t (thrown this states t))))]
     (reduce close-context active contexts)
     result)))

(defn concat-doc [parent child]
  (let [parent-doc (:doc (meta parent))]
    (str parent-doc (when parent-doc " ") (:doc (meta child)))))

(defn prepare-example [parent f]
  (RunnableExample. f (:contexts parent) (assoc (meta f) :doc (concat-doc parent f)) nil))

(defn prepare-subgroup [parent g]
  (vary-meta (assoc g :contexts (vec (concat (:contexts parent) (:contexts g))))
	     assoc :doc (concat-doc parent g)))

(defrecord Group [contexts examples subgroups]
  RunnableTest
  (run-tests
   [this]
   (container this
	      (map run-tests
		   (concat (map #(prepare-example this %) examples)
			   (map #(prepare-subgroup this %) subgroups))))))

(defn group?
  "True if x is an example group."
  [x]
  (isa? (type x) Group))

(defn new-group
  "Creates a Group."
  ([contexts examples subgroups metadata]
     {:pre [(nil-or vector? contexts)
            (nil-or vector? examples)
            (nil-or vector? subgroups)
            (every? context? contexts)
            (every? fn? examples)
            (every? group? subgroups)
            (nil-or map? metadata)]
      :post [(group? %)]}
     (Group. contexts examples subgroups metadata nil)))
