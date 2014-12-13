(ns client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [dommy.utils :as utils]
            [cljs.core.async :as async :refer [<! >! chan sub take! sliding-buffer]]
            [cljs.core.match :as m]
            [dommy.core :refer-macros [sel sel1]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs-log.core :as log]
            [client.views.pgm :refer (->pgm-view)]
            [client.views.visualizer :refer (->visualizer-view)]
            [client.views.navbar :refer [navbar-controls]]
            [client.views.options :refer [options-view]]
            [client.views.downloader :refer [downloader-view]]
            [client.ws :as ws]
            [client.request :refer [request]]))

(enable-console-print!)

(def app-state (atom nil))

(defn index [ws-chan select-chan]
  (om/root
    (fn [app owner]
      (om/component
        (html [:div
               (om/build navbar-controls app)
               [:div {:class "container-fluid"}
                ;; [:div {:cass "row"}
                ;;  (om/build debug/controls app)]
                [:div {:class "row"}
                 [:div {:class "col-md-12"}
                 [:div {:class "col-md-2"}
                  ;; (om/build options-view  app )
                  ;; (om/build downloader-view  app )
                  ]
                 [:div {:class "col-md-10"}
                  (->visualizer-view app)
                  ]]]
                [:div {:class "row"}
                 (->pgm-view app)
                 ]]])))
    app-state
    {:target (sel1 ".js-app")
     :shared {:ws-chan ws-chan
              :select-chan select-chan }}))

(def l (log/get-logger "core"))

(defn main []
  (let [stream (ws/start!)
        select-chan (chan)
        ch (chan (sliding-buffer 25))]
    (log/start-display (log/console-output))
    (go
      (let [subscriber (sub stream :post ch)]
        (loop [m (<! ch)]
          (when m
            (do
              (log/finest l (:data m))
              (recur (<! ch)))))))
    (index stream select-chan)))

(main)
