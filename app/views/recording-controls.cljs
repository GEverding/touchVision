(ns client.views.recording-controls
  (:require-macros [cljs.core.async.macros :refer [go-loop go]] )
  (:require [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [om-tools.core :refer-macros (defcomponent)]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs-log.core :as log]
            [sablono.core :as html :refer-macros [html]]
            [client.request :refer (r)]))

(defonce ^:private l (log/get-logger "rec"))

(defn clear-screen [owner]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
        (log/warning l "clearing screen")
        (put! event-bus :reset)))

(defn- new-recording [app owner]
  (let [cb (r {:type :post
               :url "/recordings"
               :data {:patient-id (:patient-id @app)} })]
    (go
      (let [res (<! cb)]
        (if (= (:status res) 200)
          (let [id (get-in res [:body :data :id])]
            (clear-screen owner)
            (om/set-state! owner :recording-id id)
            (om/update! app [:recording-id] id)
            (om/set-state! owner :state :ready)))))))

(defn- start-recording [app owner]
  (log/info (om/get-state owner))
  (let [id (:recording-id @app)
        cb (r {:type :get
               :url (str "/recordings/" id "/start") })]
    (go
      (let [res (<! cb)]
        (if (= (:status res) 200)
          (om/set-state! owner :state :started))))
    ))

(defn- stop-recording [app owner]
  (let [id (:recording-id @app)
        cb (r {:type :get
               :url (str "/recordings/" id "/stop") })]
    (go
      (let [res (<! cb)]
        (if (= (:status res) 200)
          (om/set-state! owner :state :stopped))))
    ))

(defcomponent recording-controls-view
  [app owner]
  (init-state
    [_]
    {:state :stopped ;; :ready :started :stopped
     })
  (render-state
    [_ state]
    (let [recording-id (:recording-id app)
          s (:state state)]
      (html [:div {:class "rec-container col-md-12 col-sm-6"}
             [:h3 "Recording Controls"]
             [:div
              [:span (str "Current Recording: " recording-id)]]
             [:div.option-group
              [:div.col-sm-4
               [:button {:type "button"
                         :class (str "btn btn-primary")
                         :on-click (fn [_] (new-recording app owner))
                         } "New"]]
              [:div.col-sm-4
               [:button {:type "button"
                         :class "btn btn-success"
                         :on-click (fn [_] (start-recording app owner))
                         } "Start"]]
               [:div.col-sm-4
               [:button {:type "button"
                         :class "btn btn-danger"
                         :on-click (fn [_] (stop-recording app owner))
                         } "Stop"]]]]))))
