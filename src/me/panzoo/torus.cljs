(ns me.panzoo.torus
  (:require
    [cljs.reader :as reader]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]
    [goog.events :as events]
    [goog.events.EventType :as event-type]))

(defn init [handler & {:as opts}]
  "Initialize torus.

  Begins listening to all click events on a.torus-internal
  elements and calls history.pushState() with a.href as url and
  a.data-torus-state as state (read by read-string).
  
  Calls (handler <history-state>) on each history popstate event.
  
  Takes a key-value map of options:

  :a-class string

  Class to use instead of \"torus-internal\".

  :a-state

  Attribute to use instead of \"data-torus-state\".

  :immediate-dispatch?

  Call handler immediately without waiting for a popstate event."
  (events/listen
    js/window event-type/POPSTATE
    (fn [evt]
      (handler {:history-state (. evt state)})))
  (let [aclass (or (:a-class opts) "torus-internal")
        astate (or (:a-state opts) "data-torus-state")]
    (events/listen
      (. js/document body) event-type/CLICK
      (fn [evt]
        (let [a (dom/getAncestorByTagNameAndClass
                  (. evt target) "a")]
          (when-let [url (and a
                              (classes/has a aclass)
                              (.getAttribute a "href"))]
            (. evt (preventDefault))
            (.pushState js/history
                        (reader/read-string
                          (.getAttribute a astate))
                        nil url))))))
  (when (:immediate-dispatch? opts)
    handler {:history-state (. js/history state)}))
