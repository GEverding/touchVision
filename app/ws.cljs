(ns client.ws
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
            [chord.client :refer [ws-ch]]
            [cljs.core.async :as async :refer [<! >! chan pub put! sliding-buffer]]
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
            (let [m (:message payload)
                  datom {:type (:type m)
                         :data (:data m)}]
              (put! out datom)
              (recur (<! ws-channel))))
          (.error js/console "ws throw an error: " error))))
      stdout))

(defn start! "start websocket" []
  (let [stdout (listen)]
    stdout))
