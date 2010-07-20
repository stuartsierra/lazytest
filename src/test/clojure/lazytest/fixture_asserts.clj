(ns lazytest.fixture-asserts
  (:use lazytest.fixture))

(let [f (reify Fixture
	       (setup [this] :state)
	       (teardown [this] :done))]
  (assert (= :state (setup f)))
  (assert (= :done (teardown f))))
