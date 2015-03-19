(ns client.views.search
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [sablono.core :as html :refer-macros [html]]
            [om.core :as om :include-macros true]))

(defn search-view [app owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div {:class "drawer-search js-drawer-search"}
         [:div {:class "drawer-search-inner"}
          [:input {:type "text"
                   :placeholder "Search"
                   :focus "auto" }]] ]))))
