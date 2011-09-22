(ns me.panzoo.torus.util
  (:require
    [goog.dom :as dom]))

(defn nodelist->seq [nl]
  (if (instance? js/NodeList nl)
    (for [x (range (.length nl))]
      (aget nl x))
    nl))

(defn set-title-text [s]
  (let [title (.item (dom/getElementsByTagNameAndClass "title") 0)]
    (set! (. title innerHTML) (js/escape s))))

(defn swap-node [node content]
  (let [content (nodelist->seq content)
        new-node (dom/createElement (. node tagName))
        parent (.parentNode node)]
    (.insertBefore parent new-node node)
    (.removeChild parent node)
    (doseq [n content]
      (dom/appendChild new-node n))
    (set! (. new-node id) (. node id))))
