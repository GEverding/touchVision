(ns server.config)

(defonce app-configs (atom {:profile :dev
                            :port 5000
                            :thread 4
                            :datomic-uri "datomic:free://localhost:4334/touchVision"
                            :capture-queue "touchVision.glove"
                            :playback-queue "touchVision.playback"}))

(defn cfg [key & [default]]
  (if-let [v (or (key @app-configs) default)]
    v
    (when-not (contains? @app-configs key)
      (throw (RuntimeException. (str "unknown config for key " (name key)))))))

