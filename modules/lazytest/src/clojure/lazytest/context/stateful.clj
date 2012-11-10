(ns lazytest.context.stateful
  (:use [lazytest.context :only (Context context? setup teardown)]))

;; You don't create instances of this; use the stateful or
;; stateful-fn-context functions.
(deftype StatefulFunctionContext [setup-fn teardown-fn state-atom]
  Context
    (setup [this]
     (first  ; setup always returns the state
       (swap! state-atom
	      (fn [[value counter]]
		[(if (zero? counter) (setup-fn) value)
		 (inc counter)]))))
    (teardown [this]
      (swap! state-atom
	     (fn [[value counter]]
	       (let [newcount (dec counter)]
		 (when (neg? newcount)
		   (throw (IllegalStateException.
			   "teardown called too many times on stateful context")))
		 [(if (zero? newcount) (teardown-fn value) value)
		  newcount])))
      nil)  ; teardown always returns nil

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
  (StatefulFunctionContext. (fn [] (setup c))
			    (fn [_] (teardown c))
			    (atom [nil 0])))

(defn stateful-fn-context
  "Creates a stateful context using the given functions.

  setup-fn is a function of no arguments that returns some state.
  That state may be retrieved by deref'ing this context.

  teardown-fn is a function of *one* argument.  It will be called with
  the dereferenced state.

  Nested calls to setup/teardown are counted and handled
  appropriately; only the outermost setup/teardown calls will invoke
  the given functions."
  [setup-fn teardown-fn]
  {:pre [(fn? setup-fn) (fn? teardown-fn)]}
  (StatefulFunctionContext. setup-fn teardown-fn (atom [nil 0])))
