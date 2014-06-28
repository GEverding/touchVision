(ns server.core
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.tools.cli :refer [cli]]
            [taoensso.timbre :as timbre]
            [compojure.handler :refer [site]]
            [server.routes :refer [app]]
            [server.config :refer [cfg app-configs]]
      ))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

(defn- to-int [s] (Integer/parseInt s))

(defn stop-server [system]
  (let [server (:server system)]
    (do
      (debug system)

    (when-not (nil? @server)
      ;; graceful shutdown: wait 100ms for existing requests to be finished
      ;; :timeout is optional, when no timeout, stop immediately
      (@server)
      (reset! server nil)))))

(defn start-server [system]
  (let [{:keys [server handler]} system
        thread (cfg :thread)
        port (cfg :port)
        ]
    ;; stop it if started, for run -main multi-times in repl
    (when-not (nil? @server) (@server))
    ;; if no open database, is noop
    ;; (db/close-database!)
    ;; open application global database
    ;; (db/use-database! "jdbc:mysql://localhost/test" "user" "password")

    ;; other init staff, like init-db, init-redis, ...
    (reset! server (run-server handler {:port port
                                        :thread thread}))))

(defn -main [& args]
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 3000 :parse-fn to-int]
             ["--thread" "Http worker thread count" :default 4 :parse-fn to-int]
             ["--profile" "dev or prod" :default :dev :parse-fn keyword]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    ;; config can be accessed by (cfg :key)
    (swap! app-configs merge options)
    (start-server (atom nil))
    (info (str "server started. listen on 0.0.0.0@" (cfg :port)))))
