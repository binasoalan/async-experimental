(ns binasoalan.utils.enlive
  "Utility functions for enlive transformations."
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

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

(defn embed-csrf-token
  "Embed anti-forgery-token to html. Replace *anti-forgery-token* to actual
  value."
  [html-string]
  (str/replace html-string
               #"\*anti-forgery-token\*"
               *anti-forgery-token*))

(defmacro define-fragment
  "Define a fragment for enlive template. A fragment is a memoized version of html
  string emitted by enlive."
  [name args & body]
  `(def ~name (memoize
               (fn ~args
                 (apply str ~@body)))))

(defn has-link-to
  "Selector predicate, matches elements which contain a link to specified uri."
  [uri]
  (html/has [[:a (html/attr= :href uri)]]))
