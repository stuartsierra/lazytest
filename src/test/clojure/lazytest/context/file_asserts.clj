(ns lazytest.context.file-asserts
  (:use [lazytest.context.file :only (temp-file temp-dir)]
	[lazytest.context :only (setup teardown)]))

(let [c1 (temp-file "hello" ".txt")]
  (let [f1 (setup c1)]
    (assert (instance? java.io.File f1))
    (assert (.exists f1))
    (assert (.isFile f1))
    (assert (re-matches #"hello.*\.txt" (.getName f1)))
    (teardown c1)
    (assert (not (.exists f1)))))

(let [c2 (temp-dir "hello")]
  (let [f2 (setup c2)]
    (assert (instance? java.io.File f2))
    (assert (.exists f2))
    (assert (.isDirectory f2))
    (assert (re-matches #"hello.*" (.getName f2)))
    (teardown c2)
    (assert (not (.exists f2)))))
