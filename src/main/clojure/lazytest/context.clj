(ns lazytest.context
  "Contexts provide context and/or state in which to run one or more
  tests.")

(defn context
  "Creates a context with the given setup and teardown functions."
  [setup-fn teardown-fn]
  {:pre [(fn? setup-fn) (fn? teardown-fn)]}
  (with-meta {:setup setup-fn, :teardown teardown-fn} {:type ::context}))

(defn context?
  "Returns true if x is a context."
  [x]
  (isa? (type x) ::context))

(defn memoized-context
  "Returns a context whose setup method calls, returns, and caches the
  value of f. The context's teardown method clears the cache."
  [f]
  {:pre [(fn? f)]}
  (let [cache (atom ::unset)]
    (context (fn [] (swap! cache
			   (fn [value]
			     (if (= ::unset value) (f) value))))
	     (fn [] (reset! cache ::unset)))))

(defn setup 
  "Invokes the setup function of context c."
  [c]
  {:pre [(context? c)]}
  ((:setup c)))

(defn teardown
  "Invokes the teardown function of context c."
  [c]
  {:pre [(context? c)]}
  ((:teardown c)))

(defn add-context
  "Adds context c to metadata of iobj."
  [iobj c]
  {:pre [(context? c)]}
  (vary-meta iobj update-in [::contexts] conj c))

(defn setup-contexts
  "Run setup functions of all contexts in iobj's metadata."
  [iobj]
  (doseq [c (::contexts (meta iobj))]
    (setup c)))

(defn teardown-contexts
  "Run teardown functions of all contexts in iobj's metadata."
  [iobj]
  (doseq [c (reverse (::contexts (meta iobj)))]
    (teardown c)))

(defn using-contexts
  "Returns a no-argument function applying f to the values returned by
  the setup function of contexts cs.  Ensures that the teardown
  functions of all contexts are called.

  The returned function has the same metadata as f."
  [f & cs]
  {:pre [(fn? f) (every? context? cs)]}
  (with-meta (fn [] (try (apply f (map setup cs))
			 (finally (dorun (map teardown cs)))))
    (meta f)))
