(ns server.handlers.index
  (:require [server.tmpls :as tmpls]))

(def index
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (tmpls/index)})


