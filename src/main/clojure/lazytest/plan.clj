(ns lazytest.plan
  (:use [clojure.set :only (union)]
	[lazytest.arguments :only (nil-or)]
	[lazytest.attach :only (all-groups)]
	[lazytest.groups :only (group?)]
	[lazytest.contexts :only (context?)]))

(defrecord RunnableExample [f contexts])

(defn example? [x]
  (instance? RunnableExample x))

(defn- new-runnable-example
  [f group contexts parent-tags parent-doc]
  {:pre [(nil-or set? (:tags (meta f)))]}
  (let [m (meta f)]
    (RunnableExample. f contexts
		      (assoc m
			:doc (str parent-doc (when parent-doc " ") (:doc m))
			:tags (union parent-tags (:tags m)))
		      nil)))

(defn flatten-group
  "Given a Group g, returns a flat sequence of RunnableExamples from
  that group and all its subgroups. Tags are inherited, doc strings
  are concatenated."
  ([g] (flatten-group g [] #{} nil))
  ([g parent-contexts parent-tags parent-doc]
     {:pre [(group? g)
	    (every? context? parent-contexts)]
      :post [(seq? %)
	     (every? (fn [x] (instance? RunnableExample x)) %)]}
     (let [combined-contexts (vec (concat parent-contexts (:contexts g)))
	   combined-tags (union parent-tags (:tags (meta g)))
	   combined-doc (str parent-doc (when parent-doc " ") (:doc (meta g)))]
       (concat (map #(new-runnable-example % g combined-contexts combined-tags
					   combined-doc)
		    (:examples g))
	       (mapcat #(flatten-group % combined-contexts combined-tags
				       combined-doc)
		       (:subgroups g))))))

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
  {:pre [(coll? examples)
	 (every? example? examples)]}
  (let [{:keys [tags exclude]} options]
    (filter (fn [ex]
	      (and (not-any? #(has-tag? ex %) exclude)
		   (every? #(has-tag? ex %) tags)))
	    examples)))

(defn flat-plan [groups]
  (mapcat flatten-group groups))

(defn default-filter [examples]
  (let [examples (filter-examples examples :exclude [:skip])
	focused (filter-examples examples :tags [:focus])]
    (if (seq focused) focused examples)))

(defn default-plan
  "Returns the default test plan.  Examples/groups wth tag :skip are
  omitted.  If any groups/examples have the tag :focus then only those
  groups/examples will run."
  []
  (default-filter (flat-plan (all-groups))))


;;; self-tests
(comment 
  (use 'lazytest.groups
       'lazytest.contexts)

  (let [g (new-group nil nil nil nil)]
    (assert (= '() (flatten-group g))))

  (let [g (new-group nil [identity identity] nil nil)]
    (assert (= (list (RunnableExample. identity [])
		     (RunnableExample. identity []))
	       (flatten-group g))))

  (let [child (new-group nil [+ -] nil nil)
	parent (new-group nil [* /] [child] nil)]
    (assert (= (list (RunnableExample. * [])
		     (RunnableExample. / [])
		     (RunnableExample. + [])
		     (RunnableExample. - []))
	       (flatten-group parent))))

  (let [c1 (new-context nil nil nil)
	c2 (new-context nil nil nil)
	child (new-group [c2] [+ -] nil nil)
	parent (new-group [c1] [* /] [child] nil)]
    (assert (= (list (RunnableExample. * [c1])
		     (RunnableExample. / [c1])
		     (RunnableExample. + [c1 c2])
		     (RunnableExample. - [c1 c2]))
	       (flatten-group parent))))

)