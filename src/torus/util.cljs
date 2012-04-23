(ns torus.util
  (:require
    [goog.dom :as dom]
    [goog.string :as str]))

(defn seq<- [s]
  (if (or (instance? js/NodeList s)
          (instance? js/HTMLCollection s))
    (for [x (range (. s -length))]
      (aget s x))
    (seq s)))

(defn set-title-text [s]
  (set! (. js/document -title) (str/escapeString s)))

(defn debug [& args]
  (when js/console
    (.. js/console -log (apply js/console (apply array args)))))
