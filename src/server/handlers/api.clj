(ns server.handlers.api
  (:require [server.tmpls :as tmpls])
  )

(defn foo "Index Page" [req]
  (tmpls/index {:title "go away"}))
