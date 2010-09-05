(ns lazytest.random
  "Composable generators for random data.")

(defn rand-int-in-range
  "Returns a random integer between min (inclusive) and max (exclusive)."
  [min max]
  {:pre [(integer? min)
	 (integer? max)
	 (<= min max)]}
  (+ min (rand-int (- max min))))

(def ^{:doc "Default minimum for random integers"}
     min-random-integer (/ Integer/MIN_VALUE 2))

(def ^{:doc "Default maximum for random integers"}
     max-random-integer (/ (dec Integer/MAX_VALUE) 2))

(defn integer [& options]
  "Returns a function which returns a random integer. options
  are :min (inclusive) and :max (exclusive). Defaults are
  min-random-integer and max-random-integer."
  (let [{:keys [min max]
	 :or {min min-random-integer, max max-random-integer}} options]
    (fn [] (rand-int-in-range min max))))

(defn rand-double-in-range
  "Returns a random double between min and max."
  [min max]
  {:pre [(<= min max)]}
  (+ min (* (- max min) (Math/random))))

(def ^{:doc "Default minimum for random doubles"}
     min-random-double (/ Double/MIN_VALUE 2.0))

(def ^{:doc "Default maximum for random doubles"}
     max-random-double (/ Double/MAX_VALUE 2.0))

(defn double
  "Returns a function which returns a random double. options are :min
  and :max. Defaults are min-random-double and max-random-double."
  [& options]
  (let [{:keys [min max]
	 :or {min min-random-double, max max-random-double}} options]
    (fn [] (rand-double-in-range min max))))

(def ^{:doc "Default minimum length for random collections"}
     min-random-length 0)

(def ^{:doc "Default maximum length for random collections"}
     max-random-length 50)

(defn sequence-of
  "Returns a function whicn returns a sequence populated with the
  results of calling f a random number of times. options
  are :min (inclusive) and :max (exclusive) for the length of the
  list, default to min-random-length and max-random-length. The
  returned sequence will be passed through the 'constructor' function
  before being returned."  
  [constructor f & options]
  (let [{:keys [min max]
	 :or {min min-random-length, max max-random-length}} options]
    (fn [] (constructor (repeatedly (rand-int-in-range min max) f)))))

(defn list-of
  "Like sequence-of; returns a list."
  [f & options]
  (apply sequence-of list* f options))

(defn vector-of
  "Like sequence-of; returns a vector."
  [f & options]
  (apply sequence-of vec f options))

(defn pair
  "Returns a function which returns a pair of random values selected
  by key-fn and value-fn."
  [key-fn value-fn]
  (fn [] [(key-fn) (value-fn)]))

(defn map-of
  "Like sequence-of; returns a map. f must return a key-value pair."
  [f & options]
  (apply sequence-of #(into {} %) f options))

(defn string-of
  "Like sequence-of; returns a string."
  [f & options]
  (apply sequence-of #(apply str %) f options))

(def ^{:doc "Collection of whitespace characters: space, tab, and newline"}
     whitespace [\space \tab \newline])

(def ^{:doc "Collection of characters for digits 0 through 9"}
     digit (vec (map char (range 48 58))))

(def ^{:doc "Collection of upper-case English letters"}
     upper-case (vec (map char (range 65 91))))

(def ^{:doc "Collection of lower-case English letters"}
     lower-case (vec (map char (range 97 123))))

(def ^{:doc "Collection of upper- and lower-case English letters"}
     letter (vec (concat upper-case lower-case)))

(def ^{:doc "Collection of printable ASCII characters, including
     spaces but not tabs or line breaks."}
     printable (vec (map char (range 32 127))))

(defn pick
  "Returns a function which returns a random element from any of the
  supplied collections."
  [& colls]
  (let [v (vec (apply concat colls))]
    (fn [] (rand-nth v))))
