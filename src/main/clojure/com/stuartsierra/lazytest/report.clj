(ns com.stuartsierra.lazytest.report
  (:use com.stuartsierra.lazytest
        [clojure.stacktrace :only (print-cause-trace)])
  (:import (java.io File)))

(defn source-name
  "Given a Test/Assertion result, returns an identifying form for
  the originating Test, Suite, or Assertion."  [r]
  (let [m (meta (:source r))]
    (or (:name m) (:form m) (:source r))))

(defn source-doc
  "Given a Test/Assertion result, returns the doc string of the
  originating Test, Suite, or Assertion."
  [r]
  (:doc (meta (:source r))))

(defn print-source-doc 
  "Given a Test/Assertion result, prints the doc string of the
  originating Test, Suite, or Assertion, if it exists."
  [r]
  (when-let [d (source-doc r)] (prn d)))

(defn line-and-file
  "Returns a string like (FILENAME:LINE) for the TestResult's
  originating Test or Assertion."
  [r]
  (let [m (meta (:source r))
        line (:line m)
        file (:file m)
        file (when file
               (.getName (File. file)))]
    (if (or line file)
      (str "(" file ":" line ")")
      "")))

(defn print-result
  "Prints full details of a TestResult, including file and line
  number, doc string, and stack trace if applicable."
  [r]
  (let [m (meta (:source r))]
    (print (source-name r))
    
    (newline)
    (print-source-doc r)
    (when-let [e (:error r)]
      (println "STACK TRACE")
      (print-cause-trace e))))

(defn simple-report
  "Simple test result reporter.  Takes a TestResult and prints a tree
  of child results tagged as OK/FAIL/ERROR."
  ([r] (simple-report r ""))
  ([r indent]
     (cond (= :com.stuartsierra.lazytest/TestResult (type r))
           (if (success? r)
             (do (println indent "OK" (source-name r) (line-and-file r))
                 (doseq [c (:children r)]
                   (simple-report c (str indent "   "))))
             (do (println indent "FAIL" (source-name r) (line-and-file r))
                 (print-source-doc r)
                 (doseq [c (:children r)]
                   (simple-report c (str indent "   ")))))

           (= :com.stuartsierra.lazytest/TestThrown (type r))
           (do (println indent "ERROR" (source-name r) (line-and-file r))
               (print-source-doc r)
               (print-cause-trace (:error r) (line-and-file r)))

           (= :com.stuartsierra.lazytest/AssertionPassed (type r))
           (println indent "OK" (source-name r) (line-and-file r))

           (= :com.stuartsierra.lazytest/AssertionFailed (type r))
           (do (println indent "FAIL" (source-name r) (line-and-file r))
               (print-source-doc r))

           (= :com.stuartsierra.lazytest/AssertionThrown (type r))
           (do (println indent "ERROR" (source-name r) (line-and-file r))
               (print-source-doc r)
               (print-cause-trace (:error r))))))

(defn report-first-fail
  "Lazy test result reporter.  Takes a TestResult and prints the
  details of only the first failed test.  If all tests pass, prints
  nothing."
  ([r] (report-first-fail r nil))
  ([r stack]
     (cond (= :com.stuartsierra.lazytest/TestResult (type r))
           (when-not (success? r)
             (doseq [c (:children r)]
               (report-first-fail c (cons r stack))))

           (= :com.stuartsierra.lazytest/TestThrown (type r))
           (do (print "ERROR AT ")
               (print-result r)
               (doseq [s stack]
                 (print "IN ")
                 (print-result s)))

           (= :com.stuartsierra.lazytest/AssertionPassed (type r))
           nil

           (= :com.stuartsierra.lazytest/AssertionFailed (type r))
           (do (print "FAIL AT ")
               (print-result r)
               (doseq [s stack]
                 (print "IN ")
                 (print-result s)))

           (= :com.stuartsierra.lazytest/AssertionThrown (type r))
           (do (print "ERROR AT ")
               (print-result r)
               (doseq [s stack]
                 (print "IN ")
                 (print-result s))))))

