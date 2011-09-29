(ns me.panzoo.torus.util
  (:require
    [goog.dom :as dom]
    [goog.string :as str]))

(defn domseq->seq [s]
  (if (or (instance? js/NodeList s)
          (instance? js/HTMLCollection s))
    (for [x (range (.length s))]
      (aget s x))
    (seq s)))

(defn set-title-text [s]
  (set! (. js/document title) (str/escapeString s)))
