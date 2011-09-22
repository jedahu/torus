(ns me.panzoo.torus.middleware.interfaces
  (:require
    [me.panzoo.torus.util :as util]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]))

(def ^:dynamic *iface-attr-name* "data-torus-interface-name")
(def ^:dynamic *iface-head-class* "torus-added-by-interface")

(defn setup-interface [iface]
  (let [root (. js/document documentElement)
        head (. js/document head)
        body (. js/document body)]
    (when-not (= (:name iface)
                 (.getAttribute root *iface-attr-name*))
      (util/swap-node body (:body iface))
      (doseq [node (util/nodelist->seq
                     (dom/getElementsByClass *iface-head-class* head))]
        (.removeChild head node))
      (if-let [new-head (:head iface)]
        (doseq [node (util/nodelist->seq new-head)]
          (classes/add node *iface-head-class*)
          (dom/appendChild head node)))
      (.setAttribute root *iface-attr-name* (:name iface))
      (if-let [cb (:callback iface)] (cb)))))

(defn wrap-interface [handler iface]
  "Middleware which ensures the given interface is in place before calling
  handler.

  iface must be a no-argument function that returns a map containing the
  following keys, of which :body is mandatory.

  :body node or seq of nodes

  These nodes replace the children of the body element.

  :head node or seq of nodes

  These nodes are appended to the head element. They are tagged with
  the *iface-head-class* before appending and are removed on the next
  interface replacement.

  :callback no-argument function

  This function is called after :body and :head have been inserted."
  (fn [req]
    (setup-interface (iface) attr)
    (handler req)))
