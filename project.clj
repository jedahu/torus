(defproject
  torus "0.1.0-SNAPSHOT"

  :description "Clojurescript web application library"

  :dependencies
  [[org.clojure/clojure "1.4.0"]]

  :dev-dependencies
  [[cst "0.2.3"]
   [menodora "0.1.3"]]

  :plugins
  [[lein-cst "0.2.3"]]

  :exclusions
  [org.apache.ant/ant]

  :cst
  {:suites [torus.test.client/core-tests]
   :runners
   {:console-phantom {:cljs menodora.runner.console/run-suites-browser
                      :proc torus.test.server/static-server
                      :browser :phantom}
    :console-browser {:cljs menodora.runner.console/run-suites-browser
                      :proc torus.test.server/static-server}}
   :runner :console-phantom})
