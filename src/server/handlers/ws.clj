(ns server.handlers.ws
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [chord.http-kit :refer [with-channel]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.math.numeric-tower :as math :refer [floor round]]
            [clojure.core.async :refer [<!  >! take! put! close! chan sliding-buffer dropping-buffer go-loop]]
            [taoensso.timbre :as timbre]
            [schema.core :as s]
            ))

(timbre/refer-timbre)

(defn capture-ws [req]
  (let [ resources (:resources req) ]
    (with-channel req ws-ch
      {:read-ch (chan (dropping-buffer 10))
       :format :edn} ; again, :edn is default
      (go-loop
        []
        (let [stdout (:stdout (:capture resources))
              datom (<! stdout)]
              (>! ws-ch datom)
              (recur))))))
