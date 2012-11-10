(defproject lazytest "1.2.3"
  :description "Lazytest framework for Clojure"
  :url "https://github.com/myguidingstar/lazytest/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.namespace "0.2.1"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"])
