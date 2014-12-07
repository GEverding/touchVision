(ns server.handlers.ws
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<!  >! take! put! close! chan sliding-buffer dropping-buffer go]]
            [taoensso.timbre :as timbre]
            [schema.core :as s]))

(timbre/refer-timbre)
(defonce state (atom { :stream :closed }))

(defn capture-ws [req]
  (let [ capture (-> req :resources :capture)]
    (with-channel req ws-ch
      {:read-ch (chan (dropping-buffer 10))
       :format :edn} ; again, :edn is default
      (let [stdout (:stdout capture)]
        (go
          (loop [datom (<! stdout)]
                  (when datom
                    (if (= (:stream @state) :open)
                      (when (>! ws-ch datom)
                        (recur (<! stdout)))
                      (recur (<! stdout))
                      ))))))))

(defn configure-ws [req]
  (let [{:keys [mode stream]} (:query-params req)
        caputre (-> req :resources :capture)
        ]
    (dosync
      (swap! (:state caputre) assoc :mode (keyword mode))
      (swap! state assoc :stream (keyword stream)))
    )
  "success")
