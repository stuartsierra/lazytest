(ns lazytest.fixture
  "Fixtures provide context and/or state in which to run one or more
  tests.")

(defprotocol Fixture
  (setup [this] "Sets up and returns the state provided by this fixture.")
  (teardown [this] "Cleans up state created by this fixture."))

(defn fixture? [x]
  (extends? Fixture (type x)))

(deftype FunctionFixture [f]
  Fixture
  (setup [this] (f))
  (teardown [this] nil))

(defn function-fixture
  "Returns a fixture whose setup method returns the result of calling
  f. The fixture's teardown method does nothing."
  [f]
  {:pre [(fn? f)]}
  (FunctionFixture. f))

(deftype MemoizedFixture [f cache]
  Fixture
  (setup [this]
	 (swap! cache (fn [value]
			(if (= ::unset value) (f) value))))
  (teardown [this] nil))

(defn memoized-fixture
  "Returns a fixture whose setup method calls, returns, and caches the
  value of f. The fixture's teardown method does nothing."
  [f]
  {:pre [(fn? f)]}
  (MemoizedFixture. f (atom ::unset)))
