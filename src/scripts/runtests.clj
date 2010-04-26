(use '[com.stuartsierra.lazytest :only (run-spec)]
     '[com.stuartsierra.lazytest.report :only (report-and-exit)])

(require 'lazytest-asserts)

(report-and-exit (run-spec "src/test"))
