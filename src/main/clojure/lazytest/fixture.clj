(ns lazytest.fixture
  "Fixtures provide context and/or state in which to run one or more
  tests.")

(defprotocol Fixture
  (setup [this] "Sets up and returns the state provided by this fixture.")
  (teardown [this] "Cleans up state created by this fixture."))

(defn fixture? [x]
  (extends? Fixture (type x)))

(deftype ConstantFixture [value]
  Fixture
  (setup [this] value)
  (teardown [this] nil))

(defn constant-fixture
  "Returns a fixture whose setup method always returns value and
  whose teardown method does nothing."
  [value]
  (ConstantFixture. value))

(deftype FunctionFixture [f]
  Fixture
  (setup [this] (f))
  (teardown [this] nil))

(defn function-fixture
  "Returns a fixture whose setup method calls and returns the value of
  f and whose teardown method does nothing."
  [f]
  {:pre [(fn? f)]}
  (FunctionFixture. f))
