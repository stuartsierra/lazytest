(ns lazytest.match
  (:use [clojure.set :only (union)]))

;;; Utilities

(deftype Undefined [] Object (toString [this] ""))

(def undefined (Undefined.))

(defn result [& args]
  (with-meta (apply array-map args)
    {:type ::result}))

(defn matcher-type [data matcher-type]
  (vary-meta data assoc ::matcher-type matcher-type))

(defn pad [len s]
  (let [diff (- len (count s))]
    (if (pos? diff)
      (concat s (repeat diff undefined))
      s)))

(defn pad-to-longest [a b]
  (let [len (max (count a) (count b))]
    [(pad len a) (pad len b)]))

;;; Matchers

(defn sequential-safe? [s]
  (try (seq s) true (catch Exception e false)))

(defn nth-matcher [s]
  {:pre [(sequential-safe? s)]}
  (matcher-type s :nth))

(def associative-safe? map?)

(defn key-matcher [m]
  {:pre [(associative-safe? m)]}
  (matcher-type m :key))

;;; Match

(defmulti match (fn [this that]
		  (or (:matcher-type (meta this))
		      (type this))))

(defmethod match :default [this that]
  (result :type :value
	  :matches? (= this that)
	  :expected this
	  :actual that))

(defmethod match :nth [this that]
  (if (sequential-safe? that)
    (let [[this that] (pad-to-longest this that)
	  results (map match this that)]
      (result :match-by :nth
	      :matches? (every? :matches? results)
	      :elements results))
    (result :match-by :nth
	    :matches? false
	    :expected this
	    :actual that)))

(defn getter [m k]
  (if (contains? m k)
    (get m k)
    undefined))

(defmethod match :key [this that]
  (if (associative-safe? that)
      (let [all-keys (seq (union (set (keys this)) (set (keys that))))
	    matches (map match
			 (map #(getter this %) all-keys)
			 (map #(getter that %) all-keys))]
	(result :match-by :key
		:matches? (every? :matches? matches)
		:elements (zipmap all-keys matches)))
      (result :match-by :key
	      :matches? false
	      :expected this
	      :actual that)))

(defn longest-common-substring [s t]
  (let [m (count s)
	n (count t)
	L (make-array Integer/TYPE m n)]
    (loop [i 0, j 0, z 0, ret #{}]
      (prn i j z ret)
      (cond (= j n)
	    (recur (inc i) 0 z ret)
	    
	    (= i m)
	    ret

	    (= (nth s i) (nth t j))
	    (do (if (or (zero? i) (zero? j))
		  (aset-int L i j 1)
		  (aset-int L i j (inc (aget L (dec i) (dec j)))))
		(cond (> (aget L i j) z)
		      (recur i (inc j) (aget L i j)
			     #{(subs s (inc (- i (aget L i j))) (inc i))})
		      (= (aget L i j) z)
		      (recur i (inc j) z
			     (conj ret (subs s (inc (- i z)) (inc i))))
		      :else
		      (recur i (inc j) z ret)))

	    :else
	    (recur i (inc j) z ret)))))