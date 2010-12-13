(defproject sample-leiningen-project "1.0.0-SNAPSHOT"
  :description "This project shows how to use Lazytest in a Leiningen project."
  :dependencies [[org.clojure/clojure "1.3.0-alpha4"]
		 [com.stuartsierra/lazytest "2.0.0-SNAPSHOT"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"
		 "stuartsierra-snapshots" "http://stuartsierra.com/m2snapshots"}
  :main main)