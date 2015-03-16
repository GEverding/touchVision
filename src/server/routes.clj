(ns server.routes
  (:use plumbing.core
        server.handlers.index
        server.handlers.api
        server.handlers.ws
        server.handlers.recordings)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.pprint :refer (pprint)]
            [taoensso.timbre :as timbre]
            [cheshire.core :refer :all]
            [server.middleware :refer [wrap-middleware wrap-resources]]
            [server.capture.middleware :refer [wrap-capture-channel]]))

(timbre/refer-timbre)

(defroutes app
  (GET "/" [] index)
  (PUT "/capture/config" [] configure-capture-device)
  (GET "/init" [] init)
  (POST "/recordings" [] new-recording)
  (GET "/recordings" [] list-recordings)
  (POST "/playback" [] start-playback)
  (ANY "/recordings/:id/start" [id :as r] (start id r))
  (ANY "/recordings/:id/stop" [id :as r] (stop id r))
  (GET "/recordings/:id/data" [id :as r] (get-recording-data id r))
  (GET "/recording/:id" [id :as r] (get-recording-by-id id r))
  (route/not-found "Go Away Troll"))

(defn wrap-app [resources]
  (-> app
      (wrap-resources resources)
      wrap-middleware))
