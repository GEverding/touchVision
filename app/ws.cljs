(ns client.ws
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
            [chord.client :refer [ws-ch]]
            [cljs-log.core :as log]
            [cljs.core.async :as async :refer [<! >! chan pub close! put! sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [client.request :refer [request]]))

(def ^:private l (log/get-logger "ws"))

;; stdout is a publisher channel
(defn- listen []
  (let [out (chan (sliding-buffer 25))
        stdout (pub out :type)]
    (log/info l "starting Websocket listener")
    (go
      (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws" {:format :edn})) ]
        (if-not error
          (loop [payload (<! ws-channel)]
            (when payload
              (let [{:keys [message error]} payload
                    datom {:type (:type message)
                           :data (:data message)}]
                (log/finest l message)
                (if error
                  (do
                    (log/warning l error)
                    (close! out))
                  (do
                    (put! out datom)
                    (recur (<! ws-channel)))))))
          (do
          (log/severe l "*** ws throw an error ***")
          (log/severe l error)))))
      stdout))

(defn start! "start websocket" []
  (let [stdout (listen)]
    stdout))
