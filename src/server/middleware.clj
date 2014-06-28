(ns server.middleware
  (:require [taoensso.timbre :as timbre]
            [clojure.pprint :as printer]
            [clojure.string :as string]))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

(defn request-logger "Log HTTP Request" [handler]
  (fn [request]
    (let [ { :keys [ request-method ^String uri params ] } request
          method (string/upper-case (name request-method))
          res (handler request)
          status (:status res) ]
        (do
          (debug method status uri (if ( nil? params) "" params) )
          res))))

