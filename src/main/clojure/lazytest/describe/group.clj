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
   (let [m (meta this)]
     (cond (:pending m)
             (pending this (when (string? (:pending m)) (:pending m)))
	   (:skip m)
             (skip this (when (string? (:skip m)) (:skip m)))
	   :else
	     (container this
			(map run-tests
			     (concat (map #(prepare-example this %) examples)
				     (map #(prepare-subgroup this %) subgroups))))))))

(declare group?)

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

(defrecord MappingExample [f contexts values]
  RunnableTest
  (run-tests
   [this]
   (let [active (reduce open-context {} contexts)
	 states (concat (map active contexts) values)
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

(defn prepare-mapping-example [parent test-fn values]
  (MappingExample. test-fn
		   (:contexts parent)
		   values
		   (assoc (meta test-fn) :doc (concat-doc parent test-fn))
		   nil))

(defrecord MappingGroup [contexts test-fn values-fn]
  RunnableTest
  (run-tests
   [this]
   (let [m (meta this)]
     (cond (:pending m)
	     (pending this (when (string? (:pending m)) (:pending m)))
	   (:skip m)
	     (skip this (when (string? (:skip m)) (:skip m)))
	   :else
	     (container this
			(map #(run-tests (prepare-mapping-example this test-fn %))
			     (values-fn)))))))

(defn mapping-group [contexts test-fn values-fn metadata]
  {:pre [(vector? contexts)
	 (fn? test-fn)
	 (fn? values-fn)]}
  (MappingGroup. contexts test-fn values-fn metadata nil))

(defn group?
  "True if x is an example group."
  [x]
  (or (isa? (type x) Group)
      (isa? (type x) MappingGroup)))

