(ns torus.test.server
  (:require
    [cst.server :as cst]))

(def hello-world-html
  {:status 200
   :body "<html><head><title>Hello World</title></head><body>Hello World</body></html>"})

(def hello-no-title-html
  "<html><body><h1>Hello No Title</h1></body></html>")

(defn static-handler
  [req]
  (case (:uri req)
    "/template/hello-world.html" hello-world-html
    {:status 404}))

(def static-server #(cst/serve-cljs % :handler #'static-handler))
