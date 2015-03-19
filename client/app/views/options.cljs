(ns app.views.options
  (:require-macros [cljs.core.async.macros :refer [go-loop go]] )
  (:require [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [om-tools.core :refer-macros (defcomponent)]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs-log.core :as log]
            [sablono.core :as html :refer-macros [html]]
            [app.request :refer (r)]))

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


(defn zero-glove! [app owner]
  (let [event-bus (om/get-shared owner [:event-bus :chan])
        ch (r {:type :get :url "/clear"})]
    (go (let [res (<! ch)]
          (if (= (:status res) 200)
            (do
              (log/warning l "clearing screen")
              (put! event-bus :reset))
            (log/warning l "failed to zero glove"))))))


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

(defcomponent zero-view [app owner]
  (render
    [_]
    (html [:div {:class "col-sm-3 col-md-12 zero-container"}
           [:button {:type "button"
                     :class "btn btn-danger btn-block"
                     :on-click (fn [_] (zero-glove! app owner))
                     } "Zero"]])))


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
              (->zero-view app) ]
             ]))))
