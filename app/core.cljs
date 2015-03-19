(ns client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [dommy.utils :as utils]
            [cljs.core.async :as async :refer [<! >! chan sub sliding-buffer]]
            [dommy.core :refer-macros [sel sel1]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [plumbing.core :as plumbing :include-macros true]
            [cljs-log.core :as log]
            [client.views.pgm :refer (->pgm-view)]
            [client.views.visualizer :refer (->visualizer-view)]
            [client.views.navbar :refer [navbar-controls]]
            [client.views.options :refer (->options-view)]
            [client.views.loader :refer (->loader-view)]
            [client.views.recording-controls :refer (->recording-controls-view)]
            [client.views.downloader :refer (->downloader-view)]
            [client.ws :as ws]
            [client.request :refer (r)]))

(enable-console-print!)
(log/start-display (log/console-output))

(def l (log/get-logger "core"))

(def app-state (atom nil))

(defn index [ws-chan download-chan select-chan event-bus]
  (om/root
    (fn [app owner]
      (om/component
        (html [:div
               (om/build navbar-controls app)
               [:div {:class "container-fluid"}
                ;; [:div {:cass "row"}
                ;;  (om/build debug/controls app)]
                [:div {:class "row"}
                 [:div {:class "col-md-2 col-sm-12"}
                  (->options-view app)
                  (->recording-controls-view app)
                  (->loader-view app)
                  (->downloader-view app)
                  ]
                 [:div {:class "col-md-10 col-sm-12"}
                 (->visualizer-view app)
                  ]]
                [:div {:class "row"}
                (->pgm-view app)
                 ]]])))
    app-state
    {:target (sel1 ".js-app")
     :shared {:ws-chan ws-chan
              :download-chan download-chan
              :select-chan select-chan
              :event-bus event-bus}}))

(defn main []
  (let [
        select-chan (chan)
        select-chan-multi (async/mult select-chan)
        select-bus {:bus select-chan-multi :chan select-chan}

        event-bus (chan 5)
        event-bus-mult (async/mult event-bus)
        bus {:bus event-bus-mult :chan event-bus}

        download-chan (chan)

        ch (chan (sliding-buffer 25))
        stream (ws/start! app-state bus)
        init-chan (r {:type :get :url "/init"})]
    (go
      (let [res (<! init-chan)]
        (if (= (:status res) 200)
          (let [new-app-state (plumbing/for-map
                               [[k v] (-> res :body :data)]
                               k
                               (if (string? v)
                                 (keyword v)
                                 v))]
            (log/fine l new-app-state)
            (reset! app-state new-app-state)
            (index stream download-chan select-bus bus)))))
    ;; (go
    ;;   (loop [m (<! ch)]
    ;;     (when m
    ;;       (do
    ;;         (log/finest l (:data m))
    ;;         (recur (<! ch))
    ;;         ))))
    ))

(main)
