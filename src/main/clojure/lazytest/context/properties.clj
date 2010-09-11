(ns lazytest.context.properties
  "Contexts for manipulating Java system properties."
  (:use [lazytest.context :only (Context)]))

;; You don't create instances of this; use property-context
(deftype SystemPropertyContext [property value old-value-atom]
  Context
    (setup [this]
      (swap! old-value-atom
	     (fn [_]
	       (let [old-value (System/getProperty property)]
		 (if (nil? value)
		   (System/clearProperty property)
		   (System/setProperty property value))
		 old-value))))
    (teardown [this]
      (swap! old-value-atom
	     (fn [old-value]
	       (if (nil? old-value)
		 (System/clearProperty property)
		 (System/setProperty property old-value))))))

(defn property-context
  "Returns a context that sets the Java system property to the given
  value during testing.  Both name and value must be strings.  value
  may be nil to clear the property."
  [property-name new-value]
  {:pre [(string? property-name)
	 (seq property-name)  ; may not be empty
	 (or (nil? new-value) (string? new-value))]}
  (SystemPropertyContext. property-name new-value (atom nil)))
