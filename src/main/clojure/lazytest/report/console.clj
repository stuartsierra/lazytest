(ns lazytest.report.console
  (:use [lazytest.result :only (success?)]
	[clojure.pprint :only (pprint)]
	[lazytest.report.color :only (colorize)]
	[clojure.stacktrace :only (print-cause-trace)]
	[clojure.java.io :only (file)]))

(defprotocol ReportPrinter
  (p [this]))

(defn docs [x]
  (:doc (meta (:source x))))

(defn location [x]
  (let [m (meta (:source x))
	f (.getName (file (:file m)))]
    (str "(" f ":" (:line m) ")")))

(def *indent* 0)

(defn- indent []
  (dotimes [i (dec *indent*)] (clojure.core/print " |   "))
  (when (pos? *indent*) (clojure.core/print " |-- ")))

(defn- iprintln [& args]
  (indent)
  (apply clojure.core/println args))

(defmacro with-indent [& body]
  `(binding [*indent* (inc *indent*)]
     ~@body))

(extend-protocol ReportPrinter
  ;; Base TestResult types
  lazytest.result.Pass
  (p [this]
     (iprintln (colorize (docs this) :green)
	      (location this)))

  lazytest.result.Fail
  (p [this]
     (iprintln (colorize (docs this) :red)
	      (location this))
     (with-indent 
       (iprintln "FAILURE:")
       (p (:reason this))))

  lazytest.result.Thrown
  (p [this]
     (iprintln (colorize (docs this) :red)
	      (location this))
     (with-indent
      (iprintln "ERROR:")
      (print-cause-trace (:throwable this))))

  lazytest.result.Pending
  (p [this]
     (iprintln (colorize (docs this) :blue)
	      (location this))
     (with-indent
      (iprintln "PENDING:" (:reason this))))

  lazytest.result.Skip
  (p [this]
     (iprintln (colorize (docs this) :blue)
	       (location this))
     (with-indent
      (iprintln "SKIPPED:" (:reason this))))

  lazytest.result.TestResultGroup
  (p [this]
     (iprintln (colorize (docs this) (if (success? this) :green :red))
	      (location this))
     (with-indent
       (doseq [c (:children this)] (p c))))

  ;; Failure types
  lazytest.failure.NotEqual
  (p [this]
     (iprintln "Not equal:")
     (doseq [o (:objects this)]
       (indent) (pprint o)))

  lazytest.failure.NotNotEqual
  (p [this]
     (iprintln "Equal but should not be:")
     (doseq [o (:objects this)]
       (indent) (pprint o)))

  lazytest.failure.NotInstanceOf
  (p [this]
     (iprintln "Expected an instance of" (:expected-class this))
     (iprintln "But got an instance of" (:actual-class this)))

  lazytest.failure.NotThrown
  (p [this]
     (iprintln "Expected an instance of" (:class this) "to be thrown")
     (iprintln "But nothing was thrown"))

  lazytest.failure.ThrownWithWrongMessage
  (p [this]
     (iprintln "Expected an object to be thrown with a message matching" (:expected-re this))
     (iprintln "But the actual message was" (:actual-message this)))

  lazytest.failure.NotLogicalTrue
  (p [this]
     (pprint (:value this)))

  lazytest.failure.PredicateFailed
  (p [this]
     (iprintln "The function" (:pred this))
     (iprintln "Did not return true for these arguments:")
     (doseq [arg (:args this)]
       (print " -- ")
       (pprint arg))))

(defn report [results]
  (doseq [r results]
    (p r)))
