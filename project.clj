(defproject
  torus "0.1.0-SNAPSHOT"

  :description "Clojurescript web application library"

  :plugins
  [[lein-cst "0.2.1"]
   [menodora "0.1.2"]]

  :exclusions
  [org.apache.ant/ant]

  :cst
  {:suites []})
