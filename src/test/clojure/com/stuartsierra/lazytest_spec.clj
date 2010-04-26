(ns com.stuartsierra.lazytest-spec
  (:use [com.stuartsierra.lazytest
         :only (describe spec spec?
                         is using defcontext
                         context? ok? success?
                         pending? error? container?
                         thrown? thrown-with-msg?)]))

(defcontext dummy-context-1 [] 1)

(defcontext is-without-usings []
  (is (= 1 1) "hello" (= 1 2)))

(defcontext is-with-usings []
  (using [x dummy-context-1]
         (is (= x 1) "hello" (= x 2))))

(defcontext passing-assertion [it is-without-usings]
  (first (:children it)))

(defcontext failing-assertion [it is-without-usings]
  (second (:children it)))

(defcontext is-that-throws []
  (is (/ 1 0)))

(defcontext throwing-assertion [it is-that-throws]
  (first (:children it)))

(defcontext is-thrown-assertion []
  (first (:children (is (thrown? java.lang.ArithmeticException (/ 1 0))))))

(defcontext is-thrown-assertion-wrong-type []
  (first (:children (is (thrown? java.lang.IndexOutOfBoundsException (/ 1 0))))))

(defcontext is-thrown-assertion-no-throw []
  (first (:children (is (thrown? java.lang.Exception (= 1 1))))))

(defcontext empty-is []
  (is ))

(describe
 *ns*
 (spec is-spec "The 'is' macro"
       (using [it is-without-usings]
              (spec "without any usings"
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

       (using [it is-with-usings]
              (spec "with usings"
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
       (using [a passing-assertion]
              (spec "should return an object"
                    (is (not (nil? (a)))
                        "that supports success?"
                        (ok? (success? (a)))
                        "that supports pending?"
                        (ok? (pending? (a)))
                        "that supports error?"
                        (ok? (error? (a)))
                        "that supports container?"
                        (ok? (container? (a))))))

       (spec "and passing"
             (using [a passing-assertion]
                    (is "should be success?"
                        (true? (success? (a)))
                        "should not be error?"
                        (false? (error? (a)))
                        "should not be pending?"
                        (false? (pending? (a)))
                        "should not be container?"
                        (false? (container? (a))))))

       (spec "and failing"
             (using [a failing-assertion]
                    (is "should not be success?"
                        (false? (success? (a)))
                        "should not be error?"
                        (false? (error? (a)))
                        "should not be pending?"
                        (false? (pending? (a)))
                        "should not be container?"
                        (false? (container? (a))))))

       (spec "and throwing an exception"
             (using [a throwing-assertion]
                    (is "should not be success?"
                        (false? (success? (a)))
                        "should be error?"
                        (true? (error? (a)))
                        "should not be pending?"
                        (false? (pending? (a)))
                        "should not be container?"
                        (false? (container? (a)))))))

 (spec empty-is-spec "An empty 'is' expression"
       (using [it empty-is]
              (is "should not be success?"
                  (true? (success? (it)))
                  "should not be error?"
                  (false? (error? (it)))
                  "should be pending?"
                  (true? (pending? (it)))
                  "should be container?"
                  (false? (true? (it))))))

 (spec is-thrown-spec "An (is (thrown? ...)) assertion"
       (using [a is-thrown-assertion]
              (spec "that passes"
                    (is "should be success?"
                        (true? (success? (a)))
                        "should not be error?"
                        (false? (error? (a)))
                        "should not be pending?"
                        (false? (pending? (a)))
                        "should not be container?"
                        (false? (container? (a))))))

       (using [a is-thrown-assertion-wrong-type]
              (spec "that throws the wrong type"
                    (is "should not be success?"
                        (false? (success? (a)))
                        "should be error?"
                        (true? (error? (a)))
                        "should not be pending?"
                        (false? (pending? (a)))
                        "should not be container?"
                        (false? (container? (a))))))

       (using [a is-thrown-assertion-no-throw]
              (spec "that throws nothing"
                    (is "should not be success?"
                        (false? (success? (a)))
                        "should not be error?"
                        (false? (error? (a)))
                        "should not be pending?"
                        (false? (pending? (a)))
                        "should not be container?"
                        (false? (container? (a))))))))
