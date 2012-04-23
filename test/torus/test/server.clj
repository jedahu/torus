(ns torus.test.server
  (:require
    [cst.server :as cst]))

(def hello-world-html
  "<html><head><title>Hello World</title></head><body><h1>Hello World</h1></body></html>")

(def hello-no-title-html
  "<html><body><h1>Hello No Title</h1></body></html>")

(defn static-handler
  [req]
  (case (:uri req)
    "/template/hello-world.html" hello-world-html
    "/template/hello-no-title.html" hello-no-title-html
    {:status 404}))

(def static-server #(cst/serve-cljs % :handler static-handler))
