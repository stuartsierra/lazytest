(ns runtests
  (:use [com.stuartsierra.lazytest :only (run-spec)]
        [com.stuartsierra.lazytest.report :only (report-and-exit)]))

(report-and-exit (run-spec "src/test"))
