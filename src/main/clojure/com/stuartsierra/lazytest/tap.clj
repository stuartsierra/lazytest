(ns com.stuartsierra.lazytest.tap
  (:use [com.stuartsierra.lazytest
         :only (assertion-result? result-seq success?)]
        [com.stuartsierra.lazytest.report
         :only (source-name line-and-file print-result)]
        [clojure.stacktrace
         :only (print-cause-trace)]))

(defn print-tap-plan
  "Prints a TAP plan line like '1..n'.  n is the number of tests"
  [n]
  (println (str "1.." n)))

(defn print-tap-diagnostic
  "Prints a TAP diagnostic line.  data is a (possibly multi-line)
  string."
  [data]
  (doseq [line (.split #^String data "\n")]
    (println "#" line)))

(defn print-tap-pass
  "Prints a TAP 'ok' line.  msg is a string, with no line breaks"
  [msg]
  (println "ok" msg))

(defn print-tap-fail 
  "Prints a TAP 'not ok' line.  msg is a string, with no line breaks"
  [msg]
  (println "not ok" msg))

(defn tap-test-id [r]
  (str (source-name r) " " (line-and-file r)))

(defn report-tap-test [r]
  (if (assertion-result? r)
    (if (success? r)
      (print-tap-pass (tap-test-id r))
      (do (print-tap-fail (tap-test-id r))
          (print-tap-diagnostic (with-out-str (print-result r)))))
    (doseq [c (:children r)]
      (report-tap-test c))))

(defn report-tap [r]
  (print-tap-plan (count (filter assertion-result? (result-seq r))))
  (report-tap-test r))
