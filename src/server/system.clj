(ns server.system
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [plumbing.core :refer [defnk]]
            [server.capture.core :as capture]
            [server.db :as db]
            [server.rabbit :as rabbit]
            [server.io.writer :as writer]
            [server.io.reader :as reader]
            [server.core :as app]))

(defnk system [{port 3000} {threads 2} {mode :fake} db-spec]
  (component/system-map
    :db (db/new-database db-spec)
    :rabbit (rabbit/new-rabbit)
    :capture (component/using
               (capture/capture-start mode)
               [:rabbit])
    :writer (component/using
              (writer/start-writer)
              [:capture :db :rabbit])
    :app (component/using
           (app/start-app port threads)
           [:capture :writer :db :rabbit])))

(defn -main [& args]
  (let [[port threads mode] args]
    (component/start
      (system))))

