(ns me.panzoo.torus.util
  (:require
    [goog.dom :as dom]))

(defn domseq->seq [s]
  (if (or (instance? js/NodeList s)
          (instance? js/HTMLCollection s))
    (for [x (range (.length s))]
      (aget s x))
    (seq s)))

(defn set-title-text [s]
  (let [title (.item (dom/getElementsByTagNameAndClass "title") 0)]
    (if title
      (set! (. title innerHTML) (js/escape s))
      (dom/appendChild
        (. js/document head)
        (dom/createDom "title" nil (dom/createTextNode (js/escape s)))))))
