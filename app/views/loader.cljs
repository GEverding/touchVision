(ns client.views.loader
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [cljs.core.async :as async :refer [<! pub put! chan sub sliding-buffer]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [client.views.helper :refer (clear-screen)]
            [client.request :refer (r)]
            [sablono.core :as html :refer-macros [html]]))

(def ^:private l (log/get-logger "loader"))

(defn load-recording [app owner]
  (let [ws-pub-chan  (om/get-shared owner [:ws-chan])
        record-id (.-value (om/get-node owner "record-id"))]
    (if record-id
      (let [cb (r {:type :get :url (str "/recording/" record-id)})]
        (go
          (let [res (<! cb)]
            (if (= (:status res) 200)
              (let [out (:chan ws-pub-chan)
                    datoms (get-in res [:body :data])]
                (println datoms)
                (println out)
                (log/fine l "got datoms")
                (clear-screen owner)
                (put! out {:type :post :data datoms}))
              (js/alert "Not Valid Record"))))))))

(defcomponent loader-view [app owner]
  (render
   [_]
   (html [:div {:class "loading-container col-md-12 col-sm-6"}
             [:h3 "Load Recording"]
             [:div.option-group
              [:div.col-sm-6
               [:input {:type "text"
                        :ref "record-id"}]
               ]
              [:div.col-sm-6
               [:button {:type "button"
                         :class "btn btn-success"
                         :on-click (fn [_] (load-recording app owner))
                         } "Load"]]
               ]])))
