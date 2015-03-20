(ns server.io.reader
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [clojure.core.async :refer [go-loop put! close! chan sliding-buffer <!]]
            [langohr.exchange  :as le]
            [langohr.basic     :as lb]
            [cheshire.core :as json :refer [decode encode generate-string]]
            [taoensso.timbre :as timbre]
            ))

(timbre/refer-timbre)

(defn reader->playback [pressure rmq-ch]
  (le/direct rmq-ch "touchvision")
  (info pressure)
  (lb/publish rmq-ch "touchvision" "playback"  (encode {:pressure pressure}) {:content-type "text/plain"}))

(defrecord Reader [writer rabbit]
  Lifecycle
  (start [this]
    (let [realtime? (atom false)
          stdin (:stdout writer)
          rmq-ch (:rmq-ch rabbit)]
      (go-loop [p (<! stdin)]
        (when p
          (when @realtime?
            (reader->playback p rmq-ch))
          (recur (<! stdin))))
      (assoc this :realtime? realtime?)))
  (stop [this]
    this))


(defn start-reader []
  (map->Reader {}))
