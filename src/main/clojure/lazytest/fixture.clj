(ns lazytest.fixture
  "Fixtures provide context and/or state in which to run one or more
  tests.

  Any object can be a Fixture; unless otherwise specified, its setup
  method returns the object itself.")

(defprotocol Fixture
  (setup [this] "Sets up and returns the state provided by this fixture.")
  (teardown [this] "Cleans up state created by this fixture."))

(extend java.lang.Object
  Fixture
  {:setup identity
   :teardown (constantly nil)})

(extend nil
  Fixture
  {:setup identity
   :teardown (constantly nil)})
