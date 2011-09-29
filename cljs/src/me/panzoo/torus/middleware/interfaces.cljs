(ns me.panzoo.torus.middleware.interfaces
  (:require
    [me.panzoo.torus.util :as util]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]))

(defn wrap-interface [handler iface]
  "Middleware which ensures the given interface is in place before calling
  handler.

  iface must be a no-argument function that returns a response map, with an
  optional extra key :content-id which must have a string value.

  handler :content and :head keys will be overwritten by those provided by the
  interface or ignored. handler :id, :callback, and :onremove will be ignored
  unless the interface's :content-id is set, in which case the next paragraph
  applies. handler :nested will be merged with interface :nested and will take
  priority. handler :title will be appended to interface :title. In all other
  cases, handler keys will replace interface keys of the same name.

  If :content-id is non-nil, the wrapped handler must have a :content key
  containing a node thunk. The response map is then changed so that :nested
  contains the following mapping:
  (:content-id iface) ->
    {:id       (:id handler)
     :content  (:content handler)
     :callback (:callback handler)
     :onremove (:onremove handler)}"
  (fn [req]
    (let [hr (handler req)
          ir (iface)
          content-id (:content-id ir)]
      (assert (:id ir))
      (assert (:content ir))
      (let [nest (when content-id
                   (assert (:id hr))
                   (assert (:content hr))
                   {:id (:id hr)
                    :content (:content hr)
                    :callback (:callback hr)
                    :onremove (:onremove hr)})
            title (str (:title ir) (:title hr))
            resp {:id (:id ir)
                  :title (if (seq title) title nil)
                  :head (:head ir)
                  :content (:content ir)
                  :nested (merge (:nested ir)
                                (:nested hr)
                                (when nest {content-id nest}))
                  :callback (:callback ir)
                  :onremote (:onremote ir)}
            hr* (dissoc hr
                        :id :title :head :content
                        :callback :onremove :nested)]
        (merge resp hr*)))))
