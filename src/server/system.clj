(ns server.system
  (:gen-class)
  (:require [datomic.api :as d]
            [server
                [core :refer [ start-server stop-server ]]
                [db :as db]
                [config :refer [ cfg ]]
                [routes :refer [ app ]]]))

(defn init-system
  "Returns a new instance of the whole application."
  [ ]
  {:db (cfg :datomic-uri)
   :handler app
   :server (atom nil)
   }
  )

(defn start [system]
  (do
    (db/create system)
    (start-server system)
    system)
  )

(defn stop [system]
  (do
    (stop-server system)
    system)
  )

(defn -main []
  (start (init-system))
  )
