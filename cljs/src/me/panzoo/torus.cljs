(ns me.panzoo.torus
  "Torus is a Clojurescript web application library inspired by
  Ring (https://github.com/mmcgrana/ring).
  
  Like Ring, there is a SPEC file at the root of the torus repository
  which describes the vanilla request and response interfaces. See
  README.md for a short code synopsis and usage information."
  (:require
    [me.panzoo.torus.util :as util]
    [cljs.reader :as reader]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]
    [goog.events :as events]
    [goog.events.EventType :as event-type]))

(defn- location-map []
  {:hash (.hash js/location)
   :host (. js/location host)
   :hostname (. js/location hostname)
   :href (. js/location href)
   :origin (. js/location origin)
   :pathname (. js/location pathname)
   :port (js/parseInt (. js/location port))
   :protocol (if (= "https:" (. js/location protocol)) :https :http)})

(defn- replace-head [nodes class]
  (let [head (. js/document head)]
    (doseq [n (util/domseq->seq (. head children))]
      (when (classes/has n class)
        (dom/removeNode n)))
    (doseq [n nodes]
      (dom/appendChild head n)
      (classes/add n class))))

(defn- replace-body [new-body]
  (dom/replaceNode new-body body))

(defn- replace-ids [id-map]
  (doseq [[id thunk] id-map]
    (let [node (thunk)]
      (dom/replaceNode node (dom/getElement id))
      (.setAttribute node "id" id))))

(defn- call-handler [handler req opts]
  (let [resp (handler (assoc req :location (location-map)))
        html (. js/document documentElement)]
    (when-not (= (:id resp) (.getAttribute html (:torus-response-id opts)))
      (replace-head ((:head resp)) (:torus-class opts))
      (replace-body ((:body resp)))
      (replace-ids (:replace-ids resp))
      (util/set-title-text (:title resp))
      (.setAttribute html (:torus-response-id opts) (:id resp)))
    (when-let [cb (:callback resp)] (cb))))

(defn- click-handler [handler opts evt]
  (let [a (dom/getAncestorByTagNameAndClass
            (. evt target) "a")]
    (when-let [url (and a
                        (classes/has a (:a-class opts))
                        (.getAttribute a "href"))]
      (. evt (preventDefault))
      (let [s (.getAttribute a (:a-state opts))
            o (when s (reader/read-string s))]
        (.pushState js/history o nil url)
        (call-handler handler {:history-state o} opts)))))

(defn init
  "Initialize torus with handler and opts. handler should be
  referentially transparent.
  
  Begins listening to all click events on a.torus-internal
  elements and calls history.pushState() with a.href as url and
  a.data-torus-state as state (read by read-string).
  
  Calls handler on each history popstate event.
  
  Takes a key-value map of options:
  
  :a-class string
  
  Class to use instead of \"torus-internal\".
  
  :a-state
  
  Attribute to use instead of \"data-torus-state\".
  
  :torus-class
  
  Class to use instead of \"torus-placed\".
  
  :torus-response-id
  
  Attribute to use instead of \"data-torus-response-id\".
  
  :immediate-dispatch?
  
  Call handler immediately without waiting for a popstate event."
  [handler & {:as opts}]
  (.log js/console "torus/init")
  (events/listen
    js/window event-type/POPSTATE
    (fn [evt]
      (call-handler handler {:history-state (. evt state)} opts)))
  (.log js/console "popstate listener")
  (let [opts (merge {:a-class "torus-internal"
                     :a-state "data-torus-state"
                     :torus-class "torus-placed"
                     :torus-response-id "data-torus-response-id"}
                    opts)
        aclass (:a-class opts)
        astate (:a-state opts)]
    (events/listen
      (. js/document documentElement) event-type/CLICK
      (partial click-handler handler opts)))
  (.log js/console "click listener")
  (when (:immediate-dispatch? opts)
    (call-handler handler {:history-state (. js/history state)} opts)))
