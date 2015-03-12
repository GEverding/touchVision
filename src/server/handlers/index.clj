(ns server.handlers.index
  (:require [server.tmpls :as tmpls]))

(defn index [req]
  (let [capture (get-in req [:resources :capture :state])
        query-params (get-in req [:query-params])]
    (println query-params)
    (when (contains? query-params :mode)
      (swap! capture assoc :mode (keyword (:mode query-params))))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (tmpls/index)}))
