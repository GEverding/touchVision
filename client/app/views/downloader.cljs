(ns app.views.downloader
  (:require-macros [cljs.core.async.macros :refer [go-loop go]] )
  (:require [strokes :refer [d3]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros (defcomponent)]
            [cljs-time.core :as t :refer [now plus minutes hours]]
            [cljs-time.coerce :refer [to-long from-long]]
            [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [app.request :refer (r)]
            [sablono.core :as html :refer-macros [html]]
            ))

(def ^:private l (log/get-logger "downloader"))

(defn send-pressure-data [datom e]
  (let [pressure (get-in datom [:pressure])
        cb (r {:type :post
             :url "/playback"
             :data {:pressure pressure}})]
    (go
      (let [res (<! cb)]
        (if (= (:status res) 200)
          (log/info "Pressure Value Sent"))))
    false))

(defcomponent downloader-view [app owner]
  (init-state
   [_]
   {:datom nil})
  (will-mount
   [_]
   (let [c (om/get-shared owner :download-chan)]
     (go-loop [selected-datom (<! c)]
       (om/set-state! owner :datom selected-datom)
       (recur (<! c)))))
  (render-state
   [_ state]
   (let [datom (:datom state)]
     (html [:div.js-downloader.downloader-container.col-sm-12
            [:h3 "Downloader"]
            (if-not (empty? datom)
              [:ul {:class "data-point list-group col-sm-12 col-md-3"}
               (map (fn [[k v]]
                      (let [kv (name k)]
                        [:li {:class (str "list-group-item data-point-" kv ) } (str kv ": " v )])) datom)]
              [:span {:class "js-no-data col-lg-12 no-data"} "No Data" [:br]])
            [:div.col-sm-12
             [:button {:type "button"
                       :class (str "btn btn-danger btn-block " (if (empty? datom)
                                                                           "disabled" ""))
                       :on-click #(send-pressure-data datom %)
                       } "Send"]]
            ]))))
