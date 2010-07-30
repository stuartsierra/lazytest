(ns lazytest.report.console
  (:use [lazytest.result :only (success?)]
	[clojure.pprint :only (pprint)]
	[lazytest.report.color :only (colorize)]
	[clojure.stacktrace :only (print-cause-trace)]
	[clojure.java.io :only (file)]))

(defn indent [level]
  (dotimes [i level] (print "    ")))

(defprotocol ConsoleTestReport
  (p [r l] "Print result r to the console, indented to level l"))

(defn docs [r]
  (:doc (meta (:source r))))

(defn location [r]
  (let [m (meta (:source r))]
    (str (:file m) ":" (:line m))))

(extend-protocol ConsoleTestReport
  lazytest.result.TestResultGroup
  (p [r l]
     (indent l)
     (println (colorize (docs r) (if (success? r) :green :red)))
     (doseq [c (:children r)]
       (p c (inc l))))

  lazytest.result.Pass
  (p [r l]
     (indent l)
     (println (colorize (docs r) :green)))

  lazytest.result.Fail
  (p [r l]
     (indent l)
     (println (colorize (docs r) :red))
     (println "at" (location r))
     (pprint (:reason r)))

  lazytest.result.Thrown
  (p [r l]
     (indent l)
     (println (colorize (docs r) :red))
     (println "at" (location r))
     (print-cause-trace (:throwable r)))

  lazytest.result.Skip
  (p [r l]
     (indent l)
     (println (colorize (docs r) :blue)))

  lazytest.result.Pending
  (p [r l]
     (indent l)
     (println (colorize (docs r)) :blue)))

(defn report [results]
  (doseq [r results] (p r 0)))
