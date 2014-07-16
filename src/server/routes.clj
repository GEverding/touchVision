(ns server.routes
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [taoensso.timbre :as timbre]
            [clojure.contrib.math :refer [floor]]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [chord.http-kit :refer [with-channel]]
            [liberator.dev :refer (wrap-trace)]
            [ring.util.response :refer [resource-response response]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [ring.middleware [stacktrace :refer [wrap-stacktrace]]
                             [keyword-params :refer [wrap-keyword-params ] ]
                             [params :refer [wrap-params]]
                             [json :refer [wrap-json-response]]
                             [reload :refer [wrap-reload]]]
            [org.httpkit.server :refer [on-close on-receive send!]]
            [clojure.core.async :refer [<! >! put! close! chan dropping-buffer go]]
            [server.handlers [index :as h ]
                                    [workspace :as w]]
            [server.middleware.json :refer [wrap-json-body]]
            [server.middleware :as m :refer (request-logger)]))

(timbre/refer-timbre)

(defn- random-date [start end]
  (+ start (* (rand) (- end start))))

(defn- generate-date
  "Generate random set of times (x) between start and end"
  [x start end]
  (for [low (range x )] (random-date start end)))

(defn gen-fake-data []
  {:type :post
   :data
   {:x (rand 100)
    :y (rand 100)
    :z (rand 100)
    :pressure (floor (rand 5))
    :timestamp (floor (random-date
                        (c/to-long (t/now))
                        (c/to-long
                          (t/plus (t/now) (t/seconds 100)))))}})


(defn handler [req]
  (with-channel req ws-ch
    {:read-ch (chan (dropping-buffer 10))
     :format :edn} ; again, :edn is default
    (go
     (dotimes [i 500]
       (>! ws-ch (gen-fake-data))
       ))))

(defroutes api
  (ANY "/" [] h/index)
  (GET "/ws" [] handler)
  (route/resources "/")
  (route/not-found "No Found!"))

(def app (-> (handler/site #'api)
             wrap-json-body
             ;; wrap-json-response
             wrap-params
             wrap-keyword-params
             wrap-trace
             request-logger
             wrap-reload
             wrap-stacktrace))

