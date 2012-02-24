;; Torus is a Clojurescript web application library inspired by
;; Ring (https://github.com/mmcgrana/ring).
;;   
;; Like Ring, there is a SPEC file at the root of the torus repository
;; which describes the vanilla request and response interfaces. See
;; README.md for a short code synopsis and usage information.
(ns me.panzoo.torus
  (:require
    [me.panzoo.torus.util :as util]
    [cljs.reader :as reader]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]
    [goog.events :as events]
    [goog.events.EventType :as event-type]
    [goog.net.XhrIo :as xhrio]))

(def ^:private current-response (atom {}))
(def ^:private current-head (atom []))
(def ^:private current-handler (atom (constantly nil)))
(def ^:private current-opts (atom nil))

(defn- separate [f s]
  [(filter f s) (filter (complement f) s)])

(defn- call-uninstall [resp]
  (when-let [f (:uninstall resp)]
    (uninstall resp)))

(defn- call-install [resp]
  (when-let [f (:install resp)]
    (install resp)))

(defn- location-map []
  (let [bl (fn [s] (if (seq s) s nil))]
    {:hash (bl (.hash js/location))
     :host (bl (. js/location host))
     :hostname (bl (. js/location hostname))
     :href (bl (. js/location href))
     :origin (bl (. js/location origin))
     :pathname (bl (. js/location pathname))
     :port (js/parseInt (. js/location port))
     :protocol (if (= "https:" (. js/location protocol)) :https :http)}))

(defn- replace-head [new-nodes]
  (let [head (. js/document head)]
    (swap!
      current-head
      (fn [curr]
        (doseq [n curr]
          (dom/removeNode n))
        (doseq [n new-nodes]
          (dom/appendChild head n))
        new-nodes))))

(defn- replace-body [new-body]
  (set! (. js/document body) new-node))

(defn- gxhr [url f]
  (xhrio/send url (fn [e]
                    (if (.. e -target (isSuccess))
                      (f (.. e -target (getResponseText)))
                      (throw (js/Error. "Template fetch failed."))))))

(defn- get-document [xhr x f]
  (let [decorate (fn [node]
                   {:title (aget (. node (getElementsByTagName "title")) 0)
                    :head (aget (. node (getElementsByTagName "head")) 0)
                    :body (aget (. node (getElementsByTagName "body")) 0)})]
    (cond
      (instance? js/Document x)
      (-> (. x -documentElement) decorate f)

      (instance? js/Element x)
      (-> x decorate f)

      (and (string? x)
           (= \< (first x)))
      (-> x dom/htmlToDocumentFragment decorate f)

      (string? x)
      ((or xhr gxhr) x #(-> % dom/htmlToDocumentFragment decorate f)))))

(defn- call-handler [handler req opts]
  (let [location (location-map)
        {:keys [id html install uninstall] :as resp}]
    (assert id)
    (assert html)
    (if (= id (:id @current-response))
      (install resp)
      (get-document
        (:xhr opts) html
        (fn [doc]
          (swap!
            current-response
            (fn [current]
              (call-uninstall current)
              (util/set-title-text (or title (:title doc)))
              (replace-head (:head doc))
              (replace-body (:body doc))
              (call-install resp))))))))

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

(defn goto-url [url & [state]]
  (.pushState js/history state nil url)
  (call-handler @current-handler {:history-state state} @current-opts))

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
  
  :immediate-dispatch?
  
  Call handler immediately without waiting for a popstate event."
  [handler & {:as opts}]
  (reset! current-handler handler)
  (reset! current-opts opts)
  (.log js/console "reset")
  (events/listen
    js/window event-type/POPSTATE
    (fn [evt]
      (call-handler handler {:history-state (. evt state)} opts)))
  (.log js/console "popstate")
  (let [opts (merge {:a-class "torus-internal"
                     :a-state "data-torus-state"}
                    opts)]
    (events/listen
      (. js/document documentElement) event-type/CLICK
      (partial click-handler handler opts)))
  (.log js/console "mouse")
  (when (:immediate-dispatch? opts)
    (call-handler handler {:history-state (. js/history state)} opts)))
