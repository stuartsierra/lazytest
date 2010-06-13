(ns com.stuartsierra.lazytest.watch
  (:use [com.stuartsierra.lazytest.attach :only (all-groups)]
	[com.stuartsierra.lazytest.plan :only (flat-plan)]
	[com.stuartsierra.lazytest.run :only (run)]
	[com.stuartsierra.lazytest.report :only (report)]
	[clojure.contrib.find-namespaces
	 :only (find-clojure-sources-in-dir
		read-file-ns-decl)]
	[clojure.java.io :only (file)])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)))

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
	(println "Reloading" names)
	(doseq [n names] (remove-ns n))
	(doseq [n names] (require n))
	(println "Running examples at" (java.util.Date.))
	(reporter (run (flat-plan (all-groups))))))
    (catch Exception e
      (println "ERROR:" e))))

(defn start [dirs & options]
  (let [dirs (map file dirs)
	{:keys [reporter delay], :or {delay 500, reporter report}} options
	last-run-timestamp (atom 0)
	runner #(reload-and-run dirs last-run-timestamp reporter)]
    (doto (ScheduledThreadPoolExecutor. 1)
      (.scheduleWithFixedDelay runner 0 delay TimeUnit/MILLISECONDS))))
