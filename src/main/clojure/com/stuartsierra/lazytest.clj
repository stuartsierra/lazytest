(ns com.stuartsierra.lazytest
  (:use [com.stuartsierra.lazytest.arguments
         :only (get-arg get-options standard-metadata firsts seconds)]
        [com.stuartsierra.lazytest.groups
         :only (group group-examples)]
        [com.stuartsierra.lazytest.contexts
         :only (context)]
        [com.stuartsierra.lazytest.attach
         :only (add-group!)]))

(declare describe description)

(defn- description-form? [form]
  (and (seq? form)
       (= #'description (resolve (first form)))))

(defn- split-examples-and-subgroups [args]
  [(remove description-form? args)
   (vec (filter description-form? args))])

(defmacro description
  "Returns an example group.  body begins with an optional doc string,
  followed by key-value options, followed by example expressions
  and/or nested groups.

  Option :using is followed by a vector of symbol-context pairs.  All
  examples in the group will run with the symbol bound to the state
  returned by that context.

  Option :given is followed by a vector of symbol-expression pairs.
  All examples in the group will run with the symbol bound to the
  state returned by that expression."
  [& body]
  (let [[docstring body] (get-arg string? body)
        [opts body] (get-options body)
        givens (interleave (firsts (:given opts))
                           (map (fn [e] `(context [] ~e))
                                (seconds (:given opts))))
        bindings (vec (concat (:using opts) givens))
        [examples subgroups] (split-examples-and-subgroups body)]
    `(with-meta (group ~bindings (group-examples ~@examples) ~subgroups)
       ~(standard-metadata &form docstring))))

(defn- replace-nested-describe [form]
  (if (and (seq? form)
           (= #'describe (resolve (first form))))
    (if (symbol? (second form))
      (throw (IllegalArgumentException.
              "Nested descriptions cannot be attached to Vars."))
      `(description ~@(rest form)))
    form))

(defmacro describe
  "Creates an example group and attaches it to a target.  If first
  argument is a symbol, the group will be attached to that Var.  If
  first argument is a string, the group will be attached to the
  current namespace.  Remaining arguments are like 'description'."
  [& body]
  (let [[target body] (get-arg symbol? body)
        [docstring body] (get-arg string? body)
        body (map replace-nested-describe body)
        target-ref (if target `(var ~target) `*ns*)]
    `(add-group! ~target-ref
                 (with-meta (description ~@body)
                   (assoc ~(standard-metadata &form docstring)
                     :target ~target-ref)))))

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

