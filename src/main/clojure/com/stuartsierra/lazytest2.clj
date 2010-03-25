(ns com.stuartsierra.lazytest2)

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

(defprotocol TestInvokable
  (invoke-test [t active]))

(deftype TestResults [source children])

(deftype TestPassed [source states])

(deftype TestFailed [source states])

(deftype TestThrown [source states throwable])

(deftype ContextualAssertion [contexts pred] :as this
  TestInvokable
    (invoke-test [active]
      (let [merged (reduce open-context active contexts)
            states (map merged contexts)]
        (try
         (if (apply pred states)
           (TestPassed this states)
           (TestFailed this states))
         (catch Throwable t
           (TestThrown this states t))
         (finally
          (reduce close-context merged
                  ;; Only close contexts that weren't active at start:
                  (filter #(not (contains? active %))
                          (reverse contexts))))))))

(deftype SimpleAssertion [pred] :as this
  TestInvokable
    (invoke-test [active]
      (try
        (if (pred)
          (TestPassed this nil)
          (TestFailed this nil))
        (catch Throwable t
          (TestThrown this nil t)))))

(deftype SimpleContainer [children] :as this
  TestInvokable
  (invoke-test [active]
    (try
     (TestResults this (map #(invoke-test % active) children))
     (catch Throwable t
       (TestThrown this active t)))))

(defn- has-after?
  "True if Context c or any of its parents has an :after function."
  [c]
  (or (:after c)
      (some has-after? (:parents c))))

(deftype ContextualContainer [contexts children] :as this
  TestInvokable
  (invoke-test [active]
    (let [merged (reduce open-context active contexts)
          states (map merged contexts)]
      (try
       (let [results (map #(invoke-test % active) children)]
         ;; Force non-lazy evaluation when contexts need closing:
         (when (some has-after? contexts) (dorun results))
         (TestResults this results))
       (catch Throwable t
         (TestThrown this states t))
       (finally
        (reduce close-context merged
                ;; Only close contexts that weren't active at start:
                (filter #(not (contains? active %))
                        (reverse contexts))))))))

(defmacro should [& assertions]
  (loop [r [], as assertions]
    (if (seq as)
      (let [[doc form nxt]
            (if (string? (first as))
              [(first as) (second as) (nnext as)]
              [nil (first as) (next as)])]
        (recur (conj r `(SimpleAssertion
                         (fn [] ~form)
                         {:doc ~doc,
                          :form '~form
                          :file *file*,
                          :line ~(:line (meta form))}
                         nil))
               nxt))
      `(SimpleContainer ~r))))

(defmacro given [bindings & assertions]
  (assert (vector? bindings))
  (assert (even? (count bindings)))
  (let [pairs (partition 2 bindings)
        locals (vec (map first pairs))
        contexts (vec (map second pairs))]
    (loop [r [], as assertions]
      (if (seq as)
        (let [[doc form nxt]
              (if (string? (first as))
                [(first as) (second as) (nnext as)]
                [nil (first as) (next as)])]
          (recur (conj r `(ContextualAssertion
                           ~contexts
                           (fn ~locals ~form)
                           {:doc ~doc,
                            :locals '~locals
                            :form '~form
                            :file *file*,
                            :line ~(:line (meta form))}
                           nil))
                 nxt))
        `(SimpleContainer ~r)))))

(defn- attributes
  "Reads optional name symbol and doc string from args,
  returns [m a] where m is a map containing keys
  [:name :doc :ns :file :line] and a is remaining arguments."
  [args]
  (let [m {:ns *ns*, :file *file*, :line @Compiler/LINE}
        m    (if (symbol? (first args)) (assoc m :name (first args)))
        args (if (symbol? (first args)) (next args) args)
        m    (if (string? (first args)) (assoc m :doc (first args)))
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

(defmacro testing
  "Creates a test container.
  decl   => name? docstring? option* child*

  name  => a symbol, will def a Var if provided.
  child => 'should' or 'given' or nested 'testing'.

  options => keyword/value pairs, recognized keys are:
    :contexts => vector of contexts to run only once for this container.
    :strategy => a test-running strategy."
  [& decl]
  (let [[m decl] (attributes decl)
        [opts decl] (options decl)
        {:keys [contexts strategy]} opts
        children (vec decl)
        csym (gensym "c")]
    `(let [~csym ~(if contexts
                    (do (assert (vector? contexts))
                        `(ContextualContainer ~contexts ~children '~m nil))
                    `(SimpleContainer ~children '~m nil))]
       ~(when (:name m) `(intern *ns* '~(:name m) ~csym))
       ~csym)))
