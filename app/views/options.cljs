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
            :url "/capture/config"
           :data data}))

(defn switch! [app owner new-state]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
    (when-not (= (om/get-state owner :mode) new-state)
      (om/update! app :mode new-state)
      (sync-app @app)
      (put! event-bus :reset))))


(defn clear-screen [app owner]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
        (log/warning l "clearing screen")
        (put! event-bus :reset)))

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
      (html [:div {:class "col-sm-6 col-md-12 switch-container" }
             [:button {:type "button"
                       :class (str "btn btn-info col-sm-6 "
                                   (if (is-active? :live)
                                     "active"
                                     ""))
                       :on-click #(switch! app owner :live )
                       } "Live"]
             [:button {:type "button"
                       :class (str "btn btn-info col-sm-6 "
                                   (if (is-active? :fake)
                                     "active"
                                     ""))
                       :on-click #(switch! app owner :fake )
                       } "Fake"]]
            ))))

(defcomponent clear-view [app owner]
  (render
    [_]
    (html [:div {:class "col-sm-3 col-md-12 clear-container"}
           [:button {:type "button"
                     :class "btn btn-warning btn-block"
                     :on-click (fn [_] (clear-screen app owner))
                     } "Clear"]])))

(defcomponent zero-view [app owner]
  (render
    [_]
    (html [:div {:class "col-sm-3 col-md-12 zero-container"}
           [:button {:type "button"
                     :class (str "btn btn-danger btn-block ") :on-click (fn [_] (zero-position! app owner)) } "Zero" ]])))

(defcomponent options-view [app owner]
  (init-state [_])
  (will-mount [_])
  (render
    [_]
    (let []
      (html [:div.option-container.col-sm-6.col-md-12
             [:h3 "Capture Controls"]
             [:div.option-group
              (->switch-view app)
              (->clear-view app)
              (->zero-view app) ]
             ]))))
