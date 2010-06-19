(ns lazytest.describe
  (:use [lazytest.arguments :only (get-arg get-options seconds
				   standard-metadata nil-or
				   firsts)]
        [lazytest.describe.group :only (new-group group?)]
        [lazytest.contexts :only (new-context context?)]
        [lazytest.attach :only (add-group)]))

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
  {:pre [(nil-or vector? bindings)
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

(defmacro describe
  "Creates a description, a group of runnable examples, in the current
  namespace.  Arguments, in order, are:

    1. symbol: (optional) name of the Var that this group describes
    2. docstring: (optional) a documentation string for this group
    3. options: (optional) keyword-value pairs, see below
    4. body: any number of examples, see the 'it' macro

  Options are keyword-value pairs. Any options not specified here
  become metadata on the group.

    :given [name context, ...]

       The :given option is followed by a vector of name-value pairs
       that will become locals in each of the examples.  The value in
       each pair is a context created with defcontext, the name will
       be bound to the state returned by that context's 'before'
       function.  Argument destructuring is supported as with 'let'.

    :tags [keywords ...]

       The :tags option is followed by a vector of keywords, which
       will become tags on the group."
  [& args]
  (let [[sym args] (get-arg symbol? args)
	v (when sym (resolve sym))
	[doc args] (get-arg string? args)
	[opts body] (get-options args)
	docstring (str v (when v " ") doc)
	contexts (seconds (:given opts))
	tags (set (:tags opts))
	metadata (merge (standard-metadata &form docstring sym)
			{:tags tags, :var v}
			(dissoc opts :given :tags))]
    `(wrap-context ~(:given opts)
      (let [~(with-meta (gensym) {::in-describe true}) nil
	    body# (vector ~@body)
	    examples# (vec (filter fn? body#))
	    subgroups# (vec (filter group? body#))
	    new-group# (new-group ~contexts examples# subgroups# '~metadata)]
	(if ~(some #(::in-describe (meta %)) (keys &env))
	  new-group#
	  (add-group *ns* new-group#))))))

(defmacro it
  "Creates a test example within the body of the 'describe' macro.
  Arguments are a documentation string (optional), followed
  options (see below), followed by the code implementing the example.
  The example code must return logical true to indicate success, false
  or nil to indicate failure. A thrown exception also indicates
  failure.

  Options are keyword-value pairs. Any options not specified here
  become metadata on the example.

    :tags [keywords ...]

       The :tags option is followed by a vector of keywords, which
       will become tags on the example."
  [& args]
  (let [[doc args] (get-arg string? args)
	[opts body] (get-options args)
	tags (set (:tags opts))
	metadata (merge (standard-metadata &form doc)
			{:tags tags
			 :expr (last body)
			 :pending (empty? body)}
			(dissoc opts :tags))]
    `(with-meta (fn ~(find-local-args &env) ~@body)
       '~metadata)))

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
