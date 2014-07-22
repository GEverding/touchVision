(ns client.core
  (:require-macros [dommy.macros :refer [node sel sel1]]
                   [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go]])
  (:require [dommy.utils :as utils]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :as async :refer [<! >! chan pipe]]
            [cljs.core.match :as m]
            [dommy.core :as dommy]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [client.views.pgm :refer [pgm-view]]
            [client.views.debug :refer [debug-view]]
            [client.views.navbar :refer [navbar-controls]]
            [client.views.options :refer [options-view]]
            [client.views.downloader :refer [downloader-view]]
            [client.router :as r :refer [create-routes]]
            [client.request :refer [request]]))

(enable-console-print!)

(def app-state (atom {:chan (chan)
                      :dwnld-chan (chan)
                      :filtered false
                      :visible true
                      :options {:running false
                                :stream :random}
                      :data [] }))

(defn websocket "init" []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:5000/ws"
                                                {:format :edn}))
          app-chan (:chan @app-state)]
      (if-not error
        (loop []
          (if-let [message (<! ws-channel)]
            (let [m (:message message)
                  type (:type m)
                  data (:data m)]
              (match type
                     :get identity
                     :post (let [app-data (:data @app-state)
                                 new-data (conj app-data (-> data
                                                             (assoc :hidden (:filtered @app-state))
                                                             (assoc :visible (not (:filtered @app-state)))))]
                             (swap! app-state assoc :data new-data))
                     :put identity
                     :delete identity)
              (recur))
            ))))))

(defn index []
  (om/root
    (fn [app owner]
      (om/component
        (html [:div
               (om/build navbar-controls app)
               [:div {:class "container-fluid"}
                [:div {:class "row"}
                 [:div {:class "col-lg-12"}
                 [:div {:class "col-lg-2"}
                  (om/build options-view  app )
                  (om/build downloader-view  app ) ]
                 [:div {:class "col-lg-10"}
                  (om/build debug-view app)]]]
                [:div {:class "row"}
                 (om/build pgm-view app)
                 ]]])))

    app-state
    {:target (sel1 ".js-app")})
  (websocket))

(def routes [{:name "index" :path "" :handler index} ])

(create-routes routes)

