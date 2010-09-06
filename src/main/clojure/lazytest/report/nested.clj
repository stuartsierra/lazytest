(ns lazytest.report.nested
  (:use lazytest.color
	lazytest.suite
	lazytest.test-case
	clojure.pprint
	[clojure.stacktrace :only (print-cause-trace)]))

(defn- identifier [result]
  (let [m (meta (:source result))]
    (or (:doc m) (:name m))))

;;; Nested doc printout

(declare report-result)

(defn- indent [n]
  (dotimes [i n] (print "    ")))

(defn- report-counted-suite-result [result depth]
  (indent depth)
  (let [children (:children result)
	total (count children)
	passed (count (filter :pass? children))
	failed (count (remove :pass? children))]
    (cond
     (zero? total)
       (println (colorize (str (identifier result) " (no cases run)") :yellow))
     (zero? failed)
       (println (colorize (str (identifier result) " (" passed " cases passed)") :green))
     :else
       (println (colorize (str (identifier result) " (" failed " out of " total " cases failed)") :red)))))

(defn- report-normal-suite-result [result depth]
  (indent depth)
  (println (identifier result))
  (doseq [child (:children result)]
    (report-result child (inc depth))))

(defn- unnamed-test-case-result? [x]
  (and (test-case-result? x)
       (nil? (identifier x))))

(defn- report-suite-result [result depth]
  (if (every? unnamed-test-case-result? (:children result))
    (report-counted-suite-result result depth)
    (report-normal-suite-result result depth)))

(defn- report-test-case-result [result depth]
  (indent depth)
  (println (colorize (identifier result) (if (:pass? result) :green :red))))

(defn- result-seq [result]
  (tree-seq suite-result? :children result))

(defn- report-result [result depth]
  (if (suite-result? result)
    (report-suite-result result depth)
    (report-test-case-result result depth)))

;;; Failures

(defn- print-expectation-failed [err]
  (let [reason (. err reason)]
    (println "at" (:file reason) "line" (:line reason))
    (println "Expression:" (:form reason))
    (println "Result:" (pr-str (:result reason)))
    (when (:evaluated reason)
      (println "Evaluated arguments:")
      (pprint (zipmap (rest (:form reason)) (rest (:evaluated reason)))))
    (println "Local bindings:")
    (pprint (:locals reason))))

(defn- report-test-case-failure [result docs]
  (when (not (:pass? result))
    (let [docs (conj docs (identifier result))
	  docstring (interpose " " (remove nil? docs))
	  error (:thrown result)]
      (println (colorize (apply str "FAILURE: " docstring) :red))
      (if (instance? lazytest.ExpectationFailed error)
	(print-expectation-failed error)
	(print-cause-trace error))
      (newline))))

(defn- report-failures [result docs]
  (if (suite-result? result)
    (doseq [child (:children result)]
      (report-failures child (conj docs (identifier result))))
    (report-test-case-failure result docs)))

;;; Summary

(defn- summarize [results]
  (let [test-case-results (remove suite-result? (mapcat result-seq results))
	total (count test-case-results)
	passed (count (filter :pass? test-case-results))
	failed (- total passed)]
    {:total total, :pass passed, :fail failed}))

(defn- print-summary [summary]
  (let [{:keys [total fail]} summary]
    (let [count-msg (str "Ran " total " test cases.")]
      (println (if (zero? total)
		 (colorize count-msg :yellow)
		 count-msg)))
    (println (colorize (str fail " failures.")
		       (if (zero? fail) :green :red)))))

;;; Entry point

(defn report [results]
  (doseq [r results] (report-result r 0))
  (newline)
  (doseq [r results] (report-failures r []))
  (print-summary (summarize results)))

