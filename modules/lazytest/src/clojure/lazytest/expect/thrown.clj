(ns lazytest.expect.thrown
  (:use [lazytest.expect :only (expect)]))

(defn throws? 
  "Calls f with no arguments; returns true if it throws an instance of
  class c.  Any other exception will be re-thrown.  Returns false if f
  throws no exceptions."
  [c f]
  (try (f) false
       (catch Throwable t
	 (if (instance? c t)
	   true
	   (throw t)))))

(defn throws-with-msg?
  "Calls f with no arguments; catches exceptions of class c.  If the
  message of the caught exception does not match re (with re-find),
  throws ExpectationFailed.  Any other exception not of class c will
  be re-thrown.  Returns false if f throws no exceptions."
  [c re f]
  (try (f) false
       (catch Throwable t
	 (if (instance? c t)
	   (expect (re-find re (.getMessage t)))
	   (throw t)))))

(defn cause-seq
  "Given a Throwable, returns a sequence of causes.  The first element
  of the sequence is the given throwable itself."
  [throwable]
  (lazy-seq
   (when throwable
     (cons throwable (cause-seq (.getCause throwable))))))

(defn causes?
  "Calls f with no arguments; returns true if it throws an exception
  whose cause chain includes an instance of class c.  Any other
  exception will be re-thrown.  Returns false if f throws no
  exceptions."
  [c f]
  (try (f) false
       (catch Throwable t
	 (if (some #(instance? c %) (cause-seq t))
	   true
	   (throw t)))))

(defn causes-with-msg?
  "Calls f with no arguments; catches exceptions with an instance of
  class c in their cause chain.  If the message of the causing
  exception does not match re (with re-find), throws
  ExpectationFailed.  Any non-matching exception will be re-thrown.
  Returns false if f throws no exceptions."
  [c re f]
  (try (f) false
       (catch Throwable t
	 (if-let [cause (some #(when (instance? c %) %) (cause-seq t))]
	   (expect (re-find re (.getMessage cause)))
	   (throw t)))))

(defn ok?
  "Calls f and discards its return value.  Returns true if f does not
  throw any exceptions."
  [f]
  (f) true)
