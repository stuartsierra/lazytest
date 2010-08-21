(ns lazytest.test-case)

(defn test-case
  "Sets metadata on function f identifying it as a test case.  A test
  case function may execute arbitrary code and may have side effects.
  It should throw an exception to indicate failure.  Returning without
  throwing an exception indicates success.

  Test case functions may have before/after metadata (see lazytest.wrap)."
  [f]
  {:pre [(fn? f)]}
  (vary-meta f assoc ::test-case true))

(defn test-case?
  "True if x is a test case."
  [x]
  (and (fn? x) (::test-case (meta x))))

(defn try-test-case
  "Executes a test case function.  Does not execute before/after
   metadata functions.  Catches all Throwables.  Returns a map with
   the following key-value pairs:

     :source - the input function
     :pass?  - true if the function ran without throwing
     :thrown - the Throwable instance if thrown"
  [f]
  {:pre [(test-case? f)]
   :post [(map? %) (contains? % :pass?)]}
  (try (f) {:pass? true, :source f}
       (catch Throwable t
	 {:pass? false, :source f, :thrown t})))
