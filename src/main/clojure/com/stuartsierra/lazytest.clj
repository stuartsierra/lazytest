(ns com.stuartsierra.lazytest
  (:use [com.stuartsierra.lazytest.contexts
         :only (context?)]
        [com.stuartsierra.lazytest.arguments
         :only (or-nil)]))


;;; Examples and ExampleGroups

(defrecord ExampleGroup [contexts examples])

(defn new-example-group
  "Creates an ExampleGroup."
  ([contexts examples] (new-example-group contexts examples nil))
  ([contexts examples metadata]
     {:pre [(or-nil vector? contexts)
            (or-nil vector? examples)
            (every? context? contexts)
            (every? fn? examples)
            (or-nil map? metadata)]
      :post [(isa? (type %) ExampleGroup)]}
     (ExampleGroup. contexts examples nil metadata)))


;;; Public API

(defmacro thrown?
  "Returns true if body throws an instance of class c."
  [c & body]
  `(try ~@body false
        (catch ~c e# true)))

(defmacro thrown-with-msg?
  "Returns true if body throws an instance of class c whose message
  matches re (with re-find)."
  [c re & body]
  `(try ~@body false
        (catch ~c e# (re-find ~re (.getMessage e#)))))

(defmacro ok?
  "Returns true if body does not throw anything."
  [& body]
  `(do ~@body true))

