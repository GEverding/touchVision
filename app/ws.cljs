(ns client.ws
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
            [chord.client :refer [ws-ch]]
            [cljs.core.async :as async :refer [<! >! chan pub close! put! sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [client.request :refer [request]]))

;; stdout is a publisher channel
(defn- listen []
  (let [out (chan (sliding-buffer 25))
        stdout (pub out :type)]
    (.log js/console "starting Websocket listener")
    (go
      (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws" {:format :edn})) ]
        (if-not error
          (loop [payload (<! ws-channel)]
            (when payload
              (let [{:keys [message error]} payload
                    datom {:type (:type message)
                           :data (:data message)}]
                (.log js/console message)
                (.error js/console error)
                (if error
                  (do
                    (.error js/console error)
                    (close! out))
                  (do
                    (put! out datom)
                    (recur (<! ws-channel)))))))
          (.error js/console "ws throw an error: " error))))
      stdout))

(defn start! "start websocket" []
  (let [stdout (listen)]
    stdout))
