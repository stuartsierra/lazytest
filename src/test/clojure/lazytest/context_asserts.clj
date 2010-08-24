(ns lazytest.context-asserts
  (:use lazytest.context))

(let [f (reify Context
	       (setup [this] :state)
	       (teardown [this] :done))]
  (assert (= :state (setup f)))
  (assert (= :done (teardown f))))
