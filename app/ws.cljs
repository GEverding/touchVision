(ns client.ws
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [chord.client :refer [ws-ch]]
            [cljs-log.core :as log]
            [cljs.core.async :as async :refer [<! >! chan pub close! put! pub sliding-buffer]]
            [client.request :refer (r)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def ^:private l (log/get-logger "ws"))

(def start  (atom 0))

(defn fetch [app-state out]
  (let [recording-id (:recording-id @app-state)]
    (when recording-id
      (let [cb (r {:type :get
                    :url (str "/recordings/" recording-id "/data")
                    :data {:limit 100
                             :start @start}})]

        (go
          (let [res (<! cb)]
            (when (= (:status res) 200)
              (when (-> res :body :data)
                (let [ds (-> res :body :data)]
                  (do
                    (doseq [d ds]
                      (put! out {:type :post :data d}))
                    (reset! start (-> ds last :timestamp)))
                  )))))))))

;; stdout is a publisher channel
(defn- listen [app-state event-bus]
  (let [out (chan (sliding-buffer 25))
        e-chan (chan)
        stdout (pub out :type)]
    (log/info l "starting Websocket listener")
    (async/tap (:bus event-bus) e-chan)
    (go-loop [e (<! e-chan)]
      (when e
        (when (= e :reset)
          (do
            (log/info l "resetting start timestamp")
            (reset! start 0)))
        (recur (<! e-chan))))
    (js/setInterval fetch 2000 app-state out)

    {:pub stdout
     :chan out}))

(defn start! "start websocket" [app-state event-bus]
  (let [stdout (listen app-state event-bus)]
    stdout))
