(ns lazytest.tracker
  "Keeping track of which namespaces have changed and need to be reloaded"
  (:use [lazytest.dependency :only (graph depend dependents remove-key)]
	[lazytest.nsdeps :only (deps-from-ns-decl)]
	[clojure.contrib.find-namespaces :only (find-clojure-sources-in-dir
						read-file-ns-decl)]
	[clojure.set :only (union)]))

(defn- find-sources
  [dirs]
  {:pre [(every? (fn [d] (instance? java.io.File d)) dirs)]}
  (mapcat find-clojure-sources-in-dir dirs))

(defn- newer-sources [dirs timestamp]
  (filter #(> (.lastModified %) timestamp) (find-sources dirs)))

(defn- newer-namespace-decls [dirs timestamp]
  (remove nil? (map read-file-ns-decl (newer-sources dirs timestamp))))

(defn- add-to-dep-graph [dep-graph namespace-decls]
  (reduce (fn [g decl]
	    (let [nn (second decl)
		  deps (deps-from-ns-decl decl)]
	      (apply depend g nn deps)))
	  dep-graph namespace-decls))

(defn- remove-from-dep-graph [dep-graph new-decls]
  (apply remove-key dep-graph (map second new-decls)))

(defn- update-dependency-graph [dep-graph new-decls]
  (-> dep-graph
      (remove-from-dep-graph new-decls)
      (add-to-dep-graph new-decls)))

(defn- affected-namespaces [changed-namespaces old-dependency-graph]
  (apply union (set changed-namespaces) (map #(dependents old-dependency-graph %)
					     changed-namespaces)))

(defn tracker [dirs initial-timestamp]
  "Returns a no-arg function which, when called, returns a set of
  namespaces that need to be reloaded, based on file modification
  timestamps and the graph of namespace dependencies."
  {:pre [(integer? initial-timestamp)
	 (every? (fn [f] (instance? java.io.File f)) dirs)]}
  (let [timestamp (atom initial-timestamp)
	dependency-graph (atom (graph))]
    (fn []
      (let [then @timestamp
	    now (System/currentTimeMillis)
	    new-decls (newer-namespace-decls dirs then)
	    new-names (map second new-decls)
	    affected-names (affected-namespaces new-names @dependency-graph)]
	(reset! timestamp now)
	(swap! dependency-graph update-dependency-graph new-decls)
	affected-names))))
