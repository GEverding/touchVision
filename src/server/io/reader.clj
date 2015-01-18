(ns server.io.reader
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [clojure.core.async :refer [go-loop put! close! chan sliding-buffer <!]]
            [taoensso.timbre :as timbre]
            [server.db.queries :as q]))

(timbre/refer-timbre)

(defrecord Reader [db writer]
  Lifecycle
  (start [this])
  (stop [this]))

(defn start-reader []
  (map->Reader {}))
