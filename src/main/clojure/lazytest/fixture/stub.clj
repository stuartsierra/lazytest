(ns lazytest.fixture.stub
  "A Stub is a special kind of Fixture that rebinds a Var in the
  current dynamic environment."
  (:use [lazytest.fixture :only (Fixture)]))

(deftype DynamicBindingStub [v value]
  Fixture
  (setup [this] (push-thread-bindings {v value}))
  (teardown [this] (pop-thread-bindings)))

(deftype RootBindingStub [v new-value]
  Fixture
  (setup [this]
    (alter-meta! v (fn [m]
		     (when (contains? m this)
		       (throw (IllegalStateException.
			       (str "This global stub is already active for " v))))
		     (let [old-value (var-get v)]
		       (alter-var-root v (constantly new-value))
		       (assoc m this old-value)))))
  
  (teardown [this]
    (alter-meta! v (fn [m]
		     (when (not (contains? m this))
		       (throw (IllegalStateException.
			       (str "This global stub is not active for " v))))
		     (alter-var-root v (constantly (get m this)))
		     (dissoc m this)))))

(defn stub
  "Returns a Fixture that creates a thread-local binding of Var v to
  new-value."
  [v new-value]
  {:pre [(var? v)]}
  (DynamicBindingStub. v new-value))

(defn global-stub
  "Returns a Fixture that modifies the root binding if Var v. The Var
  must have a root binding before 'setup' is called. Use 'stub'
  instead unless you need to stub the Var on all threads."
  [v new-value]
  {:pre [(var? v)]}
  (RootBindingStub. v new-value))
