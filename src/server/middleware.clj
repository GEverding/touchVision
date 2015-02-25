(ns server.middleware
  (:use plumbing.core)
  (:require [taoensso.timbre :as timbre]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.params :as params]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status content-type]]
            [clojure.pprint :as printer]
            [clojure.string :as string]
            [cheshire.core :as json]
            [aprint.core :refer [aprint]]))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

(def middleware-config (-> site-defaults
                           (assoc-in [:session :cookie-name] "session")
                           (assoc-in [:session :cookie-attrs :secure] false)
                           (assoc-in [:session :flash] false)
                           (assoc-in [:security :anti-forgery] false)))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn request-logger
  "Log HTTP Requests and Responses"
  [handler logger]
  (fn [request]
    (let [{:keys [request-method ^String uri params]} request
          method (string/upper-case (name request-method))
          res (handler request)
          status (:status res) ]
      (do
        (when (json-request? request)
          (aprint request)
          (debug (str "["logger"]") method status uri (if (nil? params) "" params) )
          (aprint res)
          )
        res))))

(defn wrap-stacktrace
  "Wrap a handler such that exceptions are caught and a response containing
  a HTML representation of the exception and stacktrace is returned."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable ex
        (do
          (aprint ex)
          (if (json-request? request)
            (-> (response {:error "whoops! something went wrong over here"})
                (status 500)
                (content-type "application/json"))
            (-> (response "whoops! something went wrong over here")
                (status 500)
                (content-type "text/html"))))))))

(defn- keywordize-middleware [handler]
  (fn [req]
    (handler
     (update-in req [:query-params] keywordize-map))))

(defn wrap-resources [handler resources]
  (fn [request]
  (-> request
      (assoc :resources resources)
      handler )))

(defn wrap-middleware "Ring Middleware Builder" [handler]
  (-> handler
      (wrap-defaults middleware-config)
      (wrap-json-body {:keywords? true})
      (request-logger "app")
      keywordize-middleware
      params/wrap-params
      wrap-json-response
      wrap-stacktrace))
