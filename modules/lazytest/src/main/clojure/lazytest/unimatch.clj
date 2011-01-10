(ns lazytest.unimatch)

;;; Subsets of match categories

(derive ::match-by-condition ::match-anything)

(derive ::match-by-predicate ::match-by-condition)
(derive ::match-by-structure ::match-by-condition)
(derive ::match-by-value     ::match-by-condition)

(derive ::anything   ::match-anything)
(derive ::equality   ::match-by-value)
(derive ::sequential ::match-by-structure)

;;; Creating matchers from data

(defmulti matcher type)

(defmethod matcher clojure.lang.IPersistentList [x]
  {:match ::sequential
   :type (type x)
   :elements (map matcher x)})

(defmethod matcher clojure.lang.IPersistentVector [x]
  {:match ::sequential
   :type (type x)
   :elements (map matcher x)})

(defmethod matcher ::matcher [x]
  x)

(defmethod matcher :default [x]
  {:match ::equality
   :type (type x)
   :value x})

;;; Specialized matchers

(defn binding-matcher [name]
  (with-meta {:match ::binding
	      :name name}
    {:type ::matcher}))

(def anything {:match ::anything})

;;; Merging matchers

(defmulti merge (fn [a b] [(:match a) (:match b)]))

(defmethod merge [::equality ::equality] [a b]
  (if (= (:value a) (:value b))
    {:match ::equality
     :matches? true
     :value (:value a)}
    {:match ::equality
     :matches? false
     :a (:value a)
     :b (:value b)}))

(defmethod merge [::anything ::anything] [a b]
  {:match ::anything
   :matches? true})

(defmethod merge [::match-anything ::match-by-condition] [a b]
  b)

(defmethod merge [::match-by-condition ::match-anything] [a b]
  a)

(defmethod merge [::anything ::missing] [a b]
  {:match ::missing-element
   :matches? false
   :a a
   :b b})

(defmethod merge [::missing ::anything] [a b]
  {:match ::missing-element
   :matches? false
   :a a
   :b b})

(defn pad [len s]
  (let [diff (- len (count s))]
    (if (pos? diff)
      (concat s (repeat diff {:match ::missing}))
      s)))

(defn sequential-merge [as bs]
  (let [len (max (count as) (count bs))]
    (map merge (pad len as) (pad len bs))))

(defmethod merge [::sequential ::sequential] [a b]
  (let [m (sequential-merge (:elements a) (:elements b))]
    {:match ::sequential
     :matches? (every? :matches? m)
     :elements m}))

(defmethod merge [::binding ::match-anything] [a b]
  {:match ::binding
   :matches? true
   :value b
   :name (:name a)})

(defmethod merge [::match-anything ::binding] [a b]
  {:match ::binding
   :matches? true
   :value a
   :name (:name b)})

(defmethod merge [::binding ::binding] [a b]
  {:match ::paired-binding
   :a a
   :b b})

;;; Rendering results

(defmulti render :match)

(defmethod render ::sequential [x]
  (if (:matches? x)
    (map render (:elements x))
    (throw (Exception.
	    (str "Tried to render non-match: " (pr-str x))))))

(defmethod render ::binding [x]
  (:name x))

(defmethod render :default [x]
  (if (:matches? x)
    (:value x)
    (throw (Exception. "Tried to render non-match: " (pr-str x)))))

