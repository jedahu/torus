(ns torus.test.client
  (:require
    [torus :as t]
    [torus.events :as te]
    [goog.events :as events]
    [goog.events.EventType :as event-type]
    [menodora.core :as mc])
  (:use
    [menodora.predicates :only (eq truthy)])
  (:use-macros
    [menodora :only (defsuite describe should should* expect)]))

(defsuite core-tests
  (describe "simple operation"
    :let [torus (atom nil)]
    :pre (reset!
              torus
              (t/start
                (fn [req]
                  (condp = (-> req :location :pathname)
                    "/"
                    {:id "default"
                     :html "<body>default</body>"}

                    "/with-title"
                    {:id "simple"
                     :html (str "<html><head><title>Simple</title></head>"
                                "<body>Hello<body></html>")}

                    "/without-title"
                    {:id "no-title"
                     :html (str "<html><head></head>"
                                "<body>Titleless</body></html>")}

                    "/with-resp-title"
                    {:id "resp-title"
                     :html (str "<head><title>Not seen</title></head>")
                     :title "Response"}))))
    :post (do
            (swap! torus t/stop)
            (. js/history pushState nil nil "/"))
    (should "start"
      (expect truthy @torus))
    (should* "install template"
      (events/listenOnce
        (:event-target @torus) te/INSTALLED
        (fn [evt]
          (expect eq "Simple" (. js/document -title))
          (expect eq "Hello" (.. js/document -body -innerText))
          (<done>)))
      (t/change-path @torus "/with-title"))
    (should* "add hostname title if none given"
      (events/listenOnce
        (:event-target @torus) te/INSTALLED
        (fn [evt]
          (expect eq "localhost" (. js/document -title))
          (expect eq "Titleless" (.. js/document -body -innerText))
          (<done>)))
      (t/change-path @torus "/without-title"))
    (should* "replace doc title with response title"
      (events/listenOnce
        (:event-target @torus) te/INSTALLED
        (fn [evt]
          (expect eq "Response" (. js/document -title))
          (<done>)))
      (t/change-path @torus "/with-resp-title"))))

;;. vim: set lispwords+=defsuite,describe,should,should*,expect:
