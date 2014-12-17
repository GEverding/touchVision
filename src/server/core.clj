(ns server.core
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.tools.cli :refer [cli]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [server.routes :refer [wrap-app]]
            [server.config :refer [cfg app-configs]]))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

(defrecord App [port threads capture writer db]
  component/Lifecycle
  (start [this]
    (let [server (get this :app (atom nil)) ]
        ;; stop it if started, for run -main multi-times in repl
        (when-not (nil? @server) (@server))
        ;; start web with app routes
        (reset! server
                (run-server (wrap-app {:capture capture :writer writer :db db})
                            {:port port
                             :thread threads}))
        (assoc this :app server)))

  (stop [this]
    (if-let [server (get this :app)]
      (do
        ;; graceful shutdown: wait 100ms for existing requests to be finished
        ;; :timeout is optional, when no timeout, stop immediately
        (@server :timeout 10)
        (reset! server nil)))))

(defn start-app [port threads]
  (map->App {:port port :threads threads}))

