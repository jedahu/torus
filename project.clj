(defproject
  me.panzoo/torus "0.0.4"

  :description "Clojurescript web application library"

  :dependencies
  [[org.clojure/clojure "1.3.0"]
   [clj-webdriver "0.2.14"]
   [cljs-compiler "0.0.3"]]

  :exclusions
  [org.apache.ant/ant]

  :source-path "cljs/src")
