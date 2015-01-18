(ns server.rabbit
  (:require [com.stuartsierra.component :as component]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clojure.core.async :refer [<! >! put! alts! close! chan sliding-buffer go-loop]]
            [taoensso.timbre :as log]
            [cheshire.core :refer [decode encode]]))


(defn ^:private message-cb [stdin ch {:keys [content-type delivery-tag type] :as meta}  ^bytes payload]
  (let [blob (decode (String. payload "UTF-8") true) ]
    (println blob)
    (put! stdin blob )))

(defrecord Rabbit []
  component/Lifecycle
  (start [this]
    (let [conn (rmq/connect)
          ch (lch/open conn)
          capture-in (chan (sliding-buffer 10))
          stdin (chan (sliding-buffer 10))
          stdout (chan (sliding-buffer 10))
          qname (lq/declare-server-named ch {:exclusive true :auto-delete true})]
      (le/direct ch "touchvision")
      (lq/bind ch qname "touchvision" {:routing-key "glove"})
      (lc/subscribe ch qname (partial message-cb capture-in) {:auto-ack true})
      (go-loop [[m ch] (alts! [capture-in stdin])]
               (cond
                 (= ch capture-in) (>! stdout m)
                 (= ch stdin) (log/fatal "nothing should be coming in yet"))
               (recur (alts! [capture-in stdin])))
      (-> this
          (assoc :stdout stdout)
          (assoc :stdin stdin)
          (assoc :rmq-conn conn)
          (assoc :rmq-ch ch) )))
  (stop [this]
    (rmq/close (:rmq-conn this))
    (close! (:stdin this))
    (close! (:stdout this))
    (-> this
        ;; (assoc :stdin nil)
        ;; (assoc :stdout nil)
        (assoc :rmq-conn nil)
        (assoc :rmq-ch nil))))

(defn new-rabbit
  "Helper to create database"
  []
  (map->Rabbit {}))
