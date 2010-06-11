(ns com.stuartsierra.lazytest
  (:use [com.stuartsierra.lazytest.arguments :only (get-arg get-options seconds
						    standard-metadata)]
        [com.stuartsierra.lazytest.groups :only (new-group group?)]
        [com.stuartsierra.lazytest.attach :only (add-group all-groups)]
        [com.stuartsierra.lazytest.plan :only (flat-plan)]
	[com.stuartsierra.lazytest.run :only (run)]))



(defmacro describe [& args]
  (let [[sym args] (get-arg symbol? args)
	[doc args] (get-arg string? args)
	[opts body] (get-options args)
	contexts (seconds (:given opts))
	metadata (merge (standard-metadata &form doc sym)
			(dissoc opts :given))]
    `(let [body# (vector ~@body)
	   examples# (vec (filter fn? body#))
	   subgroups# (vec (filter group? body#))]
       (add-group *ns* (new-group ~contexts examples# subgroups# '~metadata)))))

(defmacro it [& args]
  (let [[doc args] (get-arg string? args)
	[opts body] (get-options args)
	metadata (merge (standard-metadata &form doc) opts)]
    `(with-meta (fn [] ~@body)
       '~metadata)))

(defn run-tests [& args]
  (run (apply flat-plan (all-groups) args)))

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
