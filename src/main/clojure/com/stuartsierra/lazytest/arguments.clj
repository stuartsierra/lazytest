(ns com.stuartsierra.lazytest.arguments)

(defn get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [this-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn get-options
  "Extracts keyword-value pairs from head of args, until next arg is
  not a keyword.  Returns a vector [options-map remaining-args]"
  [args]
  (loop [options nil args args]
    (if (and (seq args) (keyword? (first args)))
      (recur (assoc options (first args) (second args)) (nnext args))
      [options args])))

(defn standard-metadata
  "Returns a metadata map with keys :ns, :file, 
  :line, :form, :doc, and :name."
  ([form docstring]
     {:ns *ns*
      :file *file*
      :line (:line (meta form))
      :form form
      :doc docstring})
  ([form docstring name]
     {:ns *ns*
      :file *file*
      :line (:line (meta form))
      :form form
      :doc docstring
      :name name}))

(defn firsts
  "Returns a vector of the first element of each pair."
  [coll]
  (vec (map first (partition 2 coll))))

(defn seconds
  "Returns a vector of the second element of each pair."
  [coll]
  (vec (map second (partition 2 coll))))

(defn nil-or
  "True if value is nil or (pred value) is true."
  [pred value]
  (or (nil? value) (pred value)))


;;; ASSERTIONS

;; get-arg
(assert (= ["doc" '(a b)] (get-arg string? '("doc" a b))))
(assert (= [nil '(a b)] (get-arg string? '(a b))))

;; get-options
(assert (= [{:a 1 :b 2} '(c d)] (get-options '(:a 1 :b 2 c d))))
(assert (= [nil '(a b :c)] (get-options '(a b :c))))

;; standard-metadata
(comment
  (defmacro test-macro [name docstring & body]
    `(defn ~name ~(standard-metadata &form docstring name) [] ~@body))

  (test-macro test-var "hello" (+ 1 2))

  (let [m (meta (var test-var))]
    (assert (= *ns* (:ns m)))
    (assert (= 'test-var (:name m)))
    (assert (= "hello" (:doc m)))
    (assert (= '(test-macro test-var "hello" (+ 1 2)) (:form m)))
    (assert (string? (:file m)))
    (assert (integer? (:line m)))))

;; firsts
(assert (= '[a b c] (firsts '[a 1 b 2 c 3])))

;; seconds
(assert (= '[1 2 3] (seconds '[a 1 b 2 c 3])))
