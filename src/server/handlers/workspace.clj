(ns server.handlers.workspace
  (:require [liberator.core :refer [defresource]]
            [taoensso.timbre :as timbre]
            [datomic.api :as d]
            [cheshire.core :refer [encode decode]]
            [clojure.core.match :refer [match]]
            [cheshire.core :as json]
            [server.handlers.util :as util]
            [server.tmpls :as tmpls]))

; Provides useful Timbre aliases in this ns
(timbre/refer-timbre)

