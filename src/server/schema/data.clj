(ns server.schema.data
  (:require [schema.core :as s]
            [schema.coerce :as c] ))

(def Data
  "Schema Decloaration for Data"
  {(s/optional-key :id) s/Int
   :x Double
   :y Double
   :z Double
   :t Double })

(def parse-data
  (c/coercer Data c/json-coercion-matcher))

