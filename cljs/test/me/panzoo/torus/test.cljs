(ns me.panzoo.torus.test
  (:require
    [me.panzoo.torus :as torus]
    [me.panzoo.torus.util :as util]
    [me.panzoo.torus.middleware.interfaces :as tmi]
    [goog.Timer :as timer]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]
    [goog.style :as style]
    [cljs.reader :as reader]))

(defn timeout [t & funs]
  (reduce
    #(timer/callOnce (fn [] (%1) (%2)) t)
    (fn [])
    (reverse funs)))

(defn timeout-wrap [t wrap & funs]
  (reduce
    #(timer/callOnce (fn [] (%1) (wrap %2)) t)
    (fn [])
    (reverse funs)))

(defn iface1 []
  {:id "iface1"
   :title "iface 1 - "
   :head (delay [(dom/createDom "meta" (.strobj {"name" "interface 1"}))])
   :content
   (delay
     (dom/createDom
       "body" nil
       (dom/createDom
         "h1" (.strobj {"id" "heading"})
         (dom/createTextNode "Title 1"))
       (dom/createDom "div" (.strobj {"id" "content"
                                      "style" "border: red solid 3px"})
                      (dom/createElement "hr"))
       (dom/createDom "p" nil (dom/createTextNode "Footer"))))
   :content-id "content"
   :callback
   (fn [_]
     (dom/appendChild (dom/getElement "heading")
                      (dom/createTextNode "suffix")))})

(defn iface2 []
  {:id "iface2"
   :content
   (delay
     (dom/createDom
       "div" nil
       (dom/createDom
         "h1" nil
         (dom/createTextNode "Title 2"))
       (dom/createDom "article" (.strobj {"id" "content1"}))
       (dom/createDom "article" (.strobj {"id" "content2"}))))})

(defn link [state url text]
  (let [a (dom/createDom
            "a"
            (.strobj
              {"href" url
               "class" "torus-internal"})
            (dom/createTextNode text))]
    (.setAttribute a "data-torus-state" state)
    a))

(defn route-list []
  (dom/createDom
    "ul" nil
    (dom/createDom "li" nil (link "1" "#state1" "state 1"))
    (dom/createDom "li" nil (link "2" "#state2" "state 2"))
    (dom/createDom "li" nil (link "3" "#state3" "state 3"))))

(defn ^:export run []
  (.log js/console "run")
  (torus/init
    (fn [req]
      (.log js/console "req")
      (let [hstate (:history-state req)]
        (cond
          (= 1 hstate) {:id "state1" :title "state 1"
                        :content (delay
                                   (dom/createDom
                                     "h1" nil (dom/createTextNode "one")))}
          (= 2 hstate) ((tmi/wrap-interface
                          (fn [req]
                            {:id "handler2"
                             :title "handler 2"
                             :content
                             (delay
                               (dom/createDom
                                 "div" nil
                                 (dom/createDom "h3" nil (dom/createTextNode "swapped"))
                                 (route-list)))})
                          iface1)
                          req)
          (= 3 hstate) ((tmi/wrap-interface
                          (constantly
                            {:nested
                             {"content1" {:id "handler 3 1"
                                          :content (delay (route-list))}
                              "content2" {:id "handler 3 2"
                                          :content (delay
                                                     (link "4" "#state4"
                                                           "second content"))}}})
                           iface2) req)
          (= 4 hstate) ((tmi/wrap-interface
                          (constantly
                            {:nested
                             {"content1" {:id "handler 3 1"
                                          :content (delay (route-list))}
                              "content2" {:id "handler 3 2"
                                          :content (delay
                                                     (link "99" "" "home"))}}})
                           iface2) req)
          :else {:id "default"
                 :content (delay (route-list))})))
    :immediate-dispatch? true))
