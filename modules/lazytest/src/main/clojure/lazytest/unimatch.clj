(ns lazytest.unimatch)

;;; Hierarchy of matcher types

(def matcher-hierarchy
  (-> (make-hierarchy)
      (derive :structural :matcher)
      (derive :variable :matcher)
      (derive :wildcard :matcher)
      (derive :value :matcher)
      (derive :predcate :matcher)

      (derive :sequential :structural)
      (derive :associative :structural)

      (derive :free :variable)
      (derive :constrained :variable)
      (derive :bound :variable)

      (derive :conjunction :predicate)))

;;; Creating matchers

(def wildcard (with-meta {} {:type :wildcard}))

(defmulti matcher type)

(defmethod matcher :matcher [x] x)

(defmethod matcher clojure.lang.IFn [x]
  (with-meta {:pred x} {:type :predicate}))

(defmethod matcher :default [x]
  (with-meta {:value x} {:type :value}))

;;; Merging matchers

(defmulti merge (fn [a b] [(type a) (type b)]))

(defmethod merge )