(ns server.io.writer
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [clojure.core.async :refer [go-loop put! close! chan sliding-buffer <! sub]]
            [taoensso.timbre :as timbre]
            [server.db.queries :as q]))

(timbre/refer-timbre)

(defonce ^:private current_recording (atom nil))

(defn set-recording-id! [id]
  (do
    (reset! current_recording id)
    true)
  false)

(defrecord Writer [capture db rabbit]
  Lifecycle
  (start [this]
    (let [conn (:conn db)
          stdin (chan (sliding-buffer 10))
          stdout (chan (sliding-buffer 100))]
      (sub (:stdout capture) :post stdin)
      (go-loop
        [datom (<! stdin)]
        (when datom
          (let [{:keys [pressure timestamp x y z]} (:data datom) ]
            (when @current_recording
              (let [row (q/append<! conn @current_recording pressure x y z timestamp)]
                ;(debug "saved: " row)
                (when row
                  (put! stdout (:pressure row)))
                ))
            (recur (<! stdin)))))
      (-> this
          (assoc :stdout stdout))))
  (stop [this]
    (close! (:stdout this))
    this))

(defn start-writer []
  (map->Writer {}))
