(ns server.routes
  (:use plumbing.core
        server.handlers.index
        server.handlers.api
        server.handlers.ws
        )
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.pprint :refer (pprint)]
            [taoensso.timbre :as timbre]
            [cheshire.core :refer :all]
            [fnhouse.middleware :as middleware]
            [fnhouse.routes :as routes]
            [fnhouse.handlers :as handlers]
            [fnhouse.middleware :refer [coercion-middleware]]
            [server.middleware :refer [wrap-middleware wrap-ws-middleware]]
            [server.capture.middleware :refer [wrap-capture-channel]]
            ))

(timbre/refer-timbre)

(defn wrapped-root-handler
  "Take the resources, partially apply them to the handlers in
   the 'guesthouse.guestbook namespace, wrap each with a custom
   coercing middleware, and then compile them into a root handler
   that will route requests to the appropriate underlying handler.
   Then, wrap the root handler in some standard ring middleware.
   When served, the handlers will be hosted at the 'guestbook' prefix."

  [resources]
  (let [ handlers (handlers/nss->handlers-fn {"" 'server.handlers.index
                                             "api" 'server.handlers.api
                                             }) ]
    (->> resources
        handlers
        (map #(coercion-middleware % (constantly nil) (constantly nil)))
        routes/root-handler
        wrap-middleware)))

(defroutes ws
  (GET "/ws" [] capture-ws)
  (route/not-found "Go Away Troll"))

(defn wrapped-ws-app [capture]
  (-> ws
      (wrap-capture-channel capture)
      wrap-ws-middleware))

