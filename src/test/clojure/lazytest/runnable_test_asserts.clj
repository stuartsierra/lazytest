(ns lazytest.runnable-test-asserts
  (:use lazytest.runnable-test))

(let [t (reify RunnableTest
	       (run-tests [this] :result))]
  (assert (= :result (run-tests t))))

(let [t (reify RunnableTest
	       (run-tests [this]
			  (or (skip-or-pending this)
			      :result)))]
  (assert (= :result (run-tests t)))

  (let [pt (with-meta t {:pending :reason-for-pending})
	result (run-tests pt)]
    (assert (instance? lazytest.result.Pending result))
    (assert (= pt (:source result)))
    (assert (= :reason-for-pending (:reason result))))

  (let [st (with-meta t {:skip :reason-for-skip})
	result (run-tests st)]
    (assert (instance? lazytest.result.Skip result))
    (assert (= st (:source result)))
    (assert (= :reason-for-skip (:reason result)))))
