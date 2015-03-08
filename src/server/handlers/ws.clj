(ns server.handlers.ws
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! sub take! put! close! chan sliding-buffer dropping-buffer go]]
            [taoensso.timbre :as timbre]
            [server.handlers.utils.res :refer (res)]
            [server.db.queries :as q]))

(timbre/refer-timbre)

;; (defn capture-ws [req]
;;   (let [capture (-> req :resources :capture)
;;         capture-out (chan (sliding-buffer 10))]
;;     (sub (:stdout capture) :post capture-out)
;;     (with-channel req ws-ch
;;       {:read-ch (chan (dropping-buffer 10))
;;        :format :edn} ; again, :edn is default
;;         (go
;;           (loop [datom (<! capture-out)]
;;             (when datom
;;               (when (>! ws-ch datom)
;;                 (recur (<! capture-out)))
;;               ))))))

(defn configure-capture-device [req]
  (let [{:keys [mode stream]} (:body req)
        caputre (-> req :resources :capture) ]
      (swap! (:state caputre) assoc :mode (keyword mode))
    (res {:msg "State Updated"})))

(defn init [req]
  (let [capture (-> req :resources :capture)
       capture-state (:state capture)
       conn (-> req :resources :db :conn)
       current-recording (q/find-active-recording conn) ]
    (res {:msg "init"
          :data {:mode (:mode @capture-state)
                 :patient-id 1
                 :recording-id (-> current-recording first :id)}})))
