(ns binasoalan.utils
  (:require [clojure.core.async :as async]))

(defn flash
  "Returns updated Ring response with associated flash data."
  [resp things]
  (assoc resp :flash things))

(defn split-if-error
  "Split the channel if the value from the channel contains error."
  [ch]
  (async/split first ch))
