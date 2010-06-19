(ns lazytest.report
  (:use [lazytest.results
	 :only (success? container? error? pending? skipped?)]
        [lazytest.color :only (colorize)]
        [clojure.stacktrace :only (print-cause-trace)])
  (:import (java.io File)))

(defn details
  "Given a TestResult, returns the map of :ns, :file, :line,
  and :form."
  [r]
  (meta (:source r)))

(defn print-details
  "Prints full details of a TestResult, including file and line
  number, doc string, and stack trace if applicable."
  [r]
  (println
   (cond (pending? r) (colorize "PENDING" :fg-yellow)
         (success? r) (colorize "SUCCESS" :fg-green)
         (error? r) (colorize "ERROR" :fg-red)
         :else (colorize "FAIL" :fg-red)))
  (let [m (details r)]
    (when-let [n (:name m)] (println "Name:" n))
    (when-let [nn (:ns m)] (println "NS:  " (ns-name nn)))
    (when-let [d (:doc m)] (println "Doc: " d))
    (when-let [e (:expr m)] (println "Expr:" e))
    (when-let [f (:file m)] (println "File:" f))
    (when-let [l (:line m)] (println "Line:" l))
    (when (seq (:locals m))
      (print "Givens: ")
      (prn (zipmap (:locals m) (:states r))))
    (when-let [e (:throwable r)]
      (println "STACK TRACE")
      (print-cause-trace e))))

(defn assertion-results
  "Returns a sequence of all assertion results (not grouped test
  results) in the tree rooted at r."
  [r]
  r)

(defn- inc-keys
  "Increment the value of each key in map m."
  [m & keys]
  (reduce (fn [m k] (assoc m k (inc (get m k 0))))
          m keys))

(defn summary
  "Returns a map of :total, :pass, :fail, and :error
  counts for assertions in the results tree rooted at r."
  [res]
  (reduce (fn [m r]
            (cond (pending? r) (inc-keys m :pending)
                  (container? r) m
                  (success? r) (inc-keys m :assertions :pass)
                  (error? r) (inc-keys m :assertions :error)
                  :else (inc-keys m :assertions :fail)))
          {:assertions 0, :pass 0, :fail 0, :error 0, :pending 0}
          res))

(defn print-summary [r]
  (let [{:keys [assertions pass fail error pending]} (summary r)]
    (println (colorize (str "Ran " assertions " examples.")
                       (if (zero? assertions) :fg-yellow :reset)))
    (print (colorize (str fail " failures")
                     (if (zero? fail) :fg-green :fg-red)))
    (print ", ")
    (print (colorize (str error " errors")
                     (if (zero? error) :fg-green :fg-red)))
    (print ", ")
    (print (colorize (str pending " pending")
                     (if (zero? pending) :fg-green :fg-yellow)))
    (newline)
    (flush)))

(defn- dot-report [r]
  (if (and (success? r) (not (pending? r)))
    (do (print (colorize "." :fg-green))
	(flush))
    (do (newline)
	(print-details r))))

(defn flatten-results [results-seq]
  (mapcat (fn [r] (if (and (container? r)
			   (not (pending? r))
			   (not (skipped? r)))
		    (flatten-results (:children r))
		    (list r)))
	  results-seq))

(defn report
  "Simple report of test results.  Prints a dot for each passed
  example; prints details for each failure or pending spec.  Uses ANSI
  color if lazytest.color/colorize? is true."
  [result-groups-seq]
  (let [results (flatten-results result-groups-seq)]
    (newline)
    (doseq [r results]
      (dot-report r))
    (newline)
    (print-summary results)))

(defn report-and-exit
  "Calls function f (defaults to report) on test result r and
  exits.  Exit status is 0 if all specs passed, -1 if some failed."
  ([r] (report-and-exit r report))
  ([r f]
     (f r)
     (System/exit (if (success? r) 0 -1))))
