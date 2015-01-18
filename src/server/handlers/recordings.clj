(ns server.handlers.recordings
  (:use plumbing.core)
  (:require [clojure.core.async :refer [<!  >! take! put! close! chan sliding-buffer dropping-buffer go-loop]]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json :refer [decode encode generate-string]]
            [server.db.queries :as q]
            [server.io.writer :as writer]
            [server.handlers.utils.res :refer (res)]))

(defn new-recording [req]
  (println (:body req))
  (let [conn (-> req :resources :db :conn)
        patient-id (-> req :body :patient-id)
        id (if (string? patient-id) (read-string patient-id) patient-id)
        new-recording (q/new-recording<! conn id)]
    (res {:msg "New Recording Created"
          :data new-recording})))

(defn start [id req]
  (let [conn (-> req :resources :db :conn) ]
    (q/start-recording! conn (read-string id))
    (writer/set-recording-id! (read-string id))
    (res {:msg "Recording Started"
          :data {:id id}})))

(defn stop [id req]
  (let [conn (-> req :resources :db :conn) ]
    (writer/set-recording-id! nil)
    (q/stop-recording! conn (read-string id))
    (res {:msg "Recording Stopped"
          :data {:id id}})))
