(ns server.handlers.index
  (:require [liberator.core :refer [defresource]]

            [taoensso.timbre :as timbre]
            [clojure.core.match :refer [match]]
            [cheshire.core :as json]
            [server.handlers.util :as util]
            [server.tmpls :as tmpls]
            ))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

(defresource index
  :available-media-types ["text/html"]
  :allowed-methods [:get]
  :known-content-types #(util/check-content-type %)
  :handle-ok (fn [_] (tmpls/index)))

