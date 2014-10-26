(ns server.ws
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.tools.cli :refer [cli]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [server.routes :refer [wrapped-ws-app]]
            [server.config :refer [cfg app-configs]]))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

(defrecord WS [port threads capture]
  component/Lifecycle
  (start [this]
    (let [server (get this :app (atom nil)) ]
        ;; stop it if started, for run -main multi-times in repl
        (when-not (nil? @server) (@server))
        ;; start web with app routes
        (reset! server
                (run-server (wrapped-ws-app capture)
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

(defn start-ws [port threads]
  (map->WS {:port port :threads threads}))

