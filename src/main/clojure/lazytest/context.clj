(ns lazytest.context
  "Contexts provide context and/or state in which to run one or more
  tests.")

(defn context [setup-fn teardown-fn]
  {:pre [(fn? setup-fn) (fn? teardown-fn)]}
  (with-meta {:setup setup-fn, :teardown teardown-fn} {:type ::context}))

(defn context?
  "Returns true if x is a context."
  [x]
  (= ::context (type x)))

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
