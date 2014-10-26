(ns server.system
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [plumbing.core :refer [defnk]]
            [server.capture.core :as capture]
            [server.ws :as ws]
            [server.core :as app]))

(defnk system [{port 3000} {threads 2} {mode :fake}]
  (component/system-map
    :capture (capture/capture-start :fake)
    :ws (component/using
          (ws/start-ws (inc port) threads)
          [:capture])
    :app (app/start-app port threads)
    ))


(defn -main [& args]
  (let [[port threads mode] args]
    (component/start
      (system))))

