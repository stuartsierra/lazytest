(ns lazytest.context.stub
  "A Stub is a special kind of Context that rebinds a Var in the
  current dynamic environment."
  (:use [lazytest.context :only (context)]))

(defn stub
  "Returns a Context that creates a thread-local binding of Var v to
  new-value."
  [v new-value]
  {:pre [(var? v)]}
  (context (fn [] (push-thread-bindings {v new-value}))
	   (fn [] (pop-thread-bindings))))

(defn global-stub
  "Returns a Context that modifies the root binding if Var v. The Var
  must have a root binding before 'setup' is called. Use 'stub'
  instead unless you need to stub the Var on all threads."
  [v new-value]
  {:pre [(var? v)]}
  (let [this-stub (Object.)]
    (context
     (fn []
       (alter-meta! v (fn [m]
			(when (contains? m this-stub)
			  (throw (IllegalStateException.
				  (str "This global stub is already active for " v))))
			(let [old-value (var-get v)]
			  (alter-var-root v (constantly new-value))
			  (assoc m this-stub old-value)))))
   
     (fn []
       (alter-meta! v (fn [m]
			(when (not (contains? m this-stub))
			  (throw (IllegalStateException.
				  (str "This global stub is not active for " v))))
			(alter-var-root v (constantly (get m this-stub)))
			(dissoc m this-stub)))))))
