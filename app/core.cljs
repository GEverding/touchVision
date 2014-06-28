(ns client.core
  (:require-macros [dommy.macros :refer [node sel sel1]]
                   [cljs.core.async.macros :refer [go]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [sablono.core :as html :refer-macros [html]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs.core.async :as async :refer [<! >! chan]]
            [client.views.navbar :refer [navbar-controls]]
            [client.router :as r :refer [create-routes]]
            [client.request :refer [request]]))

(enable-console-print!)

(def app-state (atom {:chan (chan)}))

(defn index []
  (om/root
    (fn [app owner]
      (html [:div
             (om/build navbar-controls app)
             [:div {:class "container-fluid"}
              [:h1 (:text app)]
              ] ]))
    app-state
    {:target (sel1 ".js-app")}))

(def routes [{:name "index" :path "" :handler index} ])

(create-routes routes)

