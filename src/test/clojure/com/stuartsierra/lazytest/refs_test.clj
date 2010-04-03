(ns com.stuartsierra.lazytest.refs-test
  (:use [com.stuartsierra.lazytest :only (spec is given defcontext)]))

(defcontext two-refs []
  [(ref 1) (ref 1)])

(defcontext buncha-threads [rs two-refs]
  (let [[ra rb] rs]
    (doall
     (for [i (range 50), f [inc dec]]
       (doto (Thread. #(dosync (alter ra f) (alter rb f)))
         (.start)))))
  :after [threads]
  (doseq [t threads]
    (.stop t)))

(spec ref-stress-test
      (given [rs two-refs
              tt buncha-threads]
        (let [[ra rb] rs]
          (every? true? (for [i (range 1000000)]
                          (let [[a b] (dosync [@ra @rb])]
                            (= a b)))))))
