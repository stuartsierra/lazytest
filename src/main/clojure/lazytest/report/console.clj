(ns lazytest.report.console
  (:use [lazytest.test-result :only (success?)]
	[clojure.pprint :only (pprint)]
	[clojure.stacktrace :only (print-cause-trace)]))

(defprotocol ReportPrinter
  (p [this]))

(extend-protocol ReportPrinter
  ;; Base TestResult types
  lazytest.test-result.Pass
  (p [this]
     (print ".")
     (flush))

  lazytest.test-result.Fail
  (p [this]
     (println "\nFAIL")
     (print "Metadata: ")
     (pprint (meta this))
     (print "Reason: ")
     (p (:reason this)))

  lazytest.test-result.Thrown
  (p [this]
     (println "\nERROR")
     (print "Metadata: ")
     (pprint (meta this))
     (print-cause-trace (:throwable this)))

  lazytest.test-result.Pending
  (p [this]
     (println "\nPENDING")
     (print "Metadata: ")
     (print "Reason:" (:reason this)))

  lazytest.test-result.Skip
  (p [this]
     (println "\nSKIPPED")
     (print "Metadata: ")
     (print "Reason:" (:reason this)))

  ;; Failure types
  lazytest.failure.NotEqual
  (p [this]
     (println "Not equal:")
     (doseq [o (:objects this)]
       (print " -- ")
       (pprint o)))

  lazytest.failure.NotNotEqual
  (p [this]
     (println "Equal but should not be:")
     (doseq [o (:objects this)]
       (print " -- ")
       (pprint o)))

  lazytest.failure.NotInstanceOf
  (p [this]
     (println "Expected an instance of" (:expected-class this))
     (println "But got an instance of" (:actual-class this)))

  lazytest.failure.NotThrown
  (p [this]
     (println "Expected an instance of" (:class this) "to be thrown")
     (println "But nothing was thrown"))

  lazytest.failure.ThrownWithWrongMessage
  (p [this]
     (println "Expected an object to be thrown with a message matching" (:expected-re this))
     (println "But the actual message was" (:actual-message this)))

  lazytest.failure.NotLogicalTrue
  (p [this]
     (println "Not true:")
     (print " -- ")
     (pprint (:value this)))

  lazytest.failure.PredicateFailed
  (p [this]
     (println "The function" (:pred this))
     (println "Did not return true for these arguments:")
     (doseq [arg (:args this)]
       (print " -- ")
       (pprint arg))))

(defn report [results]
  (doseq [r results]
    (if (seq (:children r))
      (report (:children r))
      (if (success? r)
	(do (print ".") (flush))
	(p r)))))
