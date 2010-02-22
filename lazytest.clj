(deftype Context [parents before after])

(defn open-context
  "Opens context c, and all its parents, unless it is already active."
  [active c]
  (let [active (reduce open-context active (:parents c))
        states (map active (:parents c))]
    (if-let [f (:before c)]
      (assoc active c (or (active c) (apply f states)))
      active)))

(defn close-context
  "Closes context c and removes it from active."
  [active c]
  (let [states (map active (:parents c))]
    (when-let [f (:after c)]
      (apply f (active c) states))
    (let [active (reduce close-context active (:parents c))]
      (dissoc active c))))

(def c1 (Context nil nil nil))

(assert (= {} (open-context {} c1)))
(assert (= {} (close-context {} c1)))

(def c2 (Context nil (fn [] 2) (fn [x] (assert (= x 2)))))

(assert (= {c2 2} (open-context {} c2)))
(assert (= {} (close-context {c2 2} c2)))
(assert (= {c2 0} (open-context {c2 0} c2)))

(def c3 (Context [c2] (fn [s2] (assert (= s2 2)) 3)
                 (fn [x s2] (assert (= x 3)) (assert (= s2 2)))))

(assert (= {c2 2, c3 3} (open-context {} c3)))
(assert (= {} (close-context {c2 2, c3 3} c3)))

(deftype TestCase [contexts children])

