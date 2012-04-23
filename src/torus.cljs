;; Torus is a Clojurescript web application library inspired by
;; Ring (https://github.com/mmcgrana/ring).
;;
;; Like Ring, there is a SPEC file at the root of the torus repository
;; which describes the vanilla request and response interfaces. See
;; README.md for a short code synopsis and usage information.
(ns torus
  (:require
    [torus.util :as util]
    [torus.events :as te]
    [cljs.reader :as reader]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]
    [goog.events :as events]
    [goog.events.EventType :as event-type]
    [goog.events.EventTarget :as etarget]
    [goog.net.XhrIo :as xhrio]))

(def current-response (atom {}))
(def current-head (atom []))

(defn- separate [f s]
  [(filter f s) (filter (complement f) s)])

(defn- call-uninstall [resp]
  (when-let [f (:uninstall resp)]
    (uninstall)))

(defn- call-install [resp]
  (when-let [f (:install resp)]
    (install)))

(defn- location-map []
  (let [bl (fn [s] (if (seq s) s nil))]
    {:hash (bl (. js/location -hash))
     :host (bl (. js/location -host))
     :hostname (bl (. js/location -hostname))
     :href (bl (. js/location -href))
     :origin (bl (. js/location -origin))
     :pathname (bl (. js/location -pathname))
     :port (js/parseInt (. js/location -port))
     :protocol (if (= "https:" (. js/location -protocol)) :https :http)}))

(defn- replace-head [nodes]
  (let [head (. js/document -head)]
    (swap!
      current-head
      (fn [curr]
        (doseq [n curr]
          (dom/removeNode n))
        (doseq [n nodes]
          (dom/appendChild head n))
        nodes))))

(defn- replace-body [response-body]
  (set! (. js/document -body) response-body))

(defn- gxhr [url f]
  (xhrio/send url (fn [e]
                    (if (.. e -target (isSuccess))
                      (f (.. e -target getResponseText))
                      (throw (js/Error. "Template fetch failed."))))))

(defn- get-document [xhr x f]
  (let [decorate (fn [node]
                   {:title (when-let [t (aget (. node
                                               getElementsByTagName
                                               "title")
                                            0)]
                             (. t -innerHTML))
                    :head (when-let [h (aget (. node
                                                getElementsByTagName
                                                "head")
                                             0)]
                            (util/seq<- (. h -children)))
                    :body (aget (. node
                                   getElementsByTagName
                                   "body")
                                0)})
        str->html (fn [s]
                    (let [html (. js/document createElement "html")]
                      (set! (. html -innerHTML) s)
                      html))]
    (cond
      (instance? js/Document x)
      (-> (. x -documentElement) decorate f)

      (instance? js/Element x)
      (-> x decorate f)

      (and (string? x)
           (= \< (first x)))
      (-> x str->html decorate f)

      (string? x)
      ((or xhr gxhr) x #(-> % str->html decorate f)))))

(defn call-handler [tmap req]
  (let [location (location-map)

        {:keys [id html title install uninstall] :as resp}
        ((:handler tmap) (assoc req :location location))]
    (assert id)
    (assert html)
    (if (= id (:id @current-response))
      (install resp)
      (get-document
        (:xhr-fn tmap) html
        (fn [doc]
          (swap!
            current-response
            (fn [current]
              (call-uninstall current)
              (util/set-title-text (or title
                                       (:title doc)
                                       (:hostname location)))
              (replace-head (:head doc))
              (replace-body (:body doc))
              (call-install resp)
              resp))
          (. (:event-target tmap) dispatchEvent te/INSTALLED))))))

(defn change-path [tmap path & [state]]
  (. js/history pushState (pr-str state) nil path)
  (call-handler tmap {:history-state state}))

(defn click-handler [tmap]
  (fn [evt]
    (let [a (dom/getAncestorByTagNameAndClass
              (. evt -target) "a")]
      (when-let [url (and a
                          (classes/has a (:a-class tmap))
                          (. a getAttribute "href"))]
        (. evt preventDefault)
        (change-path tmap )
        (let [s (. a getAttribute (:a-state tmap))
              o (when s (reader/read-string s))]
          (. js/history pushState s nil url)
          (call-handler tmap {:history-state o}))))))

(defn start
  "Initialize torus with handler and opts. handler should be
  referentially transparent.

  Begins listening to all click events on a.torus-internal
  elements and calls history.pushState() with a.href as url and
  a.data-torus-state as state (read by read-string).

  Calls handler on each history popstate event.

  Returns a vector of popstate and click listener keys.

  Takes a key-value map of options:

  :a-class string

  Class to use instead of \"torus-internal\".

  :a-state

  Attribute to use instead of \"data-torus-state\".

  :immediate-dispatch?

  Call handler immediately without waiting for a popstate event.

  :xhr-fn

  The function to use for XHR. Takes a url and a callback."
  [handler & {:as opts}]
  (let [target (goog.events.EventTarget.)
        opts (merge {:a-class "torus-internal"
                     :a-state "data-torus-state"
                     :handler handler
                     :event-target target}
                    opts)]
    (when (:immediate-dispatch? opts)
      (call-handler opts {:history-state (. js/history -state)}))
    (assoc
      opts
      :listener-keys
      [(events/listen
         js/window event-type/POPSTATE
         (fn [evt]
           (call-handler opts {:history-state (. evt -state)})))
       (events/listen
         (. js/document -documentElement) event-type/CLICK
         (click-handler opts))])))

(defn stop
  [{:keys [event-target listener-keys]}]
  (. event-target dispose)
  (doseq [k listener-keys]
    (events/unlistenByKey k)))
