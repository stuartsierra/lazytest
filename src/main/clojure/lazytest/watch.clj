(ns lazytest.watch
  (:gen-class)
  (:use	[clojure.string :only (split join)])
  (:require lazytest.runner.console
	    lazytest.report.nested)
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)
	   (java.util.regex Pattern)
	   (java.io File)))

;; (defn reload-and-run [dirs timestamp-atom reporter]
;;   (try 
;;     (let [names (newer-namespaces dirs @timestamp-atom)]
;;       (when (seq names)
;; 	(reset! timestamp-atom (System/currentTimeMillis))
;; 	(println)
;; 	(println "======================================================================")
;; 	(println "Reloading" (join ", " names))
;; 	(doseq [n names] (remove-ns n))
;; 	(doseq [n names] (require n :reload))
;; 	(println "Running examples at" (java.util.Date.))
;; 	(reporter (apply lazytest.runner.console/run-tests (all-ns)))
;; 	(println "\nDone.")))
;;     (catch Throwable t
;;       (println "ERROR:" t)
;;       (.printStackTrace t))))

;; (defn start [dirs & options]
;;   (let [dirs (map file dirs)
;; 	{:keys [reporter delay],
;; 	 :or {delay 500, reporter lazytest.report.nested/report}} options
;; 	last-run-timestamp (atom 0)
;; 	runner #(reload-and-run dirs last-run-timestamp reporter)]
;;     (doto (ScheduledThreadPoolExecutor. 1)
;;       (.scheduleWithFixedDelay runner 0 delay TimeUnit/MILLISECONDS))))

;; (defn -main [& args]
;;   (start args))
