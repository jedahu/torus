(defproject
  torus "0.1.1-SNAPSHOT"

  :description "Clojurescript web application library"

  :dependencies
  [[org.clojure/clojure "1.4.0"]]

  :profiles
  {:dev
   {:resource-paths ["test"]
    :dependencies [[cst "0.3.0"]
                   [menodora "0.1.4"]]}}

  :plugins
  [[lein-cst "0.3.0"]]

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
   :runner :console-phantom}

  :min-lein-version "2.0.0")
