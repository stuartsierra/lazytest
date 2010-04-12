(ns com.stuartsierra.lazytest.watch
  (:use [clojure.contrib.find-namespaces
         :only (read-file-ns-decl
                find-clojure-sources-in-dir
                find-namespaces-in-dir)]
        [clojure.contrib.java :only (as-file)]
        [com.stuartsierra.lazytest :only (run-spec TestThrown)]
        [com.stuartsierra.lazytest.report :only (spec-report)])
  (:import (java.io File)))

(defn ns-mod-times
  "Given a directory (java.io.File) containing .clj source files,
  returns map of namespace names (symbols) to Long timestamps."
  [d]
  (reduce (fn [m f]
            (if-let [sym (second (read-file-ns-decl f))]
              (assoc m sym (.lastModified f))))
          {} (find-clojure-sources-in-dir d)))

(defn stop
  "Sent as an agent action, stops a directory-watching agent."
  [state]
  (remove-watch *agent* ::watch)
  nil)

(defn reset
  "Send as an agent action, restarts a directory-watching agent."
  [state]
  {})

(defn ns-mod-time-agent
  "Creates an agent that monitors namespaces for changes to their
  source files.  The state of the agent will always be a map from
  namespace names (symbols) to their last modified timestamp."
  [dir & options]
  {:pre [(instance? File dir)]}
  (let [{:keys [delay] :or {delay 500}} options
        updater (fn upd [state]
                  (when state
                    (Thread/sleep delay)
                    (send *agent* upd)
                    (ns-mod-times dir)))]
    (doto (agent {})
      (send updater))))

(defn- newer-namespaces [older newer]
  (filter #(not= (older %) (newer %)) (keys newer)))

(defn watch-dir
  "Watches directory d for namespaces whose files change, calls f on
  the sequence of changed namespaces.  Returns the watching agent;
  send stop to stop watching the directory.

  f will run with same bindings in effect as when watch-dir is
  called.

  options are 
     :delay - milliseconds between directory scans"
  [d f & options]
  (let [dir (as-file d)]
    (assert (.isDirectory dir))
    (let [agnt (apply ns-mod-time-agent dir options)
          bf (bound-fn* f)]
      (add-watch agnt ::watch
                 (fn [key agnt older newer]
                   (let [names (newer-namespaces older newer)]
                     (when (seq names) (bf names)))))
      agnt)))

(defn watch-spec
  "Runs all specs in directory d, which must be on classpath, then
  watches d for changing namespaces, reloads and runs their specs when
  they change.  Prints reports to current *out*.  Returns the watching
  agent, send stop to stop watching.

  Options are
    :reporter - report function, default is spec-report
    :delay - time (ms) between directory scans"
  [d & options]
  (let [{:keys [reporter] :or {reporter spec-report}} options]
    (apply watch-dir d
           (fn [names]
             (try
              (when-let [results (run-spec names :replace)]
                (reporter results))
              (catch Throwable t
                (reporter (TestThrown names nil t)))))
           options)))
