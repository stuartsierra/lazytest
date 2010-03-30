(ns com.stuartsierra.lazytest.report
  (:use [com.stuartsierra.lazytest :only (success?)]
        [com.stuartsierra.lazytest.color :only (colorize)]
        [clojure.stacktrace :only (print-cause-trace)])
  (:import (java.io File)))

(defn result-seq
  "Given a single TestResult, returns a depth-first sequence of that
  TestResult and all its children."
  [r]
  (tree-seq :children :children r))

(defn details
  "Given a TestResult, returns the map of :name, :ns, :file, :line,
  :generator, and :form."
  [r]
  (meta (:source r)))

(defn print-details
  "Prints full details of a TestResult, including file and line
  number, doc string, and stack trace if applicable."
  [r]
  (println
   (cond (success? r) (colorize "SUCCESS" :fg-green)
         (:throwable r) (colorize "ERROR" :fg-red)
         :else (colorize "FAIL" :fg-red)))
  (let [m (details r)]
    (when-let [n (:name m)] (println "Name:" n))
    (when-let [d (:doc m)] (println "Doc: " d))
    (when (and (:form m) (not (:name m)))
      (print "Form: ")
      (prn (:form m)))
    (when-let [f (:file m)] (println "File:" f))
    (when-let [l (:line m)] (println "Line:" l))
    (when (seq (:states r))
      (print "Context states: ")
      (prn (:states r)))
    (when-let [e (:throwable r)]
      (println "STACK TRACE")
      (print-cause-trace e))))

(defn assertion-results
  "Returns a sequence of all assertion results (not grouped test
  results) in the tree rooted at r."
  [r]
  (filter #(empty? (:children %)) (result-seq r)))

(defn summary
  "Returns a map of :total, :pass, :fail, and :error
  counts for assertions in the results tree rooted at r."
  [r]
  (reduce (fn [m r]
            (assoc (cond (success? r) (assoc m :pass (inc (:pass m)))
                         (:throwable r) (assoc m :error (inc (:error m)))
                         :else (assoc m :fail (inc (:fail m))))
              :assertions (inc (:assertions m 0))))
          {:assertions 0, :pass 0, :fail 0, :error 0}
          (assertion-results r)))

(defn print-summary [r]
  (let [{:keys [assertions pass fail error]} (summary r)]
    (println "Ran" assertions "assertions.")
    (print (colorize (str fail " failures")
                     (if (zero? fail) :fg-green :fg-red)))
    (print ", ")
    (print (colorize (str error " errors")
                     (if (zero? error) :fg-green :fg-red)))
    (newline)))

(defn dot-report
  "Simple spec report.  Prints a dot for each passed assertion; prints
  details for each failure."
  [r]
  (println "Running" (:name (details r))
           "at" (str (java.util.Date.)))
  (doseq [c (assertion-results r)]
    (if (success? c)
      (do (print (colorize "." :fg-green)) (flush))
      (do (newline) (print-details c))))
  (newline)
  (print-summary r))

(defn- spec-report* [r parents]
  (if (seq (:children r))
    (doseq [c (:children r)]
      (spec-report* c (conj parents r)))
    (if (success? r)
      (do (print (colorize "." :fg-green)) (flush))
      (do (newline)
          (print-details
           (assoc r :source
                  (vary-meta (:source r) assoc :doc
                             (apply str (interpose " "
                                                   (filter identity
                                                           (map #(:doc (details %))
                                                                (conj parents r))))))))))))

(defn spec-report
  "Like dot-report but concatenates :doc strings for nested specs."
  [r]
  (println "Running" (:name (details r))
           "at" (str (java.util.Date.)))
  (spec-report* r [])
  (newline)
  (print-summary r))

