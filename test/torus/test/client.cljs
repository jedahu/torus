(ns torus.test.client
  (:require
    [torus :as t]
    [menodora.core :as mc])
  (:use
    [menodora.predicates :only (eq truthy)])
  (:use-macros
    [menodora :only (defsuite describe should should* expect)]))

(defn fresh-start
  [handler & opts]
  (. js/history go (- (dec (. js/history -length))))
  (apply t/start handler opts))

(defsuite core-tests
  (describe "torus"
    (should "start"
      (let [ks (fresh-start (constantly nil))]
        (expect truthy
          (and (seq @listener-keys)
               (every? number? @listener-keys)))
        (t/stop ks)))))

;;. vim: set lispwords+=defsuite,describe,should,should*,expect:
