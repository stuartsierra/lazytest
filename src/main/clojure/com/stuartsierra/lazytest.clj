(ns com.stuartsierra.lazytest
  (:use [com.stuartsierra.lazytest.arguments
         :only (get-arg standard-metadata)]
        [com.stuartsierra.lazytest.groups
         :only (description group-examples)]
        [com.stuartsierra.lazytest.attach
         :only (set-descriptor-name)]))

(defmacro describe
  "Creates an example group and attaches it to a target.  If first
  argument is a symbol, the group will be attached to that Var.  If
  first argument is a string, the group will be attached to the
  current namespace.  Remaining arguments are like 'description'."
  [& body]
  (let [[target body] (get-arg symbol? body)
        [docstring body] (get-arg string? body)
        description `(with-meta (description ~@body)
                       (assoc ~(standard-metadata &form docstring)
                         :target ~target))
        sym (gensym "describe-auto-")
        qsym (symbol (name (ns-name *ns*)) (name sym))]
    `(let [~(with-meta (gensym) {::in-describe true}) nil]
       ~(when target
          `(set-descriptor-name (var ~target) '~qsym))
       ~(if (some #(::in-describe (meta %)) (keys &env))
          description
          `(intern *ns* '~sym ~description)))))

(defmacro it
  "Creates a group of examples.  For use within 'describe', uses the
  contexts and givens.  Body is a series of expressions, each of which
  will be compiled to a single example.  A string in body will be
  attached as a doc string on the following expression."
  [& body]
  `(group-examples ~@body))

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
