(ns lazytest.failure)

(defprotocol Failure
  "Failure objects represent the reason an assertion failed within a
  test.  This protocol has no methods; it is only a marker for
  recognizing Failure objects.")


(defrecord NotEqual [objects] Failure)

(defn not-equal
  "Returns a Failure indicating that the given objects were supposed
  to be = but were not."
  [objects]
  (NotEqual. objects))


(defrecord NotNotEqual [objects] Failure)

(defn not-not-equal
  "Returns a Failure indicating that the given objects were supposed
  to be not= but were actually =."
  [objects]
  (NotNotEqual. objects))


(defrecord NotInstanceOf [expected-class actual-class] Failure)

(defn not-instance-of
  "Returns a Failure indicating that the object was supposed to be an
  instance of excpected-class but was not."
  [expected-class object]
  {:pre [(class? expected-class)]}
  (NotInstanceOf. expected-class (class object)))


(defrecord NotThrown [class] Failure)

(defn not-thrown
  "Returns a Failure indicating that the given Throwable class was
  supposed to be thrown but was not."
  [class]
  {:pre [(class? class)]}
  (NotThrown. class))


(defrecord ThrownWithWrongMessage [expected-re actual-message] Failure)

(defn thrown-with-wrong-message [expected-re actual-message]
  (ThrownWithWrongMessage. expected-re actual-message))


(defrecord NotLogicalTrue [value] Failure)

(defn not-logical-true
  "Returns a Failure indicating that the given value was supposed to
  be logical true but was not."
  [value]
  (NotLogicalTrue. value))


(defrecord PredicateFailed [pred args] Failure)

(defn predicate-failed [pred args]
  "Returns a failure indicating that the given predicate function,
  called on the given arguments, did not return logical true."
  [pred args]
  (PredicateFailed. pred args))
