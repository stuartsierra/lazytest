(ns lazytest.watch
  (:gen-class)
  (:use	[lazytest.reload :only (reload)]
	[lazytest.tracker :only (tracker)]
	[clojure.java.io :only (file)]
	[clojure.string :only (join)])
  (:require lazytest.runner.console
	    lazytest.report.nested)
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)
	   (java.util.regex Pattern)
	   (java.io File)))

(defn reload-and-run [tracker run-fn report-fn]
  (try 
    (let [new-names (seq (tracker))]
      (when new-names
	(println)
	(println "======================================================================")
	(println "At " (java.util.Date.))
	(println "Reloading" (join ", " new-names))
	(apply reload new-names)
	(report-fn (apply run-fn new-names))
	(println "\nDone.")))
    (catch Throwable t
      (println "ERROR:" t)
      (.printStackTrace t))))

(defn reloading-runner [dirs run-fn report-fn]
  (let [dirs (map file dirs)
	track (tracker dirs 0)]
    (fn [] (reload-and-run track run-fn report-fn))))

(defn start [dirs & options]
  (let [{:keys [run-fn report-fn delay]
	 :or {run-fn lazytest.runner.console/run-tests
	      report-fn lazytest.report.nested/report
	      delay 500}} options
	f (reloading-runner dirs run-fn report-fn)]
    (doto (ScheduledThreadPoolExecutor. 1)
      (.scheduleWithFixedDelay f 0 delay TimeUnit/MILLISECONDS))))

(defn -main [& args]
  (start args))
