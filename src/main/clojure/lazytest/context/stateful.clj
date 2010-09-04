(ns lazytest.context.stateful
  (:use [lazytest.context :only (Context context? setup teardown)]))

(deftype StatefulContext [c state-atom]
  ;; state-atom is a pair [value counter]. The wrapped setup/teardown
  ;; functions are only called when counter is zero.
  Context
    (setup [this]
	   (first
	    (swap! state-atom
		   (fn [[value counter]]
		     [(if (zero? counter) (setup c) value)
		      (inc counter)]))))
    (teardown [this]
	      (swap! state-atom
		     (fn [[value counter]]
		       (let [newcount (dec counter)]
			 [(if (zero? newcount) (teardown c) value)
			  newcount])))
	      nil)

  clojure.lang.IDeref
    (deref [this]
	   (let [[value counter] @state-atom]
	     (if (zero? counter)
	       (throw (IllegalStateException.
		       "Tried to deref context before setup."))
	       value))))

(defn stateful
  "Returns a Context that wraps Context c in a stateful container.
  The state returned by c's setup function can be retrieved by
  deref'ing this context.

  Counts the number of times setup is called; does not call c's
  teardown function until teardown has been called an equal number of
  times."
  [c]
  {:pre [(context? c)]}
  (StatefulContext. c (atom [nil 0])))
