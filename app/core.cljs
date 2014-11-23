(ns client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [dommy.utils :as utils]
            [cljs.core.async :as async :refer [<! >! chan sub take! sliding-buffer]]
            [cljs.core.match :as m]
            [dommy.core :refer-macros [sel sel1]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [client.views.pgm :refer [pgm-view]]
            [client.views.debug :refer [debug-view]]
            [client.views.navbar :refer [navbar-controls]]
            [client.views.options :refer [options-view]]
            [client.views.downloader :refer [downloader-view]]
            [client.ws :as ws]
            [client.request :refer [request]]))

(enable-console-print!)

(def app-state (atom nil))


(defn index []
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
                  (om/build options-view  app )
                  (om/build downloader-view  app ) ]
                 [:div {:class "col-md-10"}
                  (om/build debug-view app)]]]
                [:div {:class "row"}
                 (om/build pgm-view app)
                 ]]])))

    app-state
    {:target (sel1 ".js-app")})
  )

(defn main []
  (let [stream (ws/start!)
        ch (chan (sliding-buffer 25))]
    (go
      (let [subscriber (sub stream :post ch)]
        (loop [m (<! ch)]
          (println (:data m))
          (recur (<! ch)))))))

(main)
