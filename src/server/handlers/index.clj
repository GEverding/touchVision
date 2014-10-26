(ns server.handlers.index
  (:use plumbing.core)
  (:require [schema.core :as s]
            [server.tmpls :as tmpls]))

(def index
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (tmpls/index)})

(defnk $GET
  {:responses {200 s/Any}}
  []
  index)

