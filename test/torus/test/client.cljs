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

(defn start-torus
  [& {:as responses}]
  (t/start
    (fn [req]
      (get responses (-> req :location :pathname)))))

(defsuite core-tests
  (describe "client operation"
    (should* "install template"
      (let [tmap (start-torus
                   "/with-title"
                   {:id "hello"
                    :html (str "<head><title>Simple</title></head>"
                               "<body>Hello</body>")})]
        (events/listenOnce
          (:event-target tmap) te/INSTALLED
          (fn [evt]
            (expect eq "Simple" (. js/document -title))
            (expect eq "Hello" (.. js/document -body -innerText))
            (t/stop tmap)
            (<done>)))
        (t/change-path tmap "/with-title")))
    (should* "add hostname title if none given"
      (let [tmap (start-torus
                   "/without-title"
                   {:id "no-title"
                    :html (str "<html><head></head>"
                               "<body>Titleless</body></html>")})]
        (events/listenOnce
          (:event-target tmap) te/INSTALLED
          (fn [evt]
            (expect eq "localhost" (. js/document -title))
            (expect eq "Titleless" (.. js/document -body -innerText))
            (t/stop tmap)
            (<done>)))
        (t/change-path tmap "/without-title")))
    (should* "replace doc title with response title"
      (let [tmap (start-torus
                   "/with-resp-title"
                   {:id "resp-title"
                    :html (str "<head><title>Not seen</title></head>")
                    :title "Response"})]
            (events/listenOnce
              (:event-target tmap) te/INSTALLED
              (fn [evt]
                (expect eq "Response" (. js/document -title))
                (t/stop tmap)
                (<done>)))
            (t/change-path tmap "/with-resp-title"))))

  (describe "server operation"
    :post (. js/history pushState nil nil "/")
    (should* "install template"
      (let [tmap (start-torus
                   "/server-hello"
                   {:id "server hello"
                    :html "/template/hello-world.html"})]
        (events/listenOnce
          (:event-target tmap) te/INSTALLED
          (fn [evt]
            (expect eq "Hello World" (. js/document -title))
            (expect eq "Hello World" (.. js/document -body -innerText))
            (t/stop tmap)
            (<done>)))
        (t/change-path tmap "/server-hello")))
    (should* "handle 404s gracefully"
      (let [tmap (start-torus
                   "/missing-template"
                   {:id "missing"
                    :html "/template/not-found.html"})]
        (events/listenOnce
          (:event-target tmap) te/INSTALLED
          (fn [evt]
            (expect eq "INSTALLED" "event should not be dispatched")
            (events/removeAll {:event-target tmap})
            (t/stop tmap)
            (<done>)))
        (events/listenOnce
          (:event-target tmap) te/NOT-FOUND
          (fn [evt]
            (expect truthy "NOT-FOUND event should be dispatched")
            (expect eq "server hello" (:id @t/current-response))
            (events/removeAll {:event-target tmap})
            (t/stop tmap)
            (<done>)))
        (t/change-path tmap "/missing-template")))))

;;. vim: set lispwords+=defsuite,describe,should,should*,expect:
