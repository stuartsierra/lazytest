(ns #^{:doc "Compatibility layer for old clojure.test"}
  com.stuartsierra.lazytest.compat
  (:require [com.stuartsierra.lazytest :as lazy]
            [clojure.test :as old]))

(defmacro are [& args]
  `(old/are ~@args))

(defmacro is
  ([form & other] `(assert ~form)))

(defmacro deftest [name & body]
  `(lazy/deftest ~name []
     (do ~@body)))

(defmacro testing [string & body]
  `(do ~@body))
