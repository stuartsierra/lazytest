(ns lazytest.report.nested
  (:use lazytest.report.color
	lazytest.suite))

(defn identifier [result]
  (let [m (meta (:source result))]
    (or (:doc m) (:name m))))

(declare report-result)

(defn indent [n]
  (dotimes [i n] (print "    ")))

(defn report-suite-result [result depth]
  (indent depth)
  (println (identifier result))
  (doseq [child (:children result)]
    (report-result child (inc depth))))

(defn report-test-case-result [result depth]
  (indent depth)
  (println (colorize (identifier result) (if (:pass? result) :green :red))))

(defn result-seq [result]
  (tree-seq suite-result? :children result))

(defn report-result [result depth]
  (if (suite-result? result)
    (report-suite-result result depth)
    (report-test-case-result result depth)))

(defn summarize [results]
  (let [test-case-results (remove suite-result? (mapcat result-seq results))
	total (count test-case-results)
	passed (count (filter :pass? test-case-results))
	failed (- total passed)]
    {:total total, :pass passed, :fail failed}))

(defn print-summary [summary]
  (let [count-msg (str "Ran " (:total summary) " tests.")]
    (println (if (zero? (:total summary))
	       (colorize count-msg :yellow)
	       count-msg)))
  (println (colorize (str (:fail summary) " failures.")
		     (if (zero? (:fail summary)) :green :red))))

(defn report [results]
  (doseq [r results] (report-result r 0))
  (newline)
  (print-summary (summarize results)))

