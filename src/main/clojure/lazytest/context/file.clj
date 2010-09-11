(ns lazytest.context.file
  (:use [lazytest.context.stateful :only (stateful-fn-context)]
	[clojure.java.io :only (file)])
  (:import (java.io File)))

(defn temp-file
  "Returns a stateful context that creates a temporary file.  After
  setup, an empty file named with the given prefix and suffix (both
  optional) will exist.  The state of the context is the java.io.File.
  The file will be deleted during teardown.

  If suffix is a file extension, it should include the leading period,
  e.g., \".txt\"

  If dir is specified (String or File), the temporary file will be
  created in that directory; otherwise it will be in the
  system-default temporary directory."
  ([]
     (temp-file "temp" nil nil))
  ([prefix]
     (temp-file prefix nil nil))
  ([prefix suffix]
     (temp-file prefix suffix nil))
  ([prefix suffix dir]
     {:pre [(string? prefix)
	    (or (nil? suffix) (string? suffix))]}
     (let [dir (if (nil? dir) dir (file dir))]
       (stateful-fn-context
	(fn [] (File/createTempFile prefix suffix dir))
	(fn [f] (.delete f))))))

(defn create-temp-dir
  "Creates a directory with given name prefix, inside directory dir (a
  java.io.File, may be nil for default temporary directory)."
  [prefix dir]
  (let [f (File/createTempFile prefix "" dir)]
    (assert (.delete f))
    (assert (.mkdirs f))
    f))

(defn delete-dir
  "Recursively deletes directory f (java.io.File)"
  [f]
  (doseq [x (reverse (file-seq f))]
    (.delete x)))

(defn temp-dir
  "Returns a stateful context that creates a temporary directory.
  After setup, an empty directory named with the given
  prefix (optional) will exist.  The state of the context is the
  java.io.File.  The directory and all its contents will be
  recursively deleted during teardown.

  If dir is specified (String or File), the temporary directory will
  be created in that directory; otherwise it will be in the
  system-default temporary directory."
  ([]
     (temp-dir "temp" nil))
  ([prefix]
     (temp-dir prefix nil))
  ([prefix dir]
     {:pre [(string? prefix)]}
     (let [dir (if (nil? dir) dir (file dir))]
       (stateful-fn-context
	(fn [] (create-temp-dir prefix dir))
	delete-dir))))
