(ns server.db
  (:require [datomic.api :as d :refer [db q]]
            [server.db.db-util :as util ]))

(def schemas { })

(defn schema-data
  "Build Schema Data for a given schema"
  [schema]
  {:txes (val schema) })

(defn schema-map
  "Build Schema Map"
  [schemas]
  (reduce (fn [acc schema]
            (assoc acc (key schema) (schema-data schema )))
            {}
            schemas))

(defn install-schemas [conn schema-attr schemas]
  (apply util/ensure-schemas
         (into [conn schema-attr (schema-map schemas)] (keys schemas)) ))

(defn init [uri]
  (let [schema-attr :jarvis/schema
        conn (d/connect uri)]
    (install-schemas conn schema-attr schemas)))

(defn create [system]
  (let [uri (:db system) ]
    (d/create-database uri)
    (init uri)))

(defn delete [system]
  (let [uri (:db system) ]
    (d/delete-database uri)))

(defn recreate [system]
  (-> system
      delete
      create))

(defn reload [system] (create system))
