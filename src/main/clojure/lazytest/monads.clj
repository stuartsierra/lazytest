(ns lazytest.monads
  (:import lazytest.ExpectationFailed))

(defn store-result [world source result]
  (assoc-in world [:results source] result))

(assert (= {:results {= ::the-result}} (store-result {} = ::the-result)))

(defn try-expectation [f]
  (fn [] (try (f) true (catch ExpectationFailed e e))))

(defn store-assertion [assertion]
  (fn [world] 
    (store-result world assertion (try-expectation assertion))))

(defn assertion-with-arguments [f arguments]
  (fn [] (apply f arguments)))