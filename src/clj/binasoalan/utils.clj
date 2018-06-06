(ns binasoalan.utils
  (:require [clojure.core.async :as async :refer [chan]]))

(defn split-if-error
  "Split the channel if the value from the channel contains error."
  [ch]
  (async/split first ch))

(defn pipeline-split
  "Pipeline channel using xform and then splits using predicate p."
  [xform p in-ch]
  (let [out-ch (chan)]
    (async/pipeline 1 out-ch xform in-ch)
    (async/split p out-ch)))

(defn pipeline-async-split
  "Pipeline async using async function af and then splits using predicate p. af
  function is the same used for core.async/pipeline-async."
  [af p in-ch]
  (let [out-ch (chan)]
    (async/pipeline-async 1 out-ch af in-ch)
    (async/split p out-ch)))

(defn fork
  "This function real name should be pipeline-split-error but it's too long."
  [xform in-ch]
  (pipeline-split xform first in-ch))

(defn fork-async
  "This function real name should be pipeline-async-split-error but it's too
  long."
  [af in-ch]
  (pipeline-async-split af first in-ch))
