(ns client.views.options
  (:require-macros [cljs.core.async.macros :refer [go-loop]] )
  (:require [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [om-tools.core :refer-macros (defcomponent)]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs-log.core :as log]
            [sablono.core :as html :refer-macros [html]]
            [client.request :refer (r)]))

(defonce ^:private l (log/get-logger "options"))

(defn- sync-app
  [data]
  (r {:type :put
            :url "/ws/config"
           :data data}))

(defn switch! [app owner new-state]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
    (when-not (= (om/get-state owner :mode) new-state)
      (om/update! app :mode new-state)
      (sync-app @app)
      (put! event-bus :reset) )))

(defn switch-stream-state! [app owner]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
    (if (= (:stream @app) :open)
      (do
        (log/info l "Stream switched to closed state")
        (om/update! app :stream :closed)
        (sync-app @app)
        (put! event-bus :paused))
      (do
        (log/info l "Stream switched to open state")
        (om/update! app :stream :open)
        (sync-app @app)
        (put! event-bus :running)))))

(defn zero-position! [app owner]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
    (log/warning l "Preparing to zero position data and reset view")
    (put! event-bus :reset)
    ;; send request to zero
    (put! event-bus :zeroed)))

(defcomponent switch-view
  "Button Switch Component"
  [app owner]
  (render [_]
    (let [current-mode (:mode app)
          is-active? (fn [mode] (= mode (:mode app))) ]
      (html [:div {:class "col-sm-2 switch-container"
                   :style {:display "inline-block"
                           :width "17%" }}
             [:button {:type "button"
                       :class (str "btn btn-info col-md-6 "
                                   (if (is-active? :live)
                                     "active"
                                     ""))
                       :on-click #(switch! app owner :live )
                       } "Live"]
             [:button {:type "button"
                       :class (str "btn btn-info col-md-6 "
                                   (if (is-active? :fake)
                                     "active"
                                     ""))
                       :on-click #(switch! app owner :fake )
                       } "Fake"]]
            ))))

(defcomponent toggle-view [app owner]
  (render
    [_]
    (html [:div {:class "col-md-4 col-sm-2 toggle-container"}
           [:button {:type "button"
                     :class (str "btn btn-warning btn-block "
                                 (if (= :open (:stream app))
                                   "active"
                                   ""))
                     :on-click (fn [_] (switch-stream-state! app owner))
                     } (if (= :open (:stream app))
                         "On"
                         "Off"
                         ) ]])))

(defcomponent zero-view [app owner]
  (render
    [_]
    (html [:div {:class "col-md-4 col-sm-2 zero-container"}
           [:button {:type "button"
                     :class (str "btn btn-danger btn-block ") :on-click (fn [_] (zero-position! app owner)) } "Zero" ]])))

(defcomponent options-view [app owner]
  (init-state [_])
  (will-mount [_])
  (render
    [_]
    (let []
      (html [:div {:class "row option-container"}
             [:h3.col-lg-12 "Capture Controls"]
             [:div {:class "option-group"}
              (->switch-view app)
              (->toggle-view app)
              (->zero-view app) ]
             ]))))

