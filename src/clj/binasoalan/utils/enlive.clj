(ns binasoalan.utils.enlive
  "Utility functions for enlive transformations.")

(defn only-when
  "Only apply transformation when predicate p is true."
  [p transform-fn]
  (if p
    transform-fn
    identity))

(defn show-when
  "Show node according to predicate p, else remove node."
  [p]
  (when p
    identity))
