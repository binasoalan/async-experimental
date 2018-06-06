(ns binasoalan.validation
  (:require [struct.core :as s]))

(def registration-schema
  {:username [s/required s/string]
   :email [s/required s/email]
   :password [s/required s/string]})

(def login-schema
  {:username [s/required s/string]
   :password [s/required s/string]})

(defn validate-registration [request]
  (s/validate request registration-schema))

(defn validate-login [request]
  (s/validate request login-schema))
