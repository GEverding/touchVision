(ns server.routes
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [taoensso.timbre :as timbre]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [liberator.dev :refer (wrap-trace)]
            [ring.util.response :refer [resource-response response]]
            [ring.middleware [stacktrace :refer [wrap-stacktrace]]
                             [keyword-params :refer [wrap-keyword-params ] ]
                             [params :refer [wrap-params]]
                             [json :refer [wrap-json-response]]
                             [reload :refer [wrap-reload]]]
            [server.handlers [index :as h ]
                                    [workspace :as w]]
            [server.middleware.json :refer [wrap-json-body]]
            [server.middleware :as m :refer (request-logger)]))

(timbre/refer-timbre)

(defroutes api
  (ANY "/" [] h/index)
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

