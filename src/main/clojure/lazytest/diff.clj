(ns lazytest.diff
  (:use [clojure.data :only (diff)]
	[clojure.set :only (union)]
	[lazytest.color :only (colorize)]))

(defn- pr-different [a-str b-str a b]
  (.append a-str (colorize (pr-str a) :green))
  (.append b-str (colorize (pr-str b) :red)))

(defn- pr-same [a-str b-str a b]
  (.append a-str (pr-str a))
  (.append b-str (pr-str b)))

(declare do-pr-diff)

(defn- pr-pair-diff [a-str b-str a b k]
  (cond
   (and (contains? a k) (not (contains? b k)))
   (do (.append a-str (colorize (pr-str k) :green))
       (.append a-str " ")
       (.append a-str (colorize (pr-str (a k)) :green)))
   (and (not (contains? a k)) (contains? b k))
   (do (.append b-str (colorize (pr-str k) :red))
       (.append b-str " ")
       (.append b-str (colorize (pr-str (b k)) :red)))
   :else
   (do (.append a-str (pr-str k))
       (.append b-str (pr-str k))
       (.append a-str " ")
       (.append b-str " ")
       (do-pr-diff a-str b-str (a k) (b k)))))

(defn- pr-map-diff [a-str b-str a b]
  (.append a-str "{")
  (.append b-str "{")
  (let [ks (sort (union (set (keys a)) (set (keys b))))]
    (doseq [k (butlast ks)]
      (pr-pair-diff a-str b-str a b k)
      (.append a-str ", ")
      (.append b-str ", "))
    (pr-pair-diff a-str b-str a b (last ks)))
  (.append a-str "}")
  (.append b-str "}"))

(defn- pr-set-diff [a-str b-str a b]
  (.append a-str "#{")
  (.append b-str "#{")
  (let [[only-a only-b both] (diff a b)]
    (doseq [x only-a]
      (.append a-str (colorize (pr-str x) :green))
      (.append a-str " "))
    (doseq [x only-b]
      (.append b-str (colorize (pr-str x) :red))
      (.append b-str " "))
    (doseq [x both]
      (.append a-str (pr-str x))
      (.append a-str " ")
      (.append b-str (pr-str x))
      (.append b-str " ")))
  (.append a-str "}")
  (.append b-str "}"))

(defn- pr-seq-diff [a-str b-str a b]
  (.append a-str "(")
  (.append b-str "(")
  (dorun (map (fn [xa xb]
		(do-pr-diff a-str b-str xa xb)
		(.append a-str " ")
		(.append b-str " "))
	      a b))
  (let [c (- (count a) (count b))]
    (when (pos? c)
      (doseq [x (drop (inc c) a)]
	(do-pr-diff a-str b-str x nil)
	(.append a-str " ")
	(.append b-str " ")))
    (when (neg? c)
      (doseq [x (drop (dec (- c)) b)]
	(do-pr-diff a-str b-str nil x)
	(.append a-str " ")
	(.append b-str " "))))
  (.append a-str ")")
  (.append b-str ")"))

(defn- do-pr-diff [a-str b-str a b]
  (let [[only-a only-b both] (diff a b)]
    (cond (nil? both)
	    (pr-different a-str b-str a b)
	  (and (nil? only-a) (nil? only-b))
	    (pr-same a-str b-str a b)
	  (and (map? a) (map? b))
	    (pr-map-diff a-str b-str a b)
	  (and (set? a) (set? b))
	    (pr-set-diff a-str b-str a b)
	  (and (coll? a) (coll? b))
	    (pr-seq-diff a-str b-str a b)
	  :else
	    (pr-different a-str b-str a b))))

(defn pr-diff [a b]
  (let [a-str (StringBuilder.)
	b-str (StringBuilder.)]
    (do-pr-diff a-str b-str a b)
    (println "<A>" (str a-str))
    (println "<B>" (str b-str))))
