(ns me.panzoo.torus.middleware.swap-content
  (:require
    [me.panzoo.torus.util :as util]
    [goog.dom :as dom]))

(defn wrap-swap-content [handler id]
  "Middleware which replaced the node with the given id with an identical but
  empty one, then appends the output of handler to the new node.
  
  handler must return either the node or node seq to be appended to the new
  id node, or a map with the following keys, of which :title is optional.
  
  :content node or seq of nodes
  
  The value of this key is appended to the node with the given id.
  
  :title string
  
  If present, the page title is set to its value."
  (fn [req]
    (let [ret (handler req)]
      (if (map? ret)
        (let [{:keys [content title]} ret]
          (util/swap-node (dom/getElement id) content)
          (when title (util/set-title-text title)))
        (util/swap-node (dom/getElement id) ret)))))
