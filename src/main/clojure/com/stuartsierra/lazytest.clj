(ns com.stuartsierra.lazytest
  (:use [clojure.contrib.find-namespaces
         :only (find-namespaces-in-dir)]))

;;; PROTOCOLS

(defprotocol TestInvokable
  (invoke-test [t active]))

(defprotocol TestResult
  (success? [r] "True if this result and all its children passed.")
  (pending? [r] "True if this is the result of an empty test.")
  (error? [r] "True if this is the result of a thrown exception.")
  (container? [r] "True if this is a container for other results."))


;;; Results

(deftype TestResultContainer [source children]
  clojure.lang.IPersistentMap
  TestResult
    (success? [this] (every? success? children))
    (pending? [this] (if (seq children) false true))
    (error? [this] false)
    (container? [this] true))

(deftype TestPassed [source states]
  clojure.lang.IPersistentMap
  TestResult
    (success? [this] true)
    (pending? [this] false)
    (error? [this] false)
    (container? [this] false))

(deftype TestFailed [source states]
  clojure.lang.IPersistentMap
  TestResult
    (success? [this] false)
    (pending? [this] false)
    (error? [this] false)
    (container? [this] false))

(deftype TestThrown [source states throwable]
  clojure.lang.IPersistentMap
  TestResult
    (success? [this] false)
    (pending? [this] false)
    (error? [this] true)
    (container? [this] false))


;;; Contexts

(deftype Context [parents before after])

(defn- open-context
  "Opens context c, and all its parents, unless it is already active."
  [active c]
  (let [active (reduce open-context active (:parents c))
        states (map active (:parents c))]
    (if-let [f (:before c)]
      (assoc active c (or (active c) (apply f states)))
      active)))

(defn- close-context
  "Closes context c and removes it from active."
  [active c]
  (let [states (map active (:parents c))]
    (when-let [f (:after c)]
      (apply f (active c) states))
    (let [active (reduce close-context active (:parents c))]
      (dissoc active c))))

