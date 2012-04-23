(ns torus.test.client
  (:require
    [torus :as t]
    [torus.events :as te]
    [goog.events :as events]
    [menodora.core :as mc])
  (:use
    [menodora.predicates :only (eq truthy)])
  (:use-macros
    [menodora :only (defsuite describe should should* expect)]))

(defsuite core-tests
  (describe "simple operation"
    :let [torus (atom nil)]
    :before (reset! torus 
                 (t/start
                   (fn [req]
                     {:id "simple"
                      :html (str "<html><head><title>Simple</title></head>"
                                 "<body>Hello<body></html>")})))
    :after (swap! torus t/stop)
    (should "start"
      (expect truthy @torus))
    (should* "install template"
      (events/listen
        (:event-target @torus) te/INSTALLED
        (fn [evt]
          (expect eq "Simple" (. js/document -title)) 
          (expect eq "Hello" (.. js/document -body -innerText)) 
          (<done>)))
      (. js/history pushState nil nil "/"))))

;;. vim: set lispwords+=defsuite,describe,should,should*,expect:
