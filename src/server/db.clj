(ns server.db
  (:import com.jolbox.bonecp.BoneCPDataSource)
  (:require [com.stuartsierra.component :as component]))

(defn pooled-datasource [db-spec]
  (let [{:keys [classname subprotocol subname user password
                init-pool-size max-pool-size idle-time partitions]} db-spec
        min-connections (inc (quot init-pool-size partitions))
        max-connections (inc (quot max-pool-size partitions))
        cpds (doto (BoneCPDataSource.)
                   (.setDriverClass classname)
                   (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                   (.setUsername user)
                   (.setPassword password)
                   (.setMinConnectionsPerPartition min-connections)
                   (.setMaxConnectionsPerPartition max-connections)
                   (.setPartitionCount partitions)
                   (.setStatisticsEnabled true)
                   (.setIdleMaxAgeInMinutes (or idle-time 60)))]
       {:datasource cpds}))

(defrecord Database [db-spec]
  component/Lifecycle
  (start [this]
    (assoc this :conn (pooled-datasource db-spec)))
  (stop [this]
    (let [db (:conn this)]
      (.close (:datasource db))
      (assoc this :conn nil))))

(defn new-database
  "Helper to create database"
  [db-spec]
  (map->Database {:db-spec db-spec}))