(defmacro defcontext
  "Defines a context.
  decl => docstring? [bindings*] before-body* after-fn?
  after-fn => :after [state] after-body*"
  [name & decl]
  (let [m {:name name, :ns *ns*, :file *file*, :line @Compiler/LINE}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        bindings (first decl)
        bodies (next decl)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [pairs (partition 2 bindings)
          locals (vec (map first pairs))
          contexts (vec (map second pairs))
          before (take-while #(not= :after %) bodies)
          after (next (drop-while #(not= :after %) bodies))
          before-fn `(fn ~locals ~@before)]
      (when after (assert (vector? (first after))))
      (let [after-fn (when after
                       `(fn ~(vec (concat (first after) locals))
                          ~@after))]
        `(def ~name (Context ~contexts ~before-fn ~after-fn '~m nil))))))

(defn- has-after?
  "True if Context c or any of its parents has an :after function."
  [c]
  (or (:after c)
      (some has-after? (:parents c))))

(defn- close-local-contexts [contexts merged active]
  (reduce close-context merged
          ;; Only close contexts that weren't active at start:
          (filter #(not (contains? active %))
                  (reverse contexts))))

;;; Assertion types

(deftype SimpleAssertion [pred]
  clojure.lang.IFn
    (invoke [this] (invoke-test this {}))
  TestInvokable
    (invoke-test [this active]
      (try
        (if (pred)
          (TestPassed this nil)
          (TestFailed this nil))
        (catch Throwable t
          (TestThrown this nil t)))))

(deftype ContextualAssertion [contexts pred]
  clojure.lang.IFn
    (invoke [this] (invoke-test this {}))
  TestInvokable
    (invoke-test [this active]
      (let [merged (reduce open-context active contexts)
            states (map merged contexts)]
        (try
         (if (apply pred states)
           (TestPassed this states)
           (TestFailed this states))
         (catch Throwable t (TestThrown this states t))
         (finally (close-local-contexts contexts merged active))))))


;;; Container types

(deftype SimpleContainer [children]
  clojure.lang.IFn
    (invoke [this] (invoke-test this {}))
  TestInvokable
    (invoke-test [this active]
      (try
       (TestResultContainer this (map #(invoke-test % active) children))
       (catch Throwable t
         (TestThrown this nil t)))))

(deftype ContextualContainer [contexts children]
  clojure.lang.IFn
    (invoke [this] (invoke-test this {}))
  TestInvokable
    (invoke-test [this active]
      (let [merged (reduce open-context active contexts)]
        (try 
         (let [results (map #(invoke-test % merged) children)]
           ;; Force non-lazy evaluation when contexts need closing:
           (when (some has-after? contexts) (dorun results))
           (TestResultContainer this results))
         (catch Throwable t (TestThrown this nil t))
         (finally (close-local-contexts contexts merged active))))))


(defn- firsts [coll]
  (vec (map first (partition 2 coll))))

(defn- seconds [coll]
  (vec (map second (partition 2 coll))))

;;; Public API

(defmacro is
  "A series of assertions.  Each assertion is a simple expression,
  which will be compiled into a function.  A string will be attached
  as :doc metadata on the following assertion."
  [& assertions]
  (let [givens (vec (filter #(::given (meta %)) (keys &env)))]
    (loop [r [], as assertions]
      (if (seq as)
        (let [[doc form nxt] (if (string? (first as))
                               [(first as) (second as) (nnext as)]
                               [nil (first as) (next as)])
              metadata {:doc doc,
                        :form form
                        :file *file*
                        :line (:line (meta form))
                        :locals givens}]
          (recur (conj r (if (seq givens)
                           `(ContextualAssertion
                             ~givens (fn ~givens ~form)
                             '~metadata nil)
                           `(SimpleAssertion
                             (fn [] ~form) '~metadata nil)))
                 nxt))
        `(SimpleContainer ~r {:generator 'is
                              :line ~(:line (meta &form))
                              :file *file*
                              :form '~&form}
                          nil)))))

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

(defmacro are
  "A series of assertions reusing a single expression.
  Creates a function of (fn argv expr).  Values will be partitioned
  into groups of the same size as argv.  The function will be applied
  to each group.

  Example:

      (are [x y z] (= (+ x y) z)
          2 2  4
          3 2  5
          8 -1 7)

      ;; Equivalent to:

      (is (= (+ 2 2) 4)
          (= (+ 3 2) 5)
          (= (+ 8 -1) 7))
"
  [argv expr & values]
  (let [givens (vec (filter #(::given (meta %)) (keys &env)))
        argc (count argv)
        sym (gensym "f")]
    (assert (vector? argv))
    (assert (zero? (rem (count values) argc)))
    `(let [~sym (fn ~argv ~expr)]
       (SimpleContainer ~(vec (map (fn [vs]
                                     (if (seq givens)
                                       `(ContextualAssertion
                                         ~givens
                                         (fn ~givens (~sym ~@vs))
                                         {:form '(~'are ~argv ~expr ~@vs)
                                          :file *file*
                                          :line ~(some #(:line (meta %)) vs)
                                          :locals '~givens}
                                         nil)
                                       `(SimpleAssertion
                                         (fn [] (~sym ~@vs))
                                         {:form '(~'are ~argv ~expr ~@vs)
                                          :file *file*
                                          :line ~(some #(:line (meta %)) vs)}
                                         nil)))
                                   (partition argc values)))
                        {:generator 'are
                         :file *file*
                         :line ~(:line (meta &form))
                         :form '~&form}
                        nil))))

(defmacro given 
  "Binds context states to locals.  bindings is a vector of
  name-value pairs, like let, where each value is a context created
  with defcontext."
   [bindings & body]
   {:pre [(vector? bindings)
          (even? (count bindings))]}
  (let [symbols (map #(with-meta % {::given true}) (firsts bindings))]
    `(let ~(vec (interleave symbols (seconds bindings)))
       ~@body)))

(defn- attributes
  "Reads optional name symbol and doc string from args,
  returns [m a] where m is a map containing keys
  [:name :doc :ns :file] and a is remaining arguments."
  [args]
  (let [m {:ns *ns*, :file *file*}
        m    (if (symbol? (first args)) (assoc m :name (first args)) m)
        args (if (symbol? (first args)) (next args) args)
        m    (if (string? (first args)) (assoc m :doc (first args)) m)
        args (if (string? (first args)) (next args) args)]
    [m args]))

(defn- options
  "Reads keyword-value pairs from args, returns [m a] where m is a map
  of keyword/value options and a is remaining arguments."
  [args]
  (loop [opts {}, as args]
    (if (and (seq as) (keyword? (first as)))
      (recur (assoc opts (first as) (second as)) (nnext as))
      [opts as])))

(defmacro spec
  "Creates a test container.
  decl   => name? docstring? option* child*

  name  => a symbol, will def a Var if provided.
  child => 'is' or 'given' or nested 'spec'.

  options => keyword/value pairs, recognized keys are:
    :contexts => vector of contexts to run only once for this container.
    :strategy => a test-running strategy."
  [& decl]
  (let [[m decl] (attributes decl)
        m (assoc m :line (:line (meta &form))
                 :generator `spec
                 :form &form)
        [opts decl] (options decl)
        {:keys [contexts strategy]} opts
        children (vec decl)
        sym (gensym "c")]
    `(let [~sym ~(if contexts
                    (do (assert (vector? contexts))
                        `(ContextualContainer ~contexts ~children '~m nil))
                    `(SimpleContainer ~children '~m nil))]
       ~(when (:name m) `(intern *ns* '~(:name m) ~sym))
       ~sym)))

(defmacro spec-do
  "Creates an assertion function consisting of arbitrary code.
  Passes if it does not throw an exception.  Use assert for value
  tests.

  decl    => name? docstring? [binding*] body*
  binding => symbol context

  name  => a symbol, will def a Var if provided.
"
  [& decl]
  (let [[m decl] (attributes decl)
        m (assoc m :line (:line (meta &form))
                 :generator `spec-do
                 :form &form)
        bindings (first decl)
        body (next decl)
        sym (gensym "c")]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [pairs (partition 2 bindings)
          locals (map first pairs)
          contexts (map second pairs)]
      `(let [~sym ~(if (seq contexts)
                     `(ContextualAssertion ~contexts (fn ~locals ~@body :ok)
                                           '~m nil)
                     `(SimpleAssertion (fn [] ~@body :ok) '~m nil))]
         ~(when (:name m) `(intern *ns* '~(:name m) ~sym))
         ~sym))))

(defmacro describe
  "Attaches :spec metadata to target (a Namespace or a Var).  body is
  the same as the body of the spec macro.

  By writing (describe *ns* ...) you can attach a single top-level
  spec container to the current namespace."
  [target & body]
  `(alter-meta! ~target assoc :spec (spec ~@body)))

(defn spec?
  "Returns true if x is a spec, meaning it satisfies the TestInvokable
  protocol."
  [x] (satisfies? TestInvokable x))

(defn find-spec
  "Finds and returns a spec for x.

  If x is a ...

    Var/namespace with :spec metadata: recurses on that;

    Symbol: recurses on namespace with that name 
    (must be loaded already, see load-spec);

    Namespace: recurses on all Vars in the namespace;

    Collection: recurses on all elements;

    Spec object: returns it.

  otherwise returns nil."
  [x]
  (cond (spec? x) x

        (:spec (meta x))
        (find-spec (:spec (meta x)))

        (symbol? x)
        (find-spec (find-ns x))

        (instance? clojure.lang.Namespace x)
        ;; Omit the *1/*2/*3 REPL vars, in case they are specs:
        (when-let [s (find-spec (filter #(not (#{#'*1 #'*2 #'*3} %))
                                        (vals (ns-interns x))))]
          (vary-meta s assoc
                     :name (ns-name x)
                     :comment (str "Generated from all Vars in namespace "
                                   (ns-name x) " by find-spec.")))

        (var? x)
        (let [value (try (var-get x) (catch Exception e nil))]
          (if (spec? value)
            value
            (let [m (meta x)]
              (when-let [t (:test m)]
                (SimpleAssertion t {:comment (str "Generated from :test metadata on "
                                                  x " by find-spec.")
                                    :generator `find-spec
                                    :name (:name m)
                                    :ns (:ns m)
                                    :file (:file m)
                                    :line (:line m)}
                                 nil)))))

        (coll? x)
        ;; distinct so "main" and "spec" ns's don't load same specs
        (let [xs (distinct (filter identity (map find-spec x)))]
          (if (seq xs)
            (SimpleContainer (vec xs)
                             {:generator `find-spec
                              :comment "Generated from collection by find-spec."}
                             nil)))

        :else nil))

(defn load-spec
  "Like find-spec but loads namespaces with require.  options will be
  passed to require, such as :reload, or :reload-all.  Returns the
  spec or nil if none found.

  Argument may also be a java.io.File or String, which will load all
  namespaces in the named directory."
  [x & options]
  (cond (symbol? x)
        (do (apply require x options) (load-spec (the-ns x)))

        (string? x)
        (load-spec (java.io.File. x))

        (instance? java.io.File x)
        (if (.isDirectory x)
          (let [names (find-namespaces-in-dir x)]
            (doseq [n names] (apply require n options))
            (find-spec names))
          (throw (IllegalArgumentException.
                  "File argument to load-spec must be a directory")))

        (coll? x)
        (let [xs (distinct (filter identity (map #(apply load-spec % options) x)))]
          (if (seq xs)
            (SimpleContainer (vec xs)
                             {:generator `load-spec
                              :comment "Generated from collection by load-spec."}
                             nil)))

        :else (find-spec x)))

(defn run-spec
  "Like load-spec but executes the specs once they are found."
  [x & options]
  (when-let [s (apply load-spec x options)]
    (s)))
