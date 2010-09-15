(ns lazytest.loader
  "Loading namespaces and managing dependencies."
  (:use [lazytest.dependency :only (graph depend)]
	[clojure.contrib.find-namespaces :only (find-clojure-sources-in-dir
						read-file-ns-decl)]
	[clojure.java.io :only (file)]))

(defn find-sources
  [dirs]
  {:pre [(coll? dirs)
	 (every? (fn [d] (instance? java.io.File d)) dirs)]}
  (mapcat find-clojure-sources-in-dir dirs))

(defn namespace-and-deps-for-file [f]
  (let [decl (read-file-ns-decl f)]
    (when decl
      [(second decl)
       (deps-from-ns-decl decl)])))

(defn newer-sources [dirs timestamp]
  (filter #(> (.lastModified %) timestamp) (find-sources dirs)))

(defn newer-namespaces [dirs timestamp]
  (remove nil? (map namespace-and-deps-for-file (newer-sources dirs timestamp))))

(defn dep-graph [names-and-deps]
  (reduce (fn [g [n deps]] (if (seq deps)
			     (apply depend g n deps)
			     g))
	  (graph) names-and-deps))
