(ns server.handlers.ws
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<!  >! take! put! close! chan sliding-buffer dropping-buffer go]]
            [taoensso.timbre :as timbre]
            [server.handlers.utils.res :refer (res)]
            [server.db.queries :as q]
            ))

(timbre/refer-timbre)
(defonce state (atom { :stream :closed }))

(defn capture-ws [req]
  (let [ writer (-> req :resources :writer)]
    (with-channel req ws-ch
      {:read-ch (chan (dropping-buffer 10))
       :format :edn} ; again, :edn is default
      (let [stdout (:stdout writer)]
        (go
          (loop [datom (<! stdout)]
                  (when datom
                    (if (= (:stream @state) :open)
                      (when (>! ws-ch datom)
                        (recur (<! stdout)))
                      (recur (<! stdout))
                      ))))))))

(defn configure-ws [req]
  (let [{:keys [mode stream]} (:body req)
        caputre (-> req :resources :capture) ]
    (dosync
      (swap! (:state caputre) assoc :mode (keyword mode))
      (swap! state assoc :stream (keyword stream)))
    (res {:msg "Statue Updated"})))

(defn init [req]
  (let [capture (-> req :resources :capture)
       capture-state (:state capture)
       conn (-> req :resources :db :conn)
       current-recording (q/find-active-recording conn) ]
    (res {:msg "init"
          :data {:mode (:mode @capture-state)
                 :stream (:stream @state)
                 :patient-id 1
                 :current-recording current-recording}})))
