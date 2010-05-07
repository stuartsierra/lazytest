(ns com.stuartsierra.lazytest
  (:use [com.stuartsierra.lazytest.arguments
         :only (get-arg standard-metadata)]
        [com.stuartsierra.lazytest.groups
         :only (description declare-subgroup-form!)]
        [com.stuartsierra.lazytest.contexts
         :only (context)]
        [com.stuartsierra.lazytest.attach
         :only (add-group!)]))

(declare describe)

(defn- replace-nested-describe
  "If form begins with 'describe', replace it with 'description'.
  Throws an exception if first argument to the 'describe' form is a
  symbol."
  [form]
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
