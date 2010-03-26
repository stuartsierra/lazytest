(ns com.stuartsierra.lazytest.report
  (:use [com.stuartsierra.lazytest :only (success?)]
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
  (let [m (details r)]
    (when-let [n (:name m)] (println "Name:" n))
    (when-let [d (:doc m)] (println "Doc: " d))
    (when (and (:form m) (not (:name m)))
      (println "Form:" (:form m)))
    (when-let [f (:file m)] (println "File:" f))
    (when-let [l (:line m)] (println "Line:" l))
    (when (seq (:states r)) (println "Context states:" (:states r)))
    (when-let [e (:throwable r)]
      (println "STACK TRACE")
      (print-cause-trace e))))

