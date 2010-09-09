(ns lazytest.context.stateful
  (:use [lazytest.context :only (Context context? setup teardown)]))

;; You don't create instances of this; use the stateful function.
(deftype StatefulContext [c state-atom]
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
		 (when (neg? newcount)
		   (throw (IllegalStateException.
			   "teardown called too many times on stateful context")))
		 [(if (zero? newcount) (teardown c) value)
		  newcount])))
      nil)

  clojure.lang.IDeref
    (deref [this]
      (let [[value counter] @state-atom]
	(if (zero? counter)
	  ;; Can't throw here because print-method tries to deref
	  (IllegalStateException.
	   "Tried to deref stateful context before setup or after teardown.")
	  value))))

(defn stateful
  "Returns a Context that wraps Context c in a stateful container.

  The setup method of context c returns some state.  That state can be
  retrieved by deref'ing this context.

  Nested calls to setup/teardown are counted and handled
  appropriately; only the outermost setup/teardown calls are passed to
  the wrapped context."
  [c]
  {:pre [(context? c)]}
  (StatefulContext. c (atom [nil 0])))
