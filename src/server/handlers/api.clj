(ns server.handlers.api
  (:use plumbing.core)
  (:require
            [clojure.java.io :as io]
            [chord.http-kit :refer [with-channel]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.core.async :refer [<!  >! take! put! close! chan sliding-buffer dropping-buffer go-loop]]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json :refer [decode encode generate-string]]
            [server.handlers.util :refer :all]
            [server.capture.core :refer capture]
            [server.config :refer [cfg]]))


;; (defresource switch-stream
;;   :available-media-types ["application/json"]
;;   :allowed-methods [:post]
;;   :known-content-types #(check-content-type % ["application/json"])
;;   :malformed? #(parse-json % ::data)
;;   :handle-created (fn [_] ( encode {:data "success"}))
;;   :post! (fn [ctx]
;;            (let [stream (-> ctx ::data :stream) ]
;;              (println (::data ctx))
;;              (println @mode)
;;              (swap! mode assoc :stream (keyword stream))
;;              (println @mode)
;;              stream)))
;;
;; (defresource switch-running
;;   :available-media-types ["application/json"]
;;   :allowed-methods [:post]
;;   :known-content-types #(check-content-type % ["application/json"])
;;   :malformed? #(parse-json % ::data)
;;   :handle-created (fn [_] ( encode {:data "success"}) )
;;   :post! (fn [ctx]
;;            (let [is-running? (-> ctx ::data :toggle) ]
;;              (println (::data ctx))
;;              (println @mode)
;;              (swap! mode assoc :is-running? is-running?)
;;              (println @mode)
;;              )))
;;
;;
;; (defresource start-playback
;;   :available-media-types ["application/json"]
;;   :allowed-methods [:post]
;;   :known-content-types #(check-content-type % ["application/json"])
;;   :malformed? #(parse-json % ::data)
;;   :handle-created (fn [_] ( encode {:data "success"}) )
;;   :post! (fn [ctx]
;;            (   let [qname (lq/declare-server-named ch :exclusive true)
;;                     data (-> ctx ::data :pressure cap)
;;                     blob (encode { :pressure data }) ]
;;              (le/direct ch "touchvision")
;;              (info "[playback]: " blob)
;;              (lb/publish ch "touchvision" "playback"  blob :content-type "text/plain")
;;              data
;;              )))
