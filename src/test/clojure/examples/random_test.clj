(ns examples.random-test
  (:use lazytest.describe
	[lazytest.random :as r]))

(describe string-of
  (for-any [s (r/string-of (pick letter digit))]
    (it "is a string"
      (string? s))
    (it "has only letters and digits"
      (every? #(re-matches #"[0-9A-Za-z]" (str %)) s))))