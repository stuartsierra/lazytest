(ns lazytest.report.console
  (:use [lazytest.result :only (success?)]
	[clojure.pprint :only (pprint)]
	[lazytest.report.color :only (colorize)]
	[clojure.stacktrace :only (print-cause-trace)]
	[clojure.java.io :only (file)]))

(defn report [results]
  (pprint results))
