(ns com.stuartsierra.lazytest-spec
  (:use [com.stuartsierra.lazytest
         :only (describe spec spec?
                         is given defcontext
                         context? ok? success?
                         pending? error? container?)]))

(defcontext dummy-context-1 [] 1)

(defcontext is-without-givens []
  (is (= 1 1) "hello" (= 1 2)))

(defcontext is-with-givens []
  (given [x dummy-context-1]
         (is (= x 1) "hello" (= x 2))))

(defcontext passing-assertion [it is-without-givens]
  (first (:children it)))

(defcontext failing-assertion [it is-without-givens]
  (second (:children it)))

(describe
 *ns* 
 (spec is-spec "The 'is' macro"
       (given [it is-without-givens]
              (spec "without any givens"
                    (is "should create a spec"
                        (spec? it)
              
                        "should create an invokable fn"
                        (instance? clojure.lang.IFn it)

                        "should attach :doc string metadata to assertions"
                        (= "hello" (:doc (meta (second (:children it)))))

                        "should attach :form metadata to assertions"
                        (= '(= 1 1) (:form (meta (first (:children it))))))

                    (spec "should have children"
                          (is (seq (:children it))

                              "that are specs"
                              (every? spec? (:children it))

                              "that are invokable IFns"
                              (every? #(instance? clojure.lang.IFn %) (:children it))

                              "as many as there are expressions"
                              (= 2 (count (:children it)))))))

       (given [it is-with-givens]
              (spec "with givens"
                    (is "should create a spec"
                        (spec? it)
              
                        "should create an invokable IFns"
                        (instance? clojure.lang.IFn it)

                        "should attach :doc string metadata to assertions"
                        (= "hello" (:doc (meta (second (:children it)))))

                        "should attach :form metadata to assertions"
                        (= '(= x 1) (:form (meta (first (:children it))))))

                    (spec "should have children"
                          (is (seq (:children it))

                              "that are specs"
                              (every? spec? (:children it))

                              "that are invokable IFns"
                              (every? #(instance? clojure.lang.IFn %) (:children it))

                              "as many as there are expressions"
                              (= 2 (count (:children it)))

                              (spec "with contexts"
                                    (is (seq (:contexts (first (:children it))))
                                        "that are Context objects"
                                        (every? context? (:contexts (first (:children it)))))))))))

 (spec assertions-spec "Assertions, when invoked"
       (given [it passing-assertion]
              (spec "should return an object"
                    (is (not (nil? (it)))
                        "that supports success?"
                        (ok? (success? (it)))
                        "that supports pending?"
                        (ok? (pending? (it)))
                        "that supports error?"
                        (ok? (error? (it)))
                        "that supports container?"
                        (ok? (container? (it)))))))

 (spec passing-assertion "A passing assertion"
       (given [it passing-assertion]
              (is "should return true for success?"
                  (true? (success? (it)))
                  "should return false for error?"
                  (false? (error? (it)))
                  "should return false for pending?"
                  (false? (pending? (it)))
                  "should return false for container?"
                  (false? (pending? (it))))))

  (spec failing-assertion "A failing assertion"
       (given [it failing-assertion]
              (is "should return false for success?"
                  (false? (success? (it)))
                  "should return false for error?"
                  (false? (error? (it)))
                  "should return false for pending?"
                  (false? (pending? (it)))
                  "should return false for container?"
                  (false? (pending? (it)))))))
