(ns lazytest.reload
  (:require [clojure.set :as set]
	    [clojure.string :as s]))

(defn remove-from-loaded-libs
  "Removes symbols from the set in the private Ref clojure.core/*loaded-libs*"
  [& syms]
  (dosync
   (alter @#'clojure.core/*loaded-libs*
	  set/difference (set syms))))

(defn find-source-file
  "Attempts to find the source .clj file for the namespace with the
  given name, returns a java.io.File. Returns nil if the file cannot
  be found or is inside a JAR."
  [sym]
  (let [path (-> sym name (s/replace "-" "_") (s/replace "." "/") (str ".clj"))]
    (when-let [u (.. Thread currentThread getContextClassLoader (getResource path))]
      (when (= "file" (.getProtocol u))
	(when-let [p (.getPath u)]
	  (java.io.File. p))))))

(defn touch-source-file
  "Attempts to find the .clj source file for the namespace with the
  given name, and sets its modification time to the current time.
  Ensures future loads will use the .clj source file and not the
  AOT-compiled .class file."
  [sym]
  (when-let [f (find-source-file sym)]
    (.setLastModified f (System/currentTimeMillis))))

(defn reload
  "Removes all namespaces named by symbols, then reloads them."
  [& symbols]
  {:pre (every? symbol? symbols)}
  (doseq [sym symbols] (remove-ns sym))
  (apply remove-from-loaded-libs symbols)
  (doseq [sym symbols] (touch-source-file sym))
  (apply require symbols))
