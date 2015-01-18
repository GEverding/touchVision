(ns client.views.downloader
  (:require-macros [cljs.core.async.macros :refer [go-loop]] )
  (:require [strokes :refer [d3]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs-time.core :as t :refer [now plus minutes hours]]
            [cljs-time.coerce :refer [to-long from-long]]
            [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            ))

;; (defn- success [res]
;;   (.log js/console "done!"))
;; (defn- fail [error]
;;   (.log js/console (str "error: " error)))
;;
;; (defn send-pressure-data [data e]
;;   (if-let [pressure (get-in @data [:pressure])]
;;     (request {:type :post
;;               :url "/playback" }
;;              {:pressure pressure}
;;              :success success
;;              :fail fail)
;;     false))
;;
;; (defn downloader-view [app owner]
;;   (reify
;;     om/IInitState
;;     (init-state [_]
;;       {:data-point {}})
;;     om/IWillMount
;;     (will-mount [_]
;;       (let [c (:dwnld-chan app)]
;;         (go-loop
;;           []
;;           (let [selected-dp (<! c)]
;;             (om/set-state! owner :data-point selected-dp))
;;           (recur)
;;           ))
;;       )
;;     om/IRenderState
;;     (render-state [_ state]
;;       (let [dp (:data-point state)]
;;         (html [:div {:class "row js-downloader downloader-container"}
;;                [:h3.col-lg-12 "Downloader"]
;;                (if-not (empty? dp)
;;                  [:ul {:class "data-point list-group col-lg-12"}
;;                   (map (fn [[k v]]
;;                          (let [kv (name k)]
;;                            [:li {:class (str "list-group-item data-point-" kv ) } (str kv ": " v )])) dp)
;;                   ]
;;                  [:span {:class "js-no-data col-lg-12 no-data"} "No Data" [:br]])
;;                [:div.col-lg-12
;;                 [:button {:type "button"
;;                           :class (str "col-lg-12 btn btn-danger btn-block " (if (empty? dp)
;;                                                                               "disabled" ""))
;;                           :on-click #(send-pressure-data dp %)
;;                           } "Send"]]
;;                ])))))
