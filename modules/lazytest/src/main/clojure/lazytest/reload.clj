(ns lazytest.reload
  (:require [clojure.set :as set]
	    [clojure.string :as str]))

(defn- remove-from-loaded-libs
  "Removes symbols from the set in the private Ref clojure.core/*loaded-libs*"
  [& syms]
  (dosync
   (alter @#'clojure.core/*loaded-libs*
	  set/difference (set syms))))

(defn- basename
  "Converts a namespace name symbol to a String file path, without
  extension."
  [sym]
  (-> sym name (str/replace "-" "_") (str/replace "." "/")))

(defn- find-file-on-classpath
  "Given a path as a string, searches for that file on the classpath,
  returns a java.io.File. Returns nil if the file cannot be found or
  is embedded in a JAR."
  [path]
  (when-let [u (.. Thread currentThread getContextClassLoader (getResource path))]
    (when (= "file" (.getProtocol u))
      (when-let [p (.getPath u)]
	(java.io.File. p)))))

(defn- find-source-file
  "Attempts to find the source .clj file for the namespace with the
  given name, returns a java.io.File. Returns nil if the file cannot
  be found or is inside a JAR."
  [sym]
  (find-file-on-classpath (str (basename sym) ".clj")))

(defn- has-class-file?
  "Returns true if the namespace has an AOT-compiled .class file on
  the classpath."
  [sym]
  (boolean (.. Thread currentThread getContextClassLoader
	       (getResource (str (basename sym) ".class")))))

(defn- ensure-source-file-newer
  "If the given namespace name has an AOT-compiled .class file,
  attempts to find the .clj source file and set its modification time
  to the current time.  Ensures future loads will use the .clj source
  file and not the AOT-compiled .class file."
  [sym]
  (when (has-class-file? sym)
    (if-let [f (find-source-file sym)]
      (when-not (.setLastModified f (System/currentTimeMillis))
	(println "WARNING: failed to update timestamp on source file for namespace" sym))
      (println "WARNING: failed to find normal source file for namespace" sym))))

(defn reload
  "Removes all namespaces named by symbols, then reloads them."
  [& symbols]
  {:pre (every? symbol? symbols)}
  ;(doseq [sym symbols] (remove-ns sym))
  ;(apply remove-from-loaded-libs symbols)
  (doseq [sym symbols] (ensure-source-file-newer sym))
  (apply require (conj symbols :reload)))
