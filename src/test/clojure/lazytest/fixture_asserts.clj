(ns lazytest.fixture-asserts
  (:use lazytest.fixture))

(assert (nil? (setup nil)))
(assert (= 1 (setup 1)))
(assert (= "Hello, World!" (setup "Hello, World!")))
(assert (= :a (setup :a)))
(assert (= [] (setup [])))

(assert (nil? (teardown nil)))
(assert (nil? (teardown 1)))
(assert (nil? (teardown "Hello, World!")))
(assert (nil? (teardown :a)))
(assert (nil? (teardown [])))

(let [f (reify Fixture
	       (setup [this] :state)
	       (teardown [this] :done))]
  (assert (= :state (setup f)))
  (assert (= :done (teardown f))))
