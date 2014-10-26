(ns server.handlers.utils.responses
  (:require [schema.core :as s]
            [schema.coerce :as c] ))

(s/defschema Ack
  "Simple acknowledgement for successful requests"
  {:message (s/eq "OK")})

(s/defschema Response
  "Generic Response Body"
  {:message String
   s/Any s/Any })

(def ack
  {:status 200
   :body {:message "OK"}})

(s/defn error
  [message :- String]
  {:status 500
   :body {:message message} })

(s/defn not-found
  [message :- String]
  {:status 404
   :body {:message message}})

(s/defschema FormError
  {:error String
   (s/optional-key :data) {s/Keyword s/Any}})

