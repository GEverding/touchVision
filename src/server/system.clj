(ns server.system
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [plumbing.core :refer [defnk]]
            [server.capture.core :as capture]
            [server.core :as app]))

(defnk system [{port 3000} {threads 2} {mode :fake}]
  (component/system-map
    :capture (capture/capture-start mode)
    :app (component/using
           (app/start-app port threads)
           [:capture])))

(defn -main [& args]
  (let [[port threads mode] args]
    (component/start
      (system))))

