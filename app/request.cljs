(ns client.request
  (:require [ajax.core :refer [GET POST PUT DELETE ajax-request]]))

(def default-params {:finally identity
                     :format :json
                     :keywords? true })

(defn make-request
  [{:keys [success fail data] :as opts :or { success identity fail identity params "" }}]
  (-> default-params
      (assoc :handler success)
      (assoc :error-handler fail)
      (assoc :params data)))

(defmulti request (fn [req _ _] ( :type req ) ))

(defmethod request :get
  [req _ opts]
  (let [{:keys [success fail]} opts
        params (make-request { :success success :fail fail} ) ]
    (.log js/console params)
    (GET (:url req) (make-request { :success success :fail fail} ))))

(defmethod request :post
  [req data opts]
  (let [{:keys [success fail]} opts ]
    (POST (:url req) (make-request {:success success :fail fail :data data} ))))

(defmethod request :put
  [req data opts]
  (let [{:keys [success fail]} opts ]
    (PUT (:url req) (make-request { :success success :fail fail :data data} ))))

(defmethod request :delete
  [req data opts]
  (let [{:keys [success fail]} opts ]
    (DELETE (:url req) (make-request { :success success :fail fail :data data}))))

