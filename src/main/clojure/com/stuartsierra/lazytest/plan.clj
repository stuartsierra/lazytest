(ns com.stuartsierra.lazytest.plan
  (:use [clojure.set :only (union)]
	[com.stuartsierra.lazytest.arguments :only (or-nil)]
	[com.stuartsierra.lazytest.attach :only (all-groups)]
	[com.stuartsierra.lazytest.groups :only (group?)]
	[com.stuartsierra.lazytest.contexts :only (context?)]))

(defrecord RunnableExample [f contexts])

(defn example? [x]
  (instance? RunnableExample x))

(defn- new-runnable-example
  [f group contexts parent-tags]
  {:pre [(or-nil set? (:tags (meta f)))]}
  (RunnableExample. f contexts
		    (assoc (meta f)
		      :group group
		      :tags (union parent-tags (:tags (meta f))))
		    nil))

(defn flatten-group
  "Given a Group g, returns a flat sequence of RunnableExamples from
  that group and all its subgroups. Tags are inherited."
  ([g] (flatten-group g [] #{}))
  ([g parent-contexts parent-tags]
     {:pre [(group? g)
	    (every? context? parent-contexts)]
      :post [(seq? %)
	     (every? (fn [x] (instance? RunnableExample x)) %)]}
     (let [combined-contexts (vec (concat parent-contexts (:contexts g)))
	   combined-tags (union parent-tags (:tags (meta g)))]
       (concat (map #(new-runnable-example % g combined-contexts combined-tags)
		    (:examples g))
	       (mapcat #(flatten-group % combined-contexts combined-tags)
		       (:subgroups g))))))

(defn has-tag?
  "True if object x has tag t in its metadata :tags."
  [x t]
  {:pre [(or-nil set? (:tags (meta x)))
	 (keyword? t)]}
  (contains? (:tags (meta x)) t))

(defn flat-plan
  "Returns a flat sequence of RunnableExamples for all examples in Groups gs.
  Examples are filtered by the following options:

     :tags [tags]
        Every example must have at least one of the tags.
     :exclude [tags]
        Every example must NOT have any of the tags.
  "
  [gs & options]
  {:pre [(coll? gs)]}
  (let [{:keys [tags exclude]} options]
    (let [examples (mapcat flatten-group gs)]
      (filter (fn [ex]
		(and (not-any? #(has-tag? ex %) exclude)
		     (every? #(has-tag? ex %) tags)))
	      examples))))


;;; self-tests
(comment 
  (use 'com.stuartsierra.lazytest.groups
       'com.stuartsierra.lazytest.contexts)

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