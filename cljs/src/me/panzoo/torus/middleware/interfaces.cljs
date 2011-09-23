(ns me.panzoo.torus.middleware.interfaces
  (:require
    [me.panzoo.torus.util :as util]
    [goog.dom :as dom]
    [goog.dom.classes :as classes]))

(defn wrap-interface [handler iface]
  "Middleware which ensures the given interface is in place before calling
  handler.

  iface must be a no-argument function that returns a map containing the
  following keys, of which :id and :body are mandatory.

  :id string

  :body nodes

  :head nodes

  :callback no-argument function

  :content-id string

  The :id, :body, :head, and :callback keys correspond to those in the handler
  spec.  handler :body and :head keys will be overwritten by those proveded by
  the interface (or set to nil). Interface and handler callbacks will be
  sequenced, with the interface callback first. The handler :id will be
  appended to the interface :id. In all other cases, handler keys will replace
  interface keys of the same name.

  If :content-id is non-nil, the wrapped handler must have a :content key
  containing a thunk returning a node. The response map is then changed so the
  :replace-ids map contains a :content-id -> :content mapping."
  (fn [req]
    (let [hr (handler req)
          ir (iface)]
      (assert (:id hr))
      (assert (:id ir))
      (assert (:body ir))
      (let [id (str (:id ir) (:id hr))
            cb (when (and (:callback hr) (:callback ir))
                 (let [ch (:callback hr)
                       ci (:callback ir)]
                   (fn [] (ci) (ch))))
            resp (assoc hr
                        :body (:body ir)
                        :head (:head ir))
            resp (merge ir resp)
            resp (if cb
                   (assoc resp :callback cb)
                   resp)
            resp (if (:content-id ir)
                   (do
                     (assert (:content hr))
                     (assoc-in resp [:replace-ids (:content-id ir)]
                               (:content hr)))
                   resp)]
        (assoc resp :id id)))))
