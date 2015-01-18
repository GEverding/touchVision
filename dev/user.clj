
(ns user
  (:require [clojure.java.io :as io]
            [clojure.java.javadoc :refer (javadoc)]
            [clojure.pprint :refer (pprint)]
            [clojure.reflect :refer (reflect)]
            [clojure.repl :refer (apropos dir doc find-doc pst source)]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [environ.core :refer (env)]
            [schema.coerce :as coerce]
            [langohr.core :as rabbit]
            [server.core :as jarvis]
            [server.system :as system]
            [server.db.queries :as q]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname (str "//localhost:5432/" (env :pg-db))
              :user (env :pg-user)
              :password (env :pg-pass)
              :init-pool-size 2
              :max-pool-size 20
              :partitions 1})

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  ( alter-var-root #'system
                   (constantly (system/system {:port 3000
                                               :threads 4
                                               :db-spec db-spec })))
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system component/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (require :reload 'server.tmpls)
  (refresh :after 'user/go))
