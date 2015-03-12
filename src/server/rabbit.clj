(ns server.rabbit
  (:require [com.stuartsierra.component :as component]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clojure.core.async :refer [<! >! put! alts! close! chan pub sliding-buffer go-loop]]
            [taoensso.timbre :as log]
            [cheshire.core :refer [decode encode]]))


(defn ^:private message-cb [stdin ch {:keys [content-type delivery-tag type] :as meta}  ^bytes payload]
  (let [blob (decode (String. payload "UTF-8") true) ]
    (println blob)
    (put! stdin {:type :glove :data blob})))

(defrecord Rabbit []
  component/Lifecycle
  (start [this]
    (let [conn (rmq/connect)
          ch (lch/open conn)
          capture-in (chan (sliding-buffer 10))
          stdin (chan (sliding-buffer 10))
          out (chan (sliding-buffer 10))
          stdout (pub out :type)
          qname (lq/declare-server-named ch {:exclusive true :auto-delete true})]
      (le/direct ch "touchvision")
      (lq/bind ch qname "touchvision" {:routing-key "glove"})
      (lc/subscribe ch qname (partial message-cb capture-in) {:auto-ack true})
      (go-loop [[m ch] (alts! [capture-in stdin])]
               (when m
                 (cond
                   (= ch capture-in) (put! out m)
                   (= ch stdin) (log/fatal m)))
               (recur (alts! [capture-in stdin])))
      (-> this
          (assoc :out out)
          (assoc :stdout stdout)
          (assoc :capture-in capture-in)
          (assoc :stdin stdin)
          (assoc :rmq-conn conn)
          (assoc :rmq-ch ch) )))
  (stop [this]
    (rmq/close (:rmq-conn this))
    (close! (:stdin this))
    (close! (:out this))
    (close! (:capture-in this))
    (-> this
        ;; (assoc :stdin nil)
        ;; (assoc :stdout nil)
        (assoc :rmq-conn nil)
        (assoc :rmq-ch nil))))

(defn new-rabbit
  "Helper to create database"
  []
  (map->Rabbit {}))
