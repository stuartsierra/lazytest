(ns lazytest.expect.thrown)

(defmacro thrown?
  "Returns true if body throws an instance of class c."
  [c & body]
  `(try ~@body false
        (catch ~c e# true)))

(defmacro thrown-with-msg?
  "Returns true if body throws an instance of class c whose message
  matches re (with re-find)."
  [c re & body]
  `(try ~@body false
        (catch ~c e# (re-find ~re (.getMessage e#)))))

(defn causes
  "Given a Throwable, returns a sequence of the Throwables that caused
  it, in the order they occurred."
  [throwable]
  (lazy-seq
   (when throwable
     (cons throwable (causes (.getCause throwable))))))

(defmacro caused?
  "Returns true if body throws an exception which is of class c or
  caused by an exception of class c."
  [c & body]
  `(try ~@body false
	(catch Throwable e#
	  (if (some (fn [cause#] (instance? ~c cause#)) (causes e#))
	    true
	    (throw e#)))))

(defmacro caused-with-msg?
  "Returns true if body throws an exception of class c or caused by
  an exception of class c whose message matches re (with re-find)."
  [c re & body]
  `(try ~@body false
	(catch Throwable e#
	  (if (some (fn [cause#]
		      (and (instance? ~c cause#)
			   (re-find ~re (.getMessage cause#))))
		    (causes e#))
	    true
	    (throw e#)))))
