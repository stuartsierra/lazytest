;; Copyright (c) Rich Hickey. All rights reserved.  The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns examples.multimethods
  "Tests for Clojure multimethods and hierarchies, adapted from the
  original clojure.test-clojure.multimethods, written by Frantisek
  Sodomka, Robert Lachlan, and Stuart Halloway."
  (:use lazytest.describe
	lazytest.expect.thrown
	[lazytest.context.stub :only (global-stub)])
  (:require [clojure.set :as set]))

(defn hierarchy-tags
  "Return all tags in a derivation hierarchy"
  [h]
  (set/select
   #(instance? clojure.lang.Named %)
   (reduce into #{} (map keys (vals h)))))

(defn transitive-closure
  "Return all objects reachable by calling f starting with o,
   not including o itself. f should return a collection."
  [o f]
  (loop [results #{}
         more #{o}]
    (let [new-objects (set/difference more results)]
      (if (seq new-objects)
        (recur (set/union results more) (reduce into #{} (map f new-objects)))
        (disj results o)))))

(defn tag-descendants
  "Set of descedants which are tags (i.e. Named)."
  [& args]
  (set/select
   #(instance? clojure.lang.Named %)
   (or (apply descendants args) #{})))

(defn is-valid-hierarchy [h]
  (testing "it is a valid hierarchy"
    (given [tags (hierarchy-tags h)]
      (testing "ancestors are the transitive closure of parents"
	(for [tag tags]
	  (it (= (transitive-closure tag #(parents h %))
		 (or (ancestors h tag) #{})))))
      (testing "ancestors are transitive"
	(for [tag tags]
	  (it (= (transitive-closure tag #(ancestors h %))
		 (or (ancestors h tag) #{})))))
      (testing "tag descendants are transitive"
	(for [tag tags]
	  (it (= (transitive-closure tag #(tag-descendants h %))
		 (or (tag-descendants h tag) #{})))))
      (testing "a tag isa? all of its parents"
	(for [tag tags
	      :let [parents (parents h tag)]
	      parent parents]
	  (it (isa? h tag parent))))
      (testing "a tag isa? all of its ancestors"
	(for [tag tags
	      :let [ancestors (ancestors h tag)]
	      ancestor ancestors]
	  (it (isa? h tag ancestor))))
      (testing "all my descendants have me as an ancestor"
	(for [tag tags
	      :let [descendants (descendants h tag)]
	      descendant descendants]
	  (it (isa? h descendant tag))))
      (testing "there are no cycles in parents"
	(for [tag tags]
	  (it (not (contains? (transitive-closure tag #(parents h %)) tag)))))
      (testing "there are no cycles in descendants"
	(for [tag tags]
	  (it (not (contains? (descendants h tag) tag))))))))

(describe "Cycles are forbidden: a tag"
  (given [family (reduce #(apply derive (cons %1 %2)) (make-hierarchy)
			 [[::parent-1 ::ancestor-1]
			  [::parent-1 ::ancestor-2]
			  [::parent-2 ::ancestor-2]
			  [::child ::parent-2]
			  [::child ::parent-1]])]
    (it "cannot be its own parent"
      (throws-with-msg? Throwable #"\(not= tag parent\)"
	#(derive family ::child ::child)))
    (it "cannot be its own ancestor"
      (throws-with-msg? Throwable #"Cyclic derivation: :examples.multimethods/child has :examples.multimethods/ancestor-1 as ancestor"
	#(derive family ::ancestor-1 ::child)))))

(describe "Using diamond inheritance"
  (given [diamond (reduce #(apply derive (cons %1 %2)) (make-hierarchy)
			  [[::mammal ::animal]
			   [::bird ::animal]
			   [::griffin ::mammal]
			   [::griffin ::bird]])]
    (is-valid-hierarchy diamond)
    (it "a griffin is a mammal, indirectly through mammal and bird"
      (isa? diamond ::griffin ::animal))
    (it "a griffin is a bird"
      (isa? diamond ::griffin ::bird))
    (testing "after underive"
      (given [bird-no-more (underive diamond ::griffin ::bird)]
	(is-valid-hierarchy bird-no-more)
	(it "griffin is no longer a bird"
	  (not (isa? bird-no-more ::griffin ::bird)))
	(it "griffin is still an animal, via mammal"
	  (isa? bird-no-more ::griffin ::animal))))))

(describe "Derivation bridges to Java inheritance:"
  (given [h (derive (make-hierarchy) java.util.Map ::map)]
    (it "a Java class can be isa? a tag"
      (isa? h java.util.Map ::map))
    (it "if a Java class isa? a tag, so are its subclasses..."
      (isa? h java.util.HashMap ::map))
    (it "...but not its superclasses!"
      (not (isa? h java.util.Collection ::map)))))

(describe "The global hierarchy"
  (using-once [_ (global-stub #'clojure.core/global-hierarchy (make-hierarchy))]
    (is-valid-hierarchy @#'clojure.core/global-hierarchy)
    (using-once "when you add some derivations..."
		[_ (before (derive ::lion ::cat)
			   (derive ::manx ::cat))]
		(testing "...isa? sees the derivations"
		  (it (isa? ::lion ::cat))
		  (it (not (isa? ::cat ::lion))))
		(testing "... you can traverse the derivations"
		  (it (= #{::manx ::lion} (descendants ::cat)))
		  (it (= #{::cat} (parents ::manx)))
		  (it (= #{::cat} (ancestors ::manx))))
		(using-once "then, remove a derivation..."
			    [_ (before (underive ::manx ::cat))]
			    (testing "... traversals update accordingly"
			      (it (= #{::lion} (descendants ::cat)))
			      (it (nil? (parents ::manx)))
			      (it (nil? (ancestors ::manx))))))))
