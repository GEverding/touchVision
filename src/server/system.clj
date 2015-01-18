(ns server.system
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [plumbing.core :refer [defnk]]
            [server.capture.core :as capture]
            [server.db :as db]
            [server.io.writer :as writer]
            [server.io.reader :as reader]
            [server.core :as app]))

(defnk system [{port 3000} {threads 2} {mode :fake} db-spec]
  (component/system-map
    :capture (capture/capture-start mode)
    :db (db/new-database db-spec)
    :writer (component/using
              (writer/start-writer)
              [:capture :db])
    :app (component/using
           (app/start-app port threads)
           [:capture :writer :db])))

(defn -main [& args]
  (let [[port threads mode] args]
    (component/start
      (system))))

