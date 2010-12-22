(ns lazytest.nsdeps
  "Parsing namespace declarations for dependency information."
  (:use [clojure.set :only (union)]))

(defn- prepend [prefix sym]
  (symbol (str (when prefix (str prefix "."))
               sym)))

(defn- deps-from-libspec [prefix form]
  (cond (coll? form) (if (or prefix (some keyword? form) (= 1 (count form)))
                       (deps-from-libspec prefix (first form))
                       (apply union (map #(deps-from-libspec (prepend prefix (first form)) %)
                                         (rest form))))
        (symbol? form) #{(prepend prefix form)}
        (keyword? form) #{}
        :else (throw (IllegalArgumentException.
                      (pr-str "Unparsable namespace form:" form)))))

(defn- deps-from-ns-form [form]
  (when (and (list? form)
	     (contains? #{:use :require} (first form)))
    (apply union (map #(deps-from-libspec nil %) (rest form)))))

(defn deps-from-ns-decl
  "Given a (quoted) ns declaration, returns a set of symbols naming
  the dependencies of that namespace.  Handles :use and :require clauses."
  [decl]
  (apply union (map deps-from-ns-form decl)))
