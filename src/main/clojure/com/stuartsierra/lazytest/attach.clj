(ns com.stuartsierra.lazytest.attach)

(defn descriptor-name
  "Returns the symbol name of the descriptor assigned to iref, a
  namespace or Var."
  [iref]
  (::descriptor (meta iref)))

(defn set-descriptor-name
  "Sets the descriptor of iref (a namespace or Var) to sym."
  [iref sym]
  {:pre [(symbol? sym)
         (or (var? iref)
             (instance? clojure.lang.Namespace iref))]}
  (alter-meta! iref assoc ::descriptor sym))

(defn descriptor
  "Returns the descriptor, either a Var or a namespace, of iref."
  [iref]
  (let [sym (descriptor-name iref)]
    (if (namespace sym)
      ;; qualified symbol means it's a Var
      (resolve sym)
      ;; else, it's a namespace
      (the-ns sym))))
