(ns lazytest.context
  "Contexts provide context and/or state in which to run one or more
  tests.")

(defprotocol Context
  (setup [this] "Sets up and returns the state provided by this context.")
  (teardown [this] "Cleans up state created by this context."))

(defn context?
  "Returns true if x is a context, i.e., it implements the Context protocol."
  [x]
  (extends? Context (type x)))

(defrecord ConstantContext [value]
  Context
  (setup [this] value)
  (teardown [this] nil))

(defn constant-context
  "Returns a context whose setup method always returns value.  The
  context's teardown method does nothing."
  [value]
  (ConstantContext. value))

(defrecord FunctionContext [f]
  Context
  (setup [this] (f))
  (teardown [this] nil))

(defn function-context
  "Returns a context whose setup method returns the result of calling
  f. The context's teardown method does nothing."
  [f]
  {:pre [(fn? f)]}
  (FunctionContext. f))

(defrecord MemoizedContext [f cache]
  Context
  (setup [this]
	 (swap! cache (fn [value]
			(if (= ::unset value) (f) value))))
  (teardown [this]
	    (reset! cache ::unset)))

(defn memoized-context
  "Returns a context whose setup method calls, returns, and caches the
  value of f. The context's teardown method clears the cache."
  [f]
  {:pre [(fn? f)]}
  (MemoizedContext. f (atom ::unset)))

(defn sequential-context
  "Returns a sequential context whose setup method calls f, which
  should return a finite sequence of values."
  [f]
  (vary-meta (function-context f) assoc :sequential true))
