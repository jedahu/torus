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
  (describe "client operation"
    :let [tmap (atom nil)]
    :pre (reset!
           tmap
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
            (swap! tmap t/stop)
            (. js/history pushState nil nil "/"))
    (should "start"
      (expect truthy @tmap))
    (should* "install template"
      (events/listenOnce
        (:event-target @tmap) te/INSTALLED
        (fn [evt]
          (expect eq "Simple" (. js/document -title))
          (expect eq "Hello" (.. js/document -body -innerText))
          (<done>)))
      (t/change-path @tmap "/with-title"))
    (should* "add hostname title if none given"
      (events/listenOnce
        (:event-target @tmap) te/INSTALLED
        (fn [evt]
          (expect eq "localhost" (. js/document -title))
          (expect eq "Titleless" (.. js/document -body -innerText))
          (<done>)))
      (t/change-path @tmap "/without-title"))
    (should* "replace doc title with response title"
      (events/listenOnce
        (:event-target @tmap) te/INSTALLED
        (fn [evt]
          (expect eq "Response" (. js/document -title))
          (<done>)))
      (t/change-path @tmap "/with-resp-title")))

  (describe "server operation"
    :let [tmap (atom nil)]
    :pre (reset!
           tmap
           (t/start
             (fn [req]
               (condp = (-> req :location :pathname)
                 "/"
                 {:id "default"
                  :html "<body>default</body>"}

                 "/server-hello"
                 {:id "server hello"
                  :html "/template/hello-world.html"}))))
    :post (do
            (swap! tmap t/stop)
            (. js/history pushState nil nil "/"))
    (should* "install template"
      (events/listenOnce
        (:event-target @tmap) te/INSTALLED
        (fn [evt]
          (expect eq "Hello World" (. js/document -title))
          (expect eq "Hello World" (.. js/document -body -innerText))
          (<done>)))
      (t/change-path @tmap "/server-hello"))))

;;. vim: set lispwords+=defsuite,describe,should,should*,expect:
