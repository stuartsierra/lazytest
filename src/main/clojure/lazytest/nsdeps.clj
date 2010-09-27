(ns lazytest.nsdeps
  "Parsing namespace declarations for dependency information."
  (:use [clojure.set :only (union)]))

(defn- append-prefix [prefix form]
  (symbol (str (when prefix (str prefix ".")) form)))

(defn- filter-forms [forms]
  (map #(if (and (coll? %)
                 (= (second %) :only)) (first %) %)
       forms))

(defn- deps-from-libspec [prefix form]
  (cond (list? form) (apply union
                            (map #(deps-from-libspec
                                   (append-prefix prefix (first form)) %)
                                 (filter-forms (rest form))))
	(vector? form) (deps-from-libspec prefix (first form))
	(symbol? form) #{(append-prefix prefix form)}
	:else (throw
               (IllegalStateException.
                (pr-str "form must be a list, vector or symbol:" form
                        (when prefix (str "prefix = " prefix)))))))

(defn- deps-from-ns-form [form]
  (when (and (list? form)
	     (contains? #{:use :require} (first form)))
    (apply union (map #(deps-from-libspec nil %) (rest form)))))

(defn deps-from-ns-decl
  "Given a (quoted) ns declaration, returns a set of symbols naming
  the dependencies of that namespace.  Handles :use and :require clauses."
  [decl]
  (apply union (map deps-from-ns-form decl)))

