(ns server.tmpls
  (:require [clostache.parser :refer [render-resource]]
            [server.config :refer [cfg]]))

(defn add-info [data]
  (-> data
      (assoc :title "touchVision")))

(defn index
  ([] (index {}))
  ([data]
   (render-resource "templates/index.mustache.html" (add-info data)) ))
