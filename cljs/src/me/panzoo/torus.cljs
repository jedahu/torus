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
    [goog.events.EventType :as event-type]))

(def ^:private current-response (atom {}))
(def ^:private current-handler (atom (constantly nil)))
(def ^:private current-opts (atom nil))

(defn- separate [f s]
  [(filter f s) (filter (complement f) s)])

(defn- call-remove [resp]
  (when-let [remfn (:onremove resp)]
    (remfn (dissoc resp :nested :onremove))))

(defn- call-callback [resp]
  (when-let [cbfn (:callback resp)]
    (cbfn (dissoc resp :nested :callback :onremove))))

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

(defn- replace-head [new-nodes current-nodes]
  (let [head (. js/document head)]
    (doseq [n current-nodes]
      (dom/removeNode n))
    (doseq [n new-nodes]
      (dom/appendChild head n))))

(defn- replace-body [new-body]
  (let [new-node (if (= "body" (. new-body tagName))
                   new-body
                   (dom/createDom "body" nil new-body))]
    (set! (. js/document body) new-node)))

(defn- process-nested [nested]
  (let [process (fn [node-id]
                  (let [{:keys [id content callback] :as resp}
                        (get nested node-id)]
                    (assert id)
                    (assert node-id)
                    (assert content)
                    (when-let [old-node (dom/getElement node-id)]
                      (assert content)
                      (assert @content)
                      (dom/replaceNode @content old-node)
                      (.setAttribute @content "id" node-id)
                      resp)))]
    (reduce #(assoc %1 %2 (process %2)) {} (keys nested))))

(defn- set-response-defaults [{:keys [title head] :as resp} location]
  (.log js/console "title" title)
  (.log js/console "hostname" (:hostname location))
  (.log js/console "pathname" (:pathname location))
  (assoc resp
         :title (or title (:hostname location) (:pathname location))
         :head (or head (delay []))))

(defn- call-handler [handler req opts]
  (let [location (location-map)
        {:keys [id title head content nested callback onremove] :as resp}
        (set-response-defaults (handler (assoc req :location location))
                               location)
        html (. js/document documentElement)]
    (assert id)
    (assert content)
    (swap!
      current-response
      (fn [current]
        (.log js/console "current" current)
        (if (not= id (:id current))
          (do
            (.log js/console "not")
            (call-remove current)
            (util/set-title-text title)
            (replace-head @head
                          (when-let [h (:head current)]
                            @h))
            (replace-body @content)
            (call-callback resp)
            (assoc resp :nested (process-nested nested)))
          (update-in
            current [:nested]
            (fn [old]
              (.log js/console "yes")
              (doseq [[id resp] old]
                (call-remove resp)
                (dom/replaceNode (dom/createDom
                                   "div" (.strobj {"id" id}))
                                 @(:content resp)))
              (.log js/console "remove")
              (process-nested nested))))))))

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
