(ns binasoalan.utils)

(defn flash
  "Returns updated Ring response with associated flash data."
  [resp things]
  (assoc resp :flash things))
