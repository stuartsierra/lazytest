(ns examples.complex-contexts
  "This namespace shows how contexts can depend on state set up by earlier contexts.

  This example tests an imaginary server application, with two
  functions, start-server and stop-server.  Those functions use the
  Java system property \"example.server.dir\" to configure the server.

  temp-dir-context is an instance of lazytest.context.file/temp-dir to
  create a temporary data directory.

  Next, properties-context is an instance of
  lazytest.context.properties/fn-property-context that assigns the
  runtime state of temp-dir-context to the \"example.server.dir\"
  property.

  Lastly, server-context is a simple context to start and stop the
  server.

  In the one test case, all three contexts are specified in the `with`
  block.  The order is significant: temp-dir-context will be evaluated
  first, followed by properties-context, followed by server-context."
  (:use [lazytest.describe :only (describe do-it testing with using)]
        [lazytest.context.file :only (temp-dir)]
        [lazytest.context :only (fn-context)]
        [lazytest.context.properties :only (fn-property-context)]))

(defn start-server []
  (println "Server started in" (System/getProperty "example.server.dir")))

(defn stop-server []
  (println "Server stopped in" (System/getProperty "example.server.dir")))

(def temp-dir-context
     (temp-dir "data"))

(def properties-context
     (fn-property-context "example.server.dir" (fn [] (.getAbsolutePath @temp-dir-context))))

(def server-context
     (fn-context start-server stop-server))

(describe "Complex nested contexts"
  (with [temp-dir-context
         properties-context
         server-context]
    (do-it (println "Server is running in" (System/getProperty "example.server.dir")))))
