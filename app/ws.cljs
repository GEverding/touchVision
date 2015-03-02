(ns client.ws
  (:require-macros [cljs.core.async.macros :refer [go]])
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
                  (println (:data (:body res)))
                  (do
                    (doseq [d ds]
                      (do
                        (put! out {:type :post :data d})))
                    (reset! start (-> ds last :timestamp)))
                  )))))))))

;; stdout is a publisher channel
(defn- listen [app-state]
  (let [out (chan (sliding-buffer 25))
        stdout (pub out :type)]
    (log/info l "starting Websocket listener")
    (js/setInterval fetch 2000 app-state out)
    {:pub stdout
     :chan out}))

(defn start! "start websocket" [app-state]
  (let [stdout (listen app-state)]
    stdout))
