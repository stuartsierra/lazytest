(ns com.stuartsierra.lazytest
  (:use [com.stuartsierra.lazytest.arguments :only (get-arg get-options seconds
						    standard-metadata or-nil
						    firsts)]
        [com.stuartsierra.lazytest.groups :only (new-group group?)]
        [com.stuartsierra.lazytest.contexts :only (new-context context?)]
        [com.stuartsierra.lazytest.attach :only (add-group all-groups)]
        [com.stuartsierra.lazytest.plan :only (flat-plan)]
	[com.stuartsierra.lazytest.run :only (run)]))

(let [counter (atom 0)]
  (defn- local-counter []
    (swap! counter inc)))

(defmacro with-context
  "Establishes a local binding of sym to the state returned by context
  c in all groups and examples found within body."
  [form c & body]
  `(let [~(with-meta (gensym) {::local true
			       ::order (local-counter)
			       ::arg form}) ~c]
     ~@body))

(defmacro wrap-context
  [bindings body]
  {:pre [(or-nil vector? bindings)
         (even? (count bindings))]}
  (if (seq bindings)
    `(with-context ~(first bindings) ~(second bindings)
       (wrap-context ~(vec (nnext bindings)) ~body))
    body))

(defn- find-locals
  "Returns a vector of locals bound by with-context in the
  environment."
  [env]
  (vec (sort-by #(::order (meta %))
                (filter #(::local (meta %)) (keys env)))))

(defn- find-local-args
  "Returns a vector of the function argument forms of locals bound by
  with-context in the environment."
  [env]
  (vec (map #(::arg (meta %)) (find-locals env))))

(defmacro context
  "Creates a context object, using existing local contexts.
  bindings is a vector from symbols to parent contexts"
  [bindings & bodies]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  (let [locals (vec (concat (find-locals &env) (firsts bindings)))
        contexts (vec (concat (find-locals &env) (seconds bindings)))
        [before-body divider after-body] (partition-by #(= :after %) bodies)
        before-fn `(fn ~locals ~@before-body)
        after-fn (when after-body
                   (let [[state & after-body] after-body]
                     (assert (symbol? state))
                     `(fn ~(apply vector state locals) ~@after-body)))]
    `(new-context ~contexts ~before-fn ~after-fn)))

(defmacro defcontext
  "Defines a named context.  body begins with an optional doc string,
  followed by key-value option pairs, followed by the before and after
  functions as in 'context'."
  [name & body]
  (let [[docstring body] (get-arg string? body)
        [options body] (get-options body)
        bindings (:given options [])]
    `(def ~name (with-meta (context ~bindings ~@body)
                  '~(standard-metadata &form docstring name)))))

(defmacro describe [& args]
  (let [[sym args] (get-arg symbol? args)
	[doc args] (get-arg string? args)
	[opts body] (get-options args)
	contexts (seconds (:given opts))
	metadata (merge (standard-metadata &form doc sym)
			(dissoc opts :given))]
    `(wrap-context ~(:given opts)
      (let [~(with-meta (gensym) {::in-describe true}) nil
	    body# (vector ~@body)
	    examples# (vec (filter fn? body#))
	    subgroups# (vec (filter group? body#))
	    new-group# (new-group ~contexts examples# subgroups# '~metadata)]
	(if ~(some #(::in-describe (meta %)) (keys &env))
	  new-group#
	  (add-group *ns* new-group#))))))

(defmacro it [& args]
  (let [[doc args] (get-arg string? args)
	[opts body] (get-options args)
	metadata (merge (standard-metadata &form doc) opts)]
    `(with-meta (fn ~(find-local-args &env) ~@body)
       '~metadata)))

(defn run-tests [& args]
  (run (apply flat-plan (all-groups) args)))

(defn clear-tests "Delete all tests in the current namespace"
  []
  (intern *ns* (with-meta '*lazytest-groups* {:private true}) #{}))

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
