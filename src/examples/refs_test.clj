(ns refs-test
  (:use [com.stuartsierra.lazytest
         :only (spec describe given is defcontext)]))

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

(describe *ns*
 (spec ref-stress-test
   "Two refs, updated and read in transactions"
   (given [rs two-refs
           tt buncha-threads]
     "should always have consistent values."
     (is (let [[ra rb] rs]
           (every? true? (for [i (range 100000)]
                           (let [[a b] (dosync [@ra @rb])]
                             (= a b)))))))))
