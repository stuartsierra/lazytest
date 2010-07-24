(ns lazytest.watch
  (:gen-class)
  (:use	[lazytest.runnable-test :only (run-tests)]
	[lazytest.testable :only (get-tests)]
	[lazytest.report.console :only (report)]
	[clojure.contrib.find-namespaces
	 :only (find-clojure-sources-in-dir
		read-file-ns-decl)]
	[clojure.java.io :only (file)]
	[clojure.string :only (split join)])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)
	   (java.util.regex Pattern)
	   (java.io File)))

(defn find-sources
  [dirs]
  {:pre [(coll? dirs)
	 (every? (fn [d] (instance? java.io.File d)) dirs)]}
  (mapcat find-clojure-sources-in-dir dirs))

(defn namespace-for-file [f]
  (second (read-file-ns-decl f)))

(defn newer-sources [dirs timestamp]
  (filter #(> (.lastModified %) timestamp) (find-sources dirs)))

(defn newer-namespaces [dirs timestamp]
  (remove nil? (map namespace-for-file (newer-sources dirs timestamp))))

(defn reload-and-run [dirs timestamp-atom reporter]
  (try 
    (let [names (newer-namespaces dirs @timestamp-atom)]
      (when (seq names)
	(reset! timestamp-atom (System/currentTimeMillis))
	(println)
	(println "======================================================================")
	(println "Reloading" (join ", " names))
	(doseq [n names] (remove-ns n))
	(doseq [n names] (require n :reload))
	(println "Running examples at" (java.util.Date.))
	(reporter (mapcat run-tests (mapcat get-tests (all-ns))))
	(println "\nDone.")))
    (catch Throwable t
      (println "ERROR:" t)
      (.printStackTrace t))))

(defn start [dirs & options]
  (let [dirs (map file dirs)
	{:keys [reporter delay], :or {delay 500, reporter report}} options
	last-run-timestamp (atom 0)
	runner #(reload-and-run dirs last-run-timestamp reporter)]
    (doto (ScheduledThreadPoolExecutor. 1)
      (.scheduleWithFixedDelay runner 0 delay TimeUnit/MILLISECONDS))))

(defn -main [& args]
  (start args))
