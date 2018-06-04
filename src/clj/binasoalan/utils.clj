(ns binasoalan.utils
  (:require [clojure.core.async :as async]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]))

(defn flash
  "Returns updated Ring response with associated flash data."
  [resp things]
  (assoc resp :flash things))

(defn split-if-error
  "Split the channel if the value from the channel contains error."
  [ch]
  (async/split first ch))

(def common-interceptors [(body-params/body-params) http/html-body])
