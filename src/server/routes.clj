(ns server.routes
  (:use plumbing.core
        server.handlers.index
        server.handlers.api
        server.handlers.ws)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.pprint :refer (pprint)]
            [taoensso.timbre :as timbre]
            [cheshire.core :refer :all]
            [server.middleware :refer [wrap-middleware wrap-resources]]
            [server.capture.middleware :refer [wrap-capture-channel]]
            ))

(timbre/refer-timbre)

(defroutes app
  (GET "/" [] index)
  (GET "/ws" [] capture-ws)
  (GET "/ws/config" [] configure-ws)
  (route/not-found "Go Away Troll"))

(defn wrap-app [resources]
  (-> app
      (wrap-resources resources)
      wrap-middleware))

